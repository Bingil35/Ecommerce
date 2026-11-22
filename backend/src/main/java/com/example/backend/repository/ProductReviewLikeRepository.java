package com.example.backend.repository;

import com.example.backend.entity.ProductReviewLike;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewLikeRepository extends JpaRepository<ProductReviewLike, UUID> {
    boolean existsByReview_IdAndUser_Id(UUID reviewId, UUID userId);
    long countByReview_Id(UUID reviewId);
    void deleteByReview_IdAndUser_Id(UUID reviewId, UUID userId);

    @Query("select l.review.id, count(l.id) from ProductReviewLike l where l.review.product.id = :productId group by l.review.id")
    List<Object[]> countByProductIdGrouped(@Param("productId") UUID productId);

    @Query("select l.review.id from ProductReviewLike l where l.review.product.id = :productId and l.user.id = :userId")
    List<UUID> findLikedReviewIds(@Param("productId") UUID productId, @Param("userId") UUID userId);
}
