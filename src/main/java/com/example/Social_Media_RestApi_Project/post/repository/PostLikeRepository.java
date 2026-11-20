package com.example.Social_Media_RestApi_Project.post.repository;

import com.example.Social_Media_RestApi_Project.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    /** Like counts for a whole page of posts in a single query. */
    @Query("""
            select l.post.id as postId, count(l) as total
            from PostLike l
            where l.post.id in :postIds
            group by l.post.id
            """)
    List<PostCountView> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /** Which of the given posts the user has liked, in a single query. */
    @Query("select l.post.id from PostLike l where l.user.id = :userId and l.post.id in :postIds")
    Set<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Modifying
    @Query("delete from PostLike l where l.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
