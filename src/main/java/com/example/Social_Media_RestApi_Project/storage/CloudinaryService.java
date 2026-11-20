package com.example.Social_Media_RestApi_Project.storage;

import com.cloudinary.Cloudinary;
import com.example.Social_Media_RestApi_Project.config.CloudinaryProperties;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ImageUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Stores images in Cloudinary and returns their public HTTPS URL.
 */
@Service
public class CloudinaryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final String PROFILE_IMAGES_FOLDER = "true-social/profile-images";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    public String uploadProfileImage(MultipartFile file) {
        validateImage(file);
        if (!properties.isConfigured()) {
            throw new ImageUploadException("Image storage is not configured");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "folder", PROFILE_IMAGES_FOLDER,
                    "resource_type", "image"
            ));
            return String.valueOf(result.get("secure_url"));
        } catch (IOException | RuntimeException ex) {
            throw new ImageUploadException("Image upload failed", ex);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPEG, PNG, WEBP and GIF images are allowed");
        }
    }
}
