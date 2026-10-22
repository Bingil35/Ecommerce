package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.CartDetail;
import com.example.backend.entity.SubCart;

public interface CartDetailRepository extends JpaRepository<CartDetail, UUID> {

    /**
     * Loads every line of a cart with its subcart, shop, variant, product and primary
     * media in one query, so the response needs no per-line queries.
     */
    @Query("""
            select cd from CartDetail cd
            join fetch cd.subCart sc
            join fetch sc.shop
            join fetch cd.variant v
            join fetch v.product p
            left join fetch v.primaryMedia pm
            where sc.cart.id = :cartId
            order by sc.createdAt asc, cd.createdAt asc
            """)
    List<CartDetail> findDetailedByCartId(@Param("cartId") UUID cartId);

    /** Ownership is enforced in the query: the line must belong to the given customer. */
    @Query("select cd from CartDetail cd where cd.id = :detailId and cd.subCart.cart.user.id = :userId")
    Optional<CartDetail> findOwnedById(@Param("detailId") UUID detailId, @Param("userId") UUID userId);

    @Query("select cd from CartDetail cd where cd.subCart.id = :subCartId and cd.variant.id = :variantId")
    Optional<CartDetail> findBySubCartIdAndVariantId(
            @Param("subCartId") UUID subCartId,
            @Param("variantId") UUID variantId
    );

    @Query("select distinct cd.subCart from CartDetail cd where cd.variant.id = :variantId")
    List<SubCart> findSubCartsByVariantId(@Param("variantId") UUID variantId);

    boolean existsBySubCartId(UUID subCartId);

    void deleteByVariantId(UUID variantId);
}
