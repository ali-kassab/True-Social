package com.example.Social_Media_RestApi_Project.notification.dto;

import com.example.Social_Media_RestApi_Project.notification.entity.Notification;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        UserSummary actor,
        Long postId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getActor().getName() + " " + notification.getType().getAction(),
                UserSummary.from(notification.getActor()),
                notification.getPost() != null ? notification.getPost().getId() : null,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
