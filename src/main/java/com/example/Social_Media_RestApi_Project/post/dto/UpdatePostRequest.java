package com.example.Social_Media_RestApi_Project.post.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: only non-null fields are changed. */
public record UpdatePostRequest(
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        @Pattern(regexp = "(?s).*\\S.*", message = "Title must not be blank")
        String title,

        @Size(max = 5000, message = "Content must be at most 5000 characters")
        @Pattern(regexp = "(?s).*\\S.*", message = "Content must not be blank")
        String content
) {
    public boolean isEmpty() {
        return title == null && content == null;
    }
}
