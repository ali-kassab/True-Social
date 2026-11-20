package com.example.Social_Media_RestApi_Project.comment.service;

import com.example.Social_Media_RestApi_Project.comment.dto.CommentRequest;
import com.example.Social_Media_RestApi_Project.comment.dto.CommentResponse;
import com.example.Social_Media_RestApi_Project.comment.entity.Comment;
import com.example.Social_Media_RestApi_Project.comment.repository.CommentRepository;
import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.common.dto.SortValidator;
import com.example.Social_Media_RestApi_Project.exception.ForbiddenException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.entity.NotificationType;
import com.example.Social_Media_RestApi_Project.notification.service.NotificationService;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
public class CommentService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "updatedAt");

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          CurrentUserProvider currentUserProvider,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        return PageResponse.from(commentRepository.findByPostId(postId,
                SortValidator.requireAllowed(pageable, SORTABLE_PROPERTIES)).map(CommentResponse::from));
    }

    @Transactional(readOnly = true)
    public CommentResponse getComment(Long postId, Long commentId) {
        return CommentResponse.from(getCommentOrThrow(postId, commentId));
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        User author = userRepository.getReferenceById(currentUserProvider.getCurrentUserId());

        Comment comment = commentRepository.save(new Comment(request.content().trim(), author, post));
        notificationService.notify(post.getAuthor(), author, NotificationType.COMMENT, post);
        return CommentResponse.from(comment);
    }

    /** Only the comment author (or an admin) can edit a comment. */
    @Transactional
    public CommentResponse updateComment(Long postId, Long commentId, CommentRequest request) {
        Comment comment = getCommentOrThrow(postId, commentId);
        if (!currentUserProvider.canModify(comment.getAuthor().getId())) {
            throw new ForbiddenException("You can only edit your own comments");
        }
        comment.setContent(request.content().trim());
        return CommentResponse.from(commentRepository.saveAndFlush(comment));
    }

    /** The comment author, the post author (moderating their post) or an admin can delete a comment. */
    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Comment comment = getCommentOrThrow(postId, commentId);
        boolean isPostAuthor = Objects.equals(comment.getPost().getAuthor().getId(),
                currentUserProvider.getCurrentUserId());
        if (!isPostAuthor && !currentUserProvider.canModify(comment.getAuthor().getId())) {
            throw new ForbiddenException("You can only delete your own comments or comments on your posts");
        }
        commentRepository.delete(comment);
    }

    private Comment getCommentOrThrow(Long postId, Long commentId) {
        return commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    }
}
