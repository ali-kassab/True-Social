package com.example.Social_Media_RestApi_Project.block.controller;

import com.example.Social_Media_RestApi_Project.block.service.BlockService;
import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
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
@RequestMapping("/api/users")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long userId) {
        blockService.block(userId);
    }

    @DeleteMapping("/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long userId) {
        blockService.unblock(userId);
    }

    @GetMapping("/me/blocked")
    public PageResponse<UserSummary> getBlockedUsers(@PageableDefault(size = 20) Pageable pageable) {
        return blockService.getBlockedUsers(pageable);
    }
}
