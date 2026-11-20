package com.example.Social_Media_RestApi_Project.post.dto;

import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        UserSummary author,
        long likeCount,
        long commentCount,
        boolean likedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
}
