package com.example.Social_Media_RestApi_Project.follow.service;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.follow.entity.Follow;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class FollowService {

    /** Followers / following lists are always shown most recent first. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         CurrentUserProvider currentUserProvider,
                         NotificationService notificationService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
    }

    @Transactional
    public void follow(Long targetUserId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        User target = getUser(targetUserId);
        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            throw new ResourceAlreadyExistsException("You already follow this user");
        }
        User follower = getUser(currentUserId);
        followRepository.save(new Follow(follower, target));
        notificationService.notify(target, follower, NotificationType.FOLLOW, null);
    }

    @Transactional
    public void unfollow(Long targetUserId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Follow follow = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You do not follow this user"));
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> getFollowers(Long userId, Pageable pageable) {
        getUser(userId);
        return PageResponse.from(followRepository.findFollowers(userId, newestFirst(pageable)).map(UserSummary::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> getFollowing(Long userId, Pageable pageable) {
        getUser(userId);
        return PageResponse.from(followRepository.findFollowing(userId, newestFirst(pageable)).map(UserSummary::from));
    }

    private static Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
