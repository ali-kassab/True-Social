package com.example.Social_Media_RestApi_Project.follow.controller;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.follow.service.FollowService;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long userId) {
        followService.follow(userId);
    }

    @DeleteMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long userId) {
        followService.unfollow(userId);
    }

    @GetMapping("/followers")
    public PageResponse<UserSummary> getFollowers(@PathVariable Long userId,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        return followService.getFollowers(userId, pageable);
    }

    @GetMapping("/following")
    public PageResponse<UserSummary> getFollowing(@PathVariable Long userId,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        return followService.getFollowing(userId, pageable);
    }
}
