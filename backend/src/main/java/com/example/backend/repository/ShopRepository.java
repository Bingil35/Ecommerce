package com.example.backend.repository;

import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id=:id")
    Optional<Shop> findByIdForUpdate(@Param("id") UUID id);
    Optional<Shop> findByOwnerUserId(UUID ownerUserId);
    java.util.List<Shop> findByStatusOrderByShopNameAsc(ShopStatus status);
    Optional<Shop> findBySlugAndStatus(String slug, ShopStatus status);
    boolean existsByOwnerUserId(UUID ownerUserId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
