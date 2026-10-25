package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    Optional<Wishlist> findByUserId(UUID userId);
}
