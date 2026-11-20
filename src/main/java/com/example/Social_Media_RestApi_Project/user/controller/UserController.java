package com.example.Social_Media_RestApi_Project.user.controller;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.user.dto.ChangePasswordRequest;
import com.example.Social_Media_RestApi_Project.user.dto.ProfileImageResponse;
import com.example.Social_Media_RestApi_Project.user.dto.ProfileResponse;
import com.example.Social_Media_RestApi_Project.user.dto.UpdateProfileRequest;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import com.example.Social_Media_RestApi_Project.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Lists users, or searches them by name with {@code ?query=}. */
    @GetMapping
    public PageResponse<UserSummary> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.searchUsers(query, pageable);
    }

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        return userService.getMyProfile();
    }

    @PatchMapping("/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    @PostMapping(value = "/me/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse uploadProfilePhoto(@RequestPart("file") MultipartFile file) {
        return userService.uploadProfilePhoto(file);
    }

    @GetMapping("/{userId}")
    public ProfileResponse getProfile(@PathVariable Long userId) {
        return userService.getProfile(userId);
    }
}
