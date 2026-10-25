package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    /**
     * One page of the customer's wishlist, newest first (id breaks ties so paging is
     * deterministic). Only products the public catalog would show are returned: the product
     * is ACTIVE and its shop is ACTIVE. Product and category are fetched in the same query,
     * so building the page needs no per-item product queries. The caller must pass an
     * unsorted Pageable; ordering is fixed here.
     */
    @Query(
            value = """
                    select wi from WishlistItem wi
                    join fetch wi.product p
                    left join fetch p.category
                    where wi.wishlist.user.id = :userId
                      and p.status = :productStatus
                      and exists (select 1 from Shop s where s.id = p.shopId and s.status = :shopStatus)
                    order by wi.createdAt desc, wi.id desc
                    """,
            countQuery = """
                    select count(wi) from WishlistItem wi
                    where wi.wishlist.user.id = :userId
                      and wi.product.status = :productStatus
                      and exists (select 1 from Shop s where s.id = wi.product.shopId and s.status = :shopStatus)
                    """
    )
    Page<WishlistItem> findVisibleByUserId(
            @Param("userId") UUID userId,
            @Param("productStatus") ProductStatus productStatus,
            @Param("shopStatus") ShopStatus shopStatus,
            Pageable pageable
    );

    /** Ownership is enforced in the query: the entry must belong to the given customer. */
    @Query("select wi from WishlistItem wi where wi.wishlist.user.id = :userId and wi.product.id = :productId")
    Optional<WishlistItem> findOwned(
            @Param("userId") UUID userId,
            @Param("productId") UUID productId
    );

    /** Single indexed lookup for the heart icon; never loads the wishlist. */
    @Query("select count(wi) > 0 from WishlistItem wi where wi.wishlist.user.id = :userId and wi.product.id = :productId")
    boolean existsOwned(
            @Param("userId") UUID userId,
            @Param("productId") UUID productId
    );

    /** Used before a product is deleted so no wishlist row blocks the delete. */
    @Modifying(flushAutomatically = true)
    @Query("delete from WishlistItem wi where wi.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}
