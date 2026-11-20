package com.example.Social_Media_RestApi_Project.follow;

import com.example.Social_Media_RestApi_Project.block.repository.BlockRepository;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ForbiddenException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.follow.entity.Follow;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
import com.example.Social_Media_RestApi_Project.follow.service.FollowService;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
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
class FollowServiceTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private NotificationService notificationService;
    @Mock private BlockRepository blockRepository;

    @InjectMocks private FollowService followService;

    private final User me = TestData.user(1L);
    private final User target = TestData.user(2L);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void follow_savesRelationAndNotifiesTarget() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

        followService.follow(2L);

        ArgumentCaptor<Follow> saved = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(saved.capture());
        assertThat(saved.getValue().getFollower()).isSameAs(me);
        assertThat(saved.getValue().getFollowing()).isSameAs(target);
        verify(notificationService).notify(target, me, NotificationType.FOLLOW, null);
    }

    @Test
    void follow_yourself_isBadRequest() {
        assertThatThrownBy(() -> followService.follow(1L)).isInstanceOf(BadRequestException.class);
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_twice_isConflict() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> followService.follow(2L)).isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void follow_unknownUser_isNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.follow(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void follow_whenBlocked_isForbidden() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(blockRepository.existsBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> followService.follow(2L)).isInstanceOf(ForbiddenException.class);
        verify(followRepository, never()).save(any());
    }

    @Test
    void unfollow_whenNotFollowing_isNotFound() {
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollow(2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
