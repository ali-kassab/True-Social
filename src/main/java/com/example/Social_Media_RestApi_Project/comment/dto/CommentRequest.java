package com.example.Social_Media_RestApi_Project.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used both to create and to edit a comment. */
public record CommentRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must be at most 1000 characters")
        String content
) {
}
