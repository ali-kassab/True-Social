package com.example.Social_Media_RestApi_Project.exception;

/** Thrown when a requested resource does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super("%s with id %d was not found".formatted(resourceName, id));
    }
}
