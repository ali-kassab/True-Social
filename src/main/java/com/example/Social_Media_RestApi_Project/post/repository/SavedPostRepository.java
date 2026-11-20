package com.example.Social_Media_RestApi_Project.post.repository;

import com.example.Social_Media_RestApi_Project.post.entity.Post;
import com.example.Social_Media_RestApi_Project.post.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<SavedPost> findByUserIdAndPostId(Long userId, Long postId);

    /** The user's saved posts (with their authors), most recently saved first. */
    @Query(value = """
            select p from SavedPost s join s.post p join fetch p.author
            where s.user.id = :userId
            order by s.createdAt desc, s.id desc
            """,
            countQuery = "select count(s) from SavedPost s where s.user.id = :userId")
    Page<Post> findSavedPosts(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("delete from SavedPost s where s.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
