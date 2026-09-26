package com.example.backend.repository;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    boolean existsByShopIdAndSlug(
            UUID shopId,
            String slug
    );

    boolean existsByShopIdAndSlugAndIdNot(
            UUID shopId,
            String slug,
            UUID id
    );

    List<Product> findByShopIdOrderByCreatedAtDesc(
            UUID shopId
    );

    List<Product> findByShopIdAndStatusOrderByCreatedAtDesc(
            UUID shopId,
            ProductStatus status
    );

    List<Product> findByCategoryIdOrderByCreatedAtDesc(
            UUID categoryId
    );
}