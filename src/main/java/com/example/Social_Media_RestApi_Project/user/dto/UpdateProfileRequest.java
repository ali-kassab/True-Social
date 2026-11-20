package com.example.Social_Media_RestApi_Project.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: only non-null fields are changed. */
public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,

        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Size(max = 160, message = "Bio must be at most 160 characters")
        String bio
) {
    public boolean isEmpty() {
        return name == null && email == null && bio == null;
    }
}
