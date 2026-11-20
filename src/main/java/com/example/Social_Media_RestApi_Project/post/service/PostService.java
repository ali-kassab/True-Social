package com.example.Social_Media_RestApi_Project.post.service;

import com.example.Social_Media_RestApi_Project.comment.repository.CommentRepository;
import com.example.Social_Media_RestApi_Project.common.dto.PageResponse;
import com.example.Social_Media_RestApi_Project.common.dto.SortValidator;
import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import com.example.Social_Media_RestApi_Project.exception.ForbiddenException;
import com.example.Social_Media_RestApi_Project.exception.ResourceNotFoundException;
import com.example.Social_Media_RestApi_Project.notification.repository.NotificationRepository;
import com.example.Social_Media_RestApi_Project.post.dto.CreatePostRequest;
import com.example.Social_Media_RestApi_Project.post.dto.PostResponse;
import com.example.Social_Media_RestApi_Project.post.dto.UpdatePostRequest;
import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.repository.PostCountView;
import com.example.Social_Media_RestApi_Project.post.repository.PostLikeRepository;
import com.example.Social_Media_RestApi_Project.post.repository.PostRepository;
import com.example.Social_Media_RestApi_Project.post.repository.SavedPostRepository;
import com.example.Social_Media_RestApi_Project.security.CurrentUserProvider;
import com.example.Social_Media_RestApi_Project.user.dto.UserSummary;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "updatedAt", "title");

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final SavedPostRepository savedPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       SavedPostRepository savedPostRepository,
                       CommentRepository commentRepository,
                       NotificationRepository notificationRepository,
                       UserRepository userRepository,
                       CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.savedPostRepository = savedPostRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(Pageable pageable) {
        return toPageResponse(postRepository.findAll(sortable(pageable)));
    }

    /** Posts from the current user and the users they follow. */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getFeed(Pageable pageable) {
        return toPageResponse(postRepository.findFeed(currentUserProvider.getCurrentUserId(), sortable(pageable)));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return toPageResponse(postRepository.findByAuthorId(userId, sortable(pageable)));
    }

    /** The current user's saved posts, most recently saved first (sort parameters are ignored). */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getSavedPosts(Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return toPageResponse(savedPostRepository.findSavedPosts(currentUserProvider.getCurrentUserId(), unsorted));
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        return toResponse(getPostOrThrow(postId));
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        // The author always comes from the JWT, never from the request body
        User author = userRepository.getReferenceById(currentUserProvider.getCurrentUserId());
        Post post = postRepository.save(new Post(author, request.title().trim(), request.content().trim()));
        return toResponse(post);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("At least one of title or content must be provided");
        }
        Post post = getPostOrThrow(postId);
        checkCanModify(post, "update");

        if (request.title() != null) {
            post.setTitle(request.title().trim());
        }
        if (request.content() != null) {
            post.setContent(request.content().trim());
        }
        // flush so the response contains the new updatedAt timestamp
        return toResponse(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = getPostOrThrow(postId);
        checkCanModify(post, "delete");

        // Remove dependent rows first so foreign keys never block the delete
        notificationRepository.deleteByPostId(postId);
        postLikeRepository.deleteByPostId(postId);
        savedPostRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private static Pageable sortable(Pageable pageable) {
        return SortValidator.requireAllowed(pageable, SORTABLE_PROPERTIES);
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }

    private void checkCanModify(Post post, String action) {
        if (!currentUserProvider.canModify(post.getAuthor().getId())) {
            throw new ForbiddenException("You can only " + action + " your own posts");
        }
    }

    private PostResponse toResponse(Post post) {
        return toResponses(List.of(post)).get(0);
    }

    private PageResponse<PostResponse> toPageResponse(Page<Post> posts) {
        return PageResponse.from(posts, toResponses(posts.getContent()));
    }

    /**
     * Builds responses for a list of posts using three queries in total
     * (likes count, comments count, liked-by-me) instead of three queries per post.
     */
    private List<PostResponse> toResponses(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Long currentUserId = currentUserProvider.getCurrentUserId();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Long> likeCounts = toMap(postLikeRepository.countByPostIds(postIds));
        Map<Long, Long> commentCounts = toMap(commentRepository.countByPostIds(postIds));
        Set<Long> likedByMe = postLikeRepository.findLikedPostIds(currentUserId, postIds);

        return posts.stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getContent(),
                        UserSummary.from(post.getAuthor()),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        commentCounts.getOrDefault(post.getId(), 0L),
                        likedByMe.contains(post.getId()),
                        post.getCreatedAt(),
                        post.getUpdatedAt()))
                .toList();
    }

    private static Map<Long, Long> toMap(List<PostCountView> counts) {
        return counts.stream().collect(Collectors.toMap(PostCountView::getPostId, PostCountView::getTotal));
    }
}
