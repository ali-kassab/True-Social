package com.example.Social_Media_RestApi_Project.post;

import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.entity.SavedPost;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.post.repository.SavedPostRepository;
import com.example.Social_Media_RestApi_Project.post.service.SavedPostService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedPostServiceTest {

    @Mock private SavedPostRepository savedPostRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private SavedPostService savedPostService;

    private final User me = TestData.user(2L);
    private final Post post = TestData.post(10L, TestData.user(1L));

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
    }

    @Test
    void savePost_savesForCurrentUser() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(savedPostRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(false);
        when(userRepository.getReferenceById(2L)).thenReturn(me);

        savedPostService.savePost(10L);

        ArgumentCaptor<SavedPost> saved = ArgumentCaptor.forClass(SavedPost.class);
        verify(savedPostRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(me);
        assertThat(saved.getValue().getPost()).isSameAs(post);
    }

    @Test
    void savePost_twice_isConflict() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(savedPostRepository.existsByUserIdAndPostId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> savedPostService.savePost(10L))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(savedPostRepository, never()).save(any());
    }

    @Test
    void savePost_missingPost_isNotFound() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedPostService.savePost(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unsavePost_removesOwnSavedPost() {
        SavedPost savedPost = new SavedPost(me, post);
        when(savedPostRepository.findByUserIdAndPostId(2L, 10L)).thenReturn(Optional.of(savedPost));

        savedPostService.unsavePost(10L);

        verify(savedPostRepository).delete(savedPost);
    }

    @Test
    void unsavePost_whenNotSaved_isNotFound() {
        when(savedPostRepository.findByUserIdAndPostId(2L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedPostService.unsavePost(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
