package com.example.Social_Media_RestApi_Project.comment.repository;

import com.example.Social_Media_RestApi_Project.comment.entity.Comment;
import com.example.Social_Media_RestApi_Project.post.repository.PostCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    /** Finds a comment only if it belongs to the given post. */
    @EntityGraph(attributePaths = {"author", "post"})
    Optional<Comment> findByIdAndPostId(Long id, Long postId);

    @Query("""
            select c.post.id as postId, count(c) as total
            from Comment c
            where c.post.id in :postIds
            group by c.post.id
            """)
    List<PostCountView> countByPostIds(@Param("postIds") Collection<Long> postIds);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
