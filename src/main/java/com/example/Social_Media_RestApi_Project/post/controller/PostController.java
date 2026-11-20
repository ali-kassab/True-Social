package com.example.Social_Media_RestApi_Project.post.controller;

import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.post.dto.CreatePostRequest;
import com.example.Social_Media_RestApi_Project.post.dto.PostResponse;
import com.example.Social_Media_RestApi_Project.post.dto.UpdatePostRequest;
import com.example.Social_Media_RestApi_Project.post.service.PostLikeService;
import com.example.Social_Media_RestApi_Project.post.service.PostService;
import com.example.Social_Media_RestApi_Project.post.service.SavedPostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final SavedPostService savedPostService;

    public PostController(PostService postService, PostLikeService postLikeService,
                          SavedPostService savedPostService) {
        this.postService = postService;
        this.postLikeService = postLikeService;
        this.savedPostService = savedPostService;
    }

    /** All posts, newest first by default. Supports {@code ?page=&size=&sort=createdAt,desc}. */
    @GetMapping("/posts")
    public PageResponse<PostResponse> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getPosts(pageable);
    }

    /** Posts from the current user and the users they follow. */
    @GetMapping("/posts/feed")
    public PageResponse<PostResponse> getFeed(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getFeed(pageable);
    }

    @GetMapping("/users/{userId}/posts")
    public PageResponse<PostResponse> getPostsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getPostsByUser(userId, pageable);
    }

    @GetMapping("/posts/{postId}")
    public PostResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse created = postService.createPost(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/posts/{postId}")
    public PostResponse updatePost(@PathVariable Long postId, @Valid @RequestBody UpdatePostRequest request) {
        return postService.updatePost(postId, request);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
    }

    @PostMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likePost(@PathVariable Long postId) {
        postLikeService.likePost(postId);
    }

    @DeleteMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePost(@PathVariable Long postId) {
        postLikeService.unlikePost(postId);
    }

    @PostMapping("/posts/{postId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePost(@PathVariable Long postId) {
        savedPostService.savePost(postId);
    }

    @DeleteMapping("/posts/{postId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsavePost(@PathVariable Long postId) {
        savedPostService.unsavePost(postId);
    }

    /** The current user's saved posts, most recently saved first. */
    @GetMapping("/users/me/saved-posts")
    public PageResponse<PostResponse> getSavedPosts(@PageableDefault(size = 10) Pageable pageable) {
        return postService.getSavedPosts(pageable);
    }
}
