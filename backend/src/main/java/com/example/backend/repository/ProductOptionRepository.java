package com.example.backend.repository;

import com.example.backend.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductOptionRepository
        extends JpaRepository<ProductOption, UUID> {

    List<ProductOption> findByProductIdOrderByNameAsc(
            UUID productId
    );

    boolean existsByProductIdAndName(
            UUID productId,
            String name
    );

    boolean existsByProductIdAndNameAndIdNot(
            UUID productId,
            String name,
            UUID id
    );
}