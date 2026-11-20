package com.example.Social_Media_RestApi_Project.notification;

import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.entity.Notification;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.repository.NotificationRepository;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.entity.User;
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
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private NotificationService notificationService;

    private final User alice = TestData.user(1L);
    private final User bob = TestData.user(2L);

    @Test
    void notify_otherUser_savesNotification() {
        notificationService.notify(alice, bob, NotificationType.FOLLOW, null);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notify_yourself_isSkipped() {
        notificationService.notify(alice, alice, NotificationType.LIKE, TestData.post(10L, alice));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_ownNotification_marksIt() {
        Notification notification = new Notification(alice, bob, NotificationType.FOLLOW, null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(notificationRepository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(5L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_someoneElsesNotification_isNotFound() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(notificationRepository.findByIdAndRecipientId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
