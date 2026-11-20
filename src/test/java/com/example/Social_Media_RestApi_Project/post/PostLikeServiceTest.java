package com.example.Social_Media_RestApi_Project.post;

import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.entity.PostLike;
import com.example.Social_Media_RestApi_Project.post.repository.PostLikeRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.post.service.PostLikeService;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private NotificationService notificationService;

    @InjectMocks private PostLikeService postLikeService;

    private final User author = TestData.user(1L);
    private final User liker = TestData.user(2L);
    private final Post post = TestData.post(10L, author);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
    }

    @Test
    void likePost_savesLikeAndNotifiesAuthor() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(false);
        when(userRepository.getReferenceById(2L)).thenReturn(liker);

        postLikeService.likePost(10L);

        verify(postLikeRepository).save(any(PostLike.class));
        verify(notificationService).notify(author, liker, NotificationType.LIKE, post);
    }

    @Test
    void likePost_twice_isConflict() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> postLikeService.likePost(10L))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void likePost_missingPost_isNotFound() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.likePost(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unlikePost_whenNotLiked_isNotFound() {
        when(postLikeRepository.findByUserIdAndPostId(2L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.unlikePost(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unlikePost_deletesExistingLike() {
        PostLike like = new PostLike(liker, post);
        when(postLikeRepository.findByUserIdAndPostId(2L, 10L)).thenReturn(Optional.of(like));

        postLikeService.unlikePost(10L);

        verify(postLikeRepository).delete(like);
    }
}
