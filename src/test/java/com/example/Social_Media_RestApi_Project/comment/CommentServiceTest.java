package com.example.Social_Media_RestApi_Project.comment;

import com.example.Social_Media_RestApi_Project.comment.dto.CommentRequest;
import com.example.Social_Media_RestApi_Project.comment.dto.CommentResponse;
import com.example.Social_Media_RestApi_Project.comment.entity.Comment;
import com.example.Social_Media_RestApi_Project.comment.repository.CommentRepository;
import com.example.Social_Media_RestApi_Project.comment.service.CommentService;
import com.example.Social_Media_RestApi_Project.exception.ForbiddenException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private NotificationService notificationService;

    @InjectMocks private CommentService commentService;

    private final User postAuthor = TestData.user(1L);
    private final User commenter = TestData.user(2L);
    private final Post post = TestData.post(10L, postAuthor);
    private final Comment comment = TestData.comment(100L, commenter, post);

    @Test
    void createComment_savesCommentAndNotifiesPostAuthor() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(userRepository.getReferenceById(2L)).thenReturn(commenter);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.createComment(10L, new CommentRequest(" Nice post! "));

        assertThat(response.content()).isEqualTo("Nice post!");
        assertThat(response.author().id()).isEqualTo(2L);
        verify(notificationService).notify(postAuthor, commenter, NotificationType.COMMENT, post);
    }

    @Test
    void updateComment_byAuthor_updatesContent() {
        when(commentRepository.findByIdAndPostId(100L, 10L)).thenReturn(Optional.of(comment));
        when(currentUserProvider.canModify(2L)).thenReturn(true);
        when(commentRepository.saveAndFlush(comment)).thenReturn(comment);

        CommentResponse response = commentService.updateComment(10L, 100L, new CommentRequest("Edited"));

        assertThat(response.content()).isEqualTo("Edited");
    }

    /** Regression test: the original code allowed everyone EXCEPT the owner to edit a comment. */
    @Test
    void updateComment_byAnotherUser_isForbidden() {
        when(commentRepository.findByIdAndPostId(100L, 10L)).thenReturn(Optional.of(comment));
        when(currentUserProvider.canModify(2L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.updateComment(10L, 100L, new CommentRequest("Hacked")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(comment.getContent()).isEqualTo("Comment 100");
    }

    @Test
    void getComment_fromDifferentPost_isNotFound() {
        when(commentRepository.findByIdAndPostId(100L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComment(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteComment_byPostAuthor_isAllowed() {
        when(commentRepository.findByIdAndPostId(100L, 10L)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        commentService.deleteComment(10L, 100L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_byUnrelatedUser_isForbidden() {
        when(commentRepository.findByIdAndPostId(100L, 10L)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUserId()).thenReturn(3L);
        when(currentUserProvider.canModify(2L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(10L, 100L))
                .isInstanceOf(ForbiddenException.class);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void getComments_forMissingPost_isNotFound() {
        when(postRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(10L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
