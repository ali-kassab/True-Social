package com.example.Social_Media_RestApi_Project.block.repository;

import com.example.Social_Media_RestApi_Project.block.entity.Block;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** True if either user has blocked the other. */
    @Query("""
            select count(b) > 0 from Block b
            where (b.blocker.id = :userA and b.blocked.id = :userB)
               or (b.blocker.id = :userB and b.blocked.id = :userA)
            """)
    boolean existsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query(value = "select b.blocked from Block b where b.blocker.id = :userId",
            countQuery = "select count(b) from Block b where b.blocker.id = :userId")
    Page<User> findBlockedUsers(@Param("userId") Long userId, Pageable pageable);
}
