package com.example.backend.repository;

import com.example.backend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    Optional<Shop> findByOwnerUserId(UUID ownerUserId);
    boolean existsByOwnerUserId(UUID ownerUserId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
