package com.example.backend.repository;

import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    Optional<Shop> findByOwnerUserId(UUID ownerUserId);
    java.util.List<Shop> findByStatusOrderByShopNameAsc(ShopStatus status);
    boolean existsByOwnerUserId(UUID ownerUserId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
