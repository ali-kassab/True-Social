package com.example.Social_Media_RestApi_Project.notification.service;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.dto.NotificationResponse;
import com.example.Social_Media_RestApi_Project.notification.dto.UnreadCountResponse;
import com.example.Social_Media_RestApi_Project.notification.entity.Notification;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.repository.NotificationRepository;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Records a notification for {@code recipient}. Users are never notified about their own actions.
     * Runs inside the caller's transaction so the notification is saved together with the action.
     */
    @Transactional
    public void notify(User recipient, User actor, NotificationType type, Post post) {
        if (Objects.equals(recipient.getId(), actor.getId())) {
            return;
        }
        notificationRepository.save(new Notification(recipient, actor, type, post));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(boolean unreadOnly, Pageable pageable) {
        Long userId = currentUserProvider.getCurrentUserId();
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalse(userId, newestFirst)
                : notificationRepository.findByRecipientId(userId, newestFirst);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(
                notificationRepository.countByRecipientIdAndReadFalse(currentUserProvider.getCurrentUserId()));
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        // Looking up by id AND recipient means users can only touch their own notifications
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(currentUserProvider.getCurrentUserId());
    }
}
