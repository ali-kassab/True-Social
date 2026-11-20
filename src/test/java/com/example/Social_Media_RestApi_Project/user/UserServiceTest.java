package com.example.Social_Media_RestApi_Project.user;

import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.storage.CloudinaryService;
import com.example.Social_Media_RestApi_Project.support.TestData;
import com.example.Social_Media_RestApi_Project.user.dto.ChangePasswordRequest;
import com.example.Social_Media_RestApi_Project.user.dto.ProfileResponse;
import com.example.Social_Media_RestApi_Project.user.dto.UpdateProfileRequest;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import com.example.Social_Media_RestApi_Project.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private PostRepository postRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks private UserService userService;

    private final User me = TestData.user(1L);

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(me));
    }

    @Test
    void getMyProfile_includesEmailAndCounts() {
        when(followRepository.countByFollowingId(1L)).thenReturn(5L);
        when(followRepository.countByFollowerId(1L)).thenReturn(3L);
        when(postRepository.countByAuthorId(1L)).thenReturn(7L);

        ProfileResponse profile = userService.getMyProfile();

        assertThat(profile.email()).isEqualTo("user1@example.com");
        assertThat(profile.followersCount()).isEqualTo(5L);
        assertThat(profile.followingCount()).isEqualTo(3L);
        assertThat(profile.postsCount()).isEqualTo(7L);
        assertThat(profile.followedByCurrentUser()).isNull();
    }

    @Test
    void getProfile_ofAnotherUser_hidesEmail() {
        User other = TestData.user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        ProfileResponse profile = userService.getProfile(2L);

        assertThat(profile.email()).isNull();
        assertThat(profile.followedByCurrentUser()).isTrue();
    }

    @Test
    void updateMyProfile_normalizesEmailAndUpdatesFields() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        ProfileResponse profile = userService.updateMyProfile(
                new UpdateProfileRequest(" New Name ", " NEW@Example.com ", "Hello there"));

        assertThat(me.getName()).isEqualTo("New Name");
        assertThat(me.getEmail()).isEqualTo("new@example.com");
        assertThat(me.getBio()).isEqualTo("Hello there");
        assertThat(profile.email()).isEqualTo("new@example.com");
    }

    @Test
    void updateMyProfile_withTakenEmail_isConflict() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateMyProfile(new UpdateProfileRequest(null, "taken@example.com", null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void updateMyProfile_withNoFields_isBadRequest() {
        assertThatThrownBy(() -> userService.updateMyProfile(new UpdateProfileRequest(null, null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePassword_withWrongCurrentPassword_isBadRequest() {
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("wrong", "newPassword1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void changePassword_withSamePassword_isBadRequest() {
        when(passwordEncoder.matches("samePassword", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("samePassword", "samePassword")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePassword_storesNewHash() {
        when(passwordEncoder.matches("oldPassword", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        userService.changePassword(new ChangePasswordRequest("oldPassword", "newPassword1"));

        assertThat(me.getPassword()).isEqualTo("new-hash");
    }
}
