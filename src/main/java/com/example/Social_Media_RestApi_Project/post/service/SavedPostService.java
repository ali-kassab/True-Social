package com.example.Social_Media_RestApi_Project.post.service;

import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.entity.SavedPost;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.post.repository.SavedPostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public SavedPostService(SavedPostRepository savedPostRepository,
                            PostRepository postRepository,
                            UserRepository userRepository,
                            CurrentUserProvider currentUserProvider) {
        this.savedPostRepository = savedPostRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void savePost(Long postId) {
        Long userId = currentUserProvider.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if (savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new ResourceAlreadyExistsException("You already saved this post");
        }
        savedPostRepository.save(new SavedPost(userRepository.getReferenceById(userId), post));
    }

    @Transactional
    public void unsavePost(Long postId) {
        // Looked up by the current user's id, so users can only remove their own saved posts
        SavedPost savedPost = savedPostRepository
                .findByUserIdAndPostId(currentUserProvider.getCurrentUserId(), postId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not saved this post"));
        savedPostRepository.delete(savedPost);
    }
}
