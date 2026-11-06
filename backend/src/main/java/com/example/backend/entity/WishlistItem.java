package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "wishlistitems",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_wishlistitems_wishlist_product",
                        columnNames = {"wishlist_id", "product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_wishlistitems_wishlist_created", columnList = "wishlist_id, created_at"),
                @Index(name = "idx_wishlistitems_product_id", columnList = "product_id")
        }
)
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "wishlist_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_wishlistitems_wishlist")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_wishlistitems_product")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public WishlistItem() {
    }

    public WishlistItem(Wishlist wishlist, Product product) {
        this.wishlist = wishlist;
        this.product = product;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Wishlist getWishlist() { return wishlist; }
    public void setWishlist(Wishlist wishlist) { this.wishlist = wishlist; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
