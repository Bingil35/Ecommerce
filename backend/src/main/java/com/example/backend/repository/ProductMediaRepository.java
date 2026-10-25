package com.example.backend.repository;

import com.example.backend.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductMediaRepository
        extends JpaRepository<ProductMedia, UUID> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(
            UUID productId
    );

    List<ProductMedia> findByProductIdInOrderBySortOrderAsc(
            Collection<UUID> productIds
    );
}