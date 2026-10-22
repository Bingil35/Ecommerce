package com.example.backend.repository;
import com.example.backend.entity.VariantOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VariantOptionValueRepository extends JpaRepository<VariantOptionValue, UUID> {
    List<VariantOptionValue> findByVariantId(UUID variantId);
    List<VariantOptionValue> findByOptionValueId(UUID optionValueId);
    void deleteByVariantId(UUID variantId);

    @Query("""
            select vov from VariantOptionValue vov
            join fetch vov.optionValue ov
            join fetch ov.productOption po
            where vov.variant.id in :variantIds
            order by po.sortOrder asc, po.name asc
            """)
    List<VariantOptionValue> findWithOptionsByVariantIdIn(
            @Param("variantIds") Collection<UUID> variantIds
    );
}
