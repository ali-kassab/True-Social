package com.example.Social_Media_RestApi_Project.auth.dto;

import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {

    public static LoginResponse bearer(String token, long expiresInSeconds, UserSummary user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
