package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviewreplies", uniqueConstraints = @UniqueConstraint(name = "uq_reviewreplies_review_id", columnNames = "review_id"))
public class ReviewReply {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id") private ProductReview review;
    @Column(name = "shop_id", nullable = false) private UUID shopId;
    @Column(name = "content", columnDefinition = "text", nullable = false) private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public ProductReview getReview() { return review; }
    public void setReview(ProductReview value) { review = value; }
    public UUID getShopId() { return shopId; }
    public void setShopId(UUID value) { shopId = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
