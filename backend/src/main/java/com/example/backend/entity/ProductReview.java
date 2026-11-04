package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "productreviews")
public class ProductReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id") private ProductVariant variant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "suborder_id") private SubOrder subOrder;
    @Column(name = "rating", nullable = false) private Short rating;
    @Column(name = "content", columnDefinition = "text") private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { OffsetDateTime now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product value) { product = value; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant value) { variant = value; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public SubOrder getSubOrder() { return subOrder; }
    public void setSubOrder(SubOrder value) { subOrder = value; }
    public Short getRating() { return rating; }
    public void setRating(Short value) { rating = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
