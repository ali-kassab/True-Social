package com.example.Social_Media_RestApi_Project.user.dto;

import com.example.Social_Media_RestApi_Project.user.entity.User;

/** Public, minimal view of a user (used in lists, posts, comments, notifications). */
public record UserSummary(Long id, String name, String profileImageUrl) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getProfileImage());
    }
}
