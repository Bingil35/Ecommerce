package com.example.backend.repository;

import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(
            UUID productId
    );

    List<ProductVariant> findByProductIdAndStatusOrderByCreatedAtAsc(
            UUID productId,
            ProductVariantStatus status
    );

    boolean existsByProductIdAndSku(
            UUID productId,
            String sku
    );

    boolean existsByProductIdAndSkuAndIdNot(
            UUID productId,
            String sku,
            UUID id
    );
}