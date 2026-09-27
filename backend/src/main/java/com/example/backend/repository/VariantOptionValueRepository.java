package com.example.backend.repository;

import com.example.backend.entity.VariantOptionValue;
import com.example.backend.entity.VariantOptionValueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VariantOptionValueRepository
        extends JpaRepository<
            VariantOptionValue,
            VariantOptionValueId
        > {

    List<VariantOptionValue> findByVariantId(
            UUID variantId
    );

    List<VariantOptionValue> findByOptionValueId(
            UUID optionValueId
    );

    void deleteByVariantId(
            UUID variantId
    );
}