package com.example.Social_Media_RestApi_Project.user.service;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.common.dto.SortValidator;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.storage.CloudinaryService;
import com.example.Social_Media_RestApi_Project.user.dto.ChangePasswordRequest;
import com.example.Social_Media_RestApi_Project.user.dto.ProfileImageResponse;
import com.example.Social_Media_RestApi_Project.user.dto.ProfileResponse;
import com.example.Social_Media_RestApi_Project.user.dto.UpdateProfileRequest;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("name", "createdAt");

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final CloudinaryService cloudinaryService;

    public UserService(UserRepository userRepository,
                       FollowRepository followRepository,
                       PostRepository postRepository,
                       PasswordEncoder passwordEncoder,
                       CurrentUserProvider currentUserProvider,
                       CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        return toProfile(getUser(currentUserProvider.getCurrentUserId()));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return toProfile(getUser(userId));
    }

    /** Lists users, optionally filtered by a case-insensitive name fragment. */
    @Transactional(readOnly = true)
    public PageResponse<UserSummary> searchUsers(String query, Pageable pageable) {
        SortValidator.requireAllowed(pageable, SORTABLE_PROPERTIES);
        Page<User> users = StringUtils.hasText(query)
                ? userRepository.findByNameContainingIgnoreCase(query.trim(), pageable)
                : userRepository.findAll(pageable);
        return PageResponse.from(users.map(UserSummary::from));
    }

    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("At least one of name, email or bio must be provided");
        }
        User user = getUser(currentUserProvider.getCurrentUserId());

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ResourceAlreadyExistsException("Email is already in use");
            }
            user.setEmail(email);
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        return toProfile(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getUser(currentUserProvider.getCurrentUserId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public ProfileImageResponse uploadProfilePhoto(MultipartFile file) {
        User user = getUser(currentUserProvider.getCurrentUserId());
        String imageUrl = cloudinaryService.uploadProfileImage(file);
        user.setProfileImage(imageUrl);
        return new ProfileImageResponse(imageUrl);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private ProfileResponse toProfile(User user) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        boolean isMe = Objects.equals(user.getId(), currentUserId);
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                isMe ? user.getEmail() : null,
                user.getBio(),
                user.getProfileImage(),
                followRepository.countByFollowingId(user.getId()),
                followRepository.countByFollowerId(user.getId()),
                postRepository.countByAuthorId(user.getId()),
                isMe ? null : followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId()),
                user.getCreatedAt()
        );
    }
}
