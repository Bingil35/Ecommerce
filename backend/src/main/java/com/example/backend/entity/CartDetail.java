package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One purchasable line inside a subcart (table cartdetails). The purchasable unit is
 * the ProductVariant; the product is reached through the variant.
 */
@Entity
@Table(
        name = "cartdetails",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cartdetails_subcart_variant", columnNames = {"subcart_id", "variant_id"})
        }
)
@Check(constraints = "quantity > 0")
public class CartDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "subcart_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_cartdetails_subcart")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SubCart subCart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_cartdetails_variant")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CartDetail() {
    }

    public CartDetail(SubCart subCart, ProductVariant variant, Integer quantity) {
        this.subCart = subCart;
        this.variant = variant;
        this.quantity = quantity;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SubCart getSubCart() { return subCart; }
    public void setSubCart(SubCart subCart) { this.subCart = subCart; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
