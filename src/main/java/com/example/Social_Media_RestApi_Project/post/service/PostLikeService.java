package com.example.Social_Media_RestApi_Project.post.service;

import com.example.Social_Media_RestApi_Project.exception.ResourceAlreadyExistsException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.entity.PostLike;
import com.example.Social_Media_RestApi_Project.post.repository.PostLikeRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    public PostLikeService(PostLikeRepository postLikeRepository,
                           PostRepository postRepository,
                           UserRepository userRepository,
                           CurrentUserProvider currentUserProvider,
                           NotificationService notificationService) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
    }

    @Transactional
    public void likePost(Long postId) {
        Long userId = currentUserProvider.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new ResourceAlreadyExistsException("You already liked this post");
        }
        User user = userRepository.getReferenceById(userId);
        postLikeRepository.save(new PostLike(user, post));
        notificationService.notify(post.getAuthor(), user, NotificationType.LIKE, post);
    }

    @Transactional
    public void unlikePost(Long postId) {
        PostLike like = postLikeRepository.findByUserIdAndPostId(currentUserProvider.getCurrentUserId(), postId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not liked this post"));
        postLikeRepository.delete(like);
    }
}
