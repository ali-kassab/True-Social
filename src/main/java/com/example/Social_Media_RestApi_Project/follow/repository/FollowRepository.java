package com.example.Social_Media_RestApi_Project.follow.repository;

import com.example.Social_Media_RestApi_Project.follow.entity.Follow;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    @Query(value = "select f.follower from Follow f where f.following.id = :userId",
            countQuery = "select count(f) from Follow f where f.following.id = :userId")
    Page<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "select f.following from Follow f where f.follower.id = :userId",
            countQuery = "select count(f) from Follow f where f.follower.id = :userId")
    Page<User> findFollowing(@Param("userId") Long userId, Pageable pageable);
}
