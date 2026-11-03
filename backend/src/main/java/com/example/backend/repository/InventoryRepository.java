package com.example.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findFirstByVariantId(UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variant.id = :variantId")
    Optional<Inventory> findFirstByVariantIdForUpdate(@Param("variantId") UUID variantId);

    void deleteByVariantId(UUID variantId);
    List<Inventory> findByVariantIdIn(Collection<UUID> variantIds);
}
