package com.example.Social_Media_RestApi_Project.notification.repository;

import com.example.Social_Media_RestApi_Project.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"actor", "post"})
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "post"})
    Page<Notification> findByRecipientIdAndReadFalse(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.read = false")
    int markAllAsRead(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("delete from Notification n where n.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
