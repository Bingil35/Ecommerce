package com.example.backend.repository;

import com.example.backend.entity.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductOptionValueRepository
        extends JpaRepository<ProductOptionValue, UUID> {

    List<ProductOptionValue> findByOptionIdOrderByValueAsc(
            UUID optionId
    );

    boolean existsByOptionIdAndValue(
            UUID optionId,
            String value
    );

    boolean existsByOptionIdAndValueAndIdNot(
            UUID optionId,
            String value,
            UUID id
    );
}