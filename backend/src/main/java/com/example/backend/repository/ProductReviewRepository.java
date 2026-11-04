package com.example.backend.repository;

import com.example.backend.entity.ProductReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(UUID productId);
    @Query("select r from ProductReview r where r.product.shopId = :shopId order by r.createdAt desc")
    List<ProductReview> findByShopId(@Param("shopId") UUID shopId);
    boolean existsBySubOrder_IdAndProduct_IdAndUser_Id(UUID subOrderId, UUID productId, UUID userId);
    @Query("select r from ProductReview r where r.id = :id and r.product.shopId = :shopId")
    Optional<ProductReview> findByIdAndShopId(@Param("id") UUID id, @Param("shopId") UUID shopId);
}
