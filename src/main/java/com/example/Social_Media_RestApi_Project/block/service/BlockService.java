package com.example.Social_Media_RestApi_Project.block.service;

import com.example.Social_Media_RestApi_Project.block.entity.Block;
import com.example.Social_Media_RestApi_Project.block.repository.BlockRepository;
import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.follow.repository.FollowRepository;
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
public class BlockService {

    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public BlockService(BlockRepository blockRepository,
                        FollowRepository followRepository,
                        UserRepository userRepository,
                        CurrentUserProvider currentUserProvider) {
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /** Blocks a user and removes any follow relationship between the two users. */
    @Transactional
    public void block(Long targetUserId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BadRequestException("You cannot block yourself");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));
        if (blockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            throw new ResourceAlreadyExistsException("You already blocked this user");
        }
        blockRepository.save(new Block(userRepository.getReferenceById(currentUserId), target));

        followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerIdAndFollowingId(targetUserId, currentUserId)
                .ifPresent(followRepository::delete);
    }

    @Transactional
    public void unblock(Long targetUserId) {
        // Looked up by the current user as blocker, so users can only remove their own blocks
        Block block = blockRepository.findByBlockerIdAndBlockedId(currentUserProvider.getCurrentUserId(), targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not blocked this user"));
        blockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> getBlockedUsers(Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(blockRepository
                .findBlockedUsers(currentUserProvider.getCurrentUserId(), newestFirst)
                .map(UserSummary::from));
    }
}
