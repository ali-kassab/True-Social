package com.example.Social_Media_RestApi_Project.post;

import com.example.Social_Media_RestApi_Project.comment.repository.CommentRepository;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ForbiddenException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.repository.NotificationRepository;
import com.example.Social_Media_RestApi_Project.post.dto.CreatePostRequest;
import com.example.Social_Media_RestApi_Project.post.dto.PostResponse;
import com.example.Social_Media_RestApi_Project.post.dto.UpdatePostRequest;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.repository.PostLikeRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.post.repository.SavedPostRepository;
import com.example.Social_Media_RestApi_Project.post.service.PostService;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private SavedPostRepository savedPostRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private PostService postService;

    private final User author = TestData.user(1L);
    private final Post post = TestData.post(10L, author);

    @BeforeEach
    void setUp() {
        lenient().when(postLikeRepository.countByPostIds(anyCollection())).thenReturn(List.of());
        lenient().when(commentRepository.countByPostIds(anyCollection())).thenReturn(List.of());
        lenient().when(postLikeRepository.findLikedPostIds(anyLong(), anyCollection())).thenReturn(Set.of());
    }

    @Test
    void createPost_usesAuthenticatedUserAsAuthor() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(author);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L); // the database assigns the id
            return saved;
        });

        PostResponse response = postService.createPost(new CreatePostRequest("  Hello world ", "Some content"));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());
        assertThat(saved.getValue().getAuthor()).isSameAs(author);
        assertThat(saved.getValue().getTitle()).isEqualTo("Hello world");
        assertThat(response.author().id()).isEqualTo(1L);
        assertThat(response.likeCount()).isZero();
    }

    @Test
    void updatePost_byOwner_updatesOnlyProvidedFields() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(currentUserProvider.canModify(1L)).thenReturn(true);
        when(postRepository.saveAndFlush(post)).thenReturn(post);

        PostResponse response = postService.updatePost(10L, new UpdatePostRequest("New title", null));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.content()).isEqualTo("Post content 10");
    }

    @Test
    void updatePost_byAnotherUser_isForbidden() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(currentUserProvider.canModify(1L)).thenReturn(false);

        assertThatThrownBy(() -> postService.updatePost(10L, new UpdatePostRequest("Hacked", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(post.getTitle()).isEqualTo("Post title 10");
        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatePost_withNoFields_isBadRequest() {
        assertThatThrownBy(() -> postService.updatePost(10L, new UpdatePostRequest(null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getPost_whenMissing_throwsNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deletePost_byOwner_removesDependentRowsThenPost() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(currentUserProvider.canModify(1L)).thenReturn(true);

        postService.deletePost(10L);

        verify(notificationRepository).deleteByPostId(10L);
        verify(postLikeRepository).deleteByPostId(10L);
        verify(savedPostRepository).deleteByPostId(10L);
        verify(commentRepository).deleteByPostId(10L);
        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_byAnotherUser_isForbidden() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(currentUserProvider.canModify(1L)).thenReturn(false);

        assertThatThrownBy(() -> postService.deletePost(10L)).isInstanceOf(ForbiddenException.class);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void getPostsByUser_whenUserMissing_throwsNotFound() {
        when(userRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> postService.getPostsByUser(5L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
