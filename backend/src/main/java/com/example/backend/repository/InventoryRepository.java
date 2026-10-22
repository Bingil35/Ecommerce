package com.example.backend.repository;
import com.example.backend.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findFirstByVariantId(UUID variantId);
    void deleteByVariantId(UUID variantId);

    List<Inventory> findByVariantIdIn(Collection<UUID> variantIds);
}
