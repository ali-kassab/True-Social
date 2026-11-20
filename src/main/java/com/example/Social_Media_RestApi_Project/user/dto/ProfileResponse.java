package com.example.Social_Media_RestApi_Project.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Full user profile. {@code email} is only included when users view their own profile,
 * and {@code followedByCurrentUser} only when they view someone else's.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        Long id,
        String name,
        String email,
        String bio,
        String profileImageUrl,
        long followersCount,
        long followingCount,
        long postsCount,
        Boolean followedByCurrentUser,
        Instant createdAt
) {
}
