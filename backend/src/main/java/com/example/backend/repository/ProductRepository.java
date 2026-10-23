package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;

public interface ProductRepository
        extends JpaRepository<Product, UUID>,
                JpaSpecificationExecutor<Product> {

    java.util.Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Product> findByShopIdOrderByCreatedAtDesc(
            UUID shopId
    );

    List<Product> findByShopIdAndStatusOrderByCreatedAtDesc(
            UUID shopId,
            ProductStatus status
    );

    List<Product> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId);
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
    List<Product> findByCategoryIdAndStatusOrderByCreatedAtDesc(UUID categoryId, ProductStatus status);
}