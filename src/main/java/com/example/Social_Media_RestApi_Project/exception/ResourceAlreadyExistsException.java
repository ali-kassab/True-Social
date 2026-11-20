package com.example.Social_Media_RestApi_Project.exception;

/** Thrown when creating something that already exists (HTTP 409). */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
