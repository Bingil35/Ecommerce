package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.SubCart;

public interface SubCartRepository extends JpaRepository<SubCart, UUID> {

    @Query("select sc from SubCart sc where sc.cart.id = :cartId and sc.shop.id = :shopId")
    Optional<SubCart> findByCartIdAndShopId(@Param("cartId") UUID cartId, @Param("shopId") UUID shopId);
}
