package com.example.backend.repository;

import com.example.backend.entity.ReviewReply;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {
    List<ReviewReply> findByReview_IdOrderByCreatedAtAsc(UUID reviewId);
    Optional<ReviewReply> findFirstByReview_IdOrderByCreatedAtAsc(UUID reviewId);
}
