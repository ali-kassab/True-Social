package com.example.Social_Media_RestApi_Project.comment.dto;

import com.example.Social_Media_RestApi_Project.comment.entity.Comment;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long postId,
        String content,
        UserSummary author,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                UserSummary.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
