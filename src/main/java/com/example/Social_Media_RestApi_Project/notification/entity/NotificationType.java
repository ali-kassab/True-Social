package com.example.Social_Media_RestApi_Project.notification.entity;

public enum NotificationType {
    LIKE("liked your post"),
    COMMENT("commented on your post"),
    FOLLOW("started following you");

    private final String action;

    NotificationType(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
