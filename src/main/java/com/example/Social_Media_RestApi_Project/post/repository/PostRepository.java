package com.example.Social_Media_RestApi_Project.post.repository;

import com.example.Social_Media_RestApi_Project.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

/**
 * The {@link EntityGraph} annotations fetch each post's author in the same query,
 * avoiding one extra query per post (the N+1 problem) when building responses.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(@NonNull Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    /** Posts written by the user or by anyone the user follows. */
    @EntityGraph(attributePaths = "author")
    @Query(value = """
            select p from Post p
            where p.author.id = :userId
               or p.author.id in (select f.following.id from Follow f where f.follower.id = :userId)
            """,
            countQuery = """
            select count(p) from Post p
            where p.author.id = :userId
               or p.author.id in (select f.following.id from Follow f where f.follower.id = :userId)
            """)
    Page<Post> findFeed(@Param("userId") Long userId, Pageable pageable);

    long countByAuthorId(Long authorId);
}
