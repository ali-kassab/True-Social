package com.example.Social_Media_RestApi_Project.exception;

/** Thrown when the authenticated user is not allowed to modify a resource (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
