package com.example.Social_Media_RestApi_Project.exception;

/** Thrown when a request is well-formed but violates a business rule (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
