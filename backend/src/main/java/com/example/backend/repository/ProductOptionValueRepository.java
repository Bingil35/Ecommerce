package com.example.backend.repository;

import com.example.backend.entity.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductOptionValueRepository
        extends JpaRepository<ProductOptionValue, UUID> {

    List<ProductOptionValue> findByProductOptionIdOrderBySortOrderAsc(
            UUID optionId
    );

    boolean existsByProductOptionIdAndValue(
            UUID optionId,
            String value
    );

    boolean existsByProductOptionIdAndValueAndIdNot(
            UUID optionId,
            String value,
            UUID id
    );
}