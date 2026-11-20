package com.example.Social_Media_RestApi_Project.exception;

/** Thrown when the external image storage cannot store a file (HTTP 502). */
public class ImageUploadException extends RuntimeException {

    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
