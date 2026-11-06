package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Maps the lightweight history table used by RecentlyViewedService's JDBC queries. */
@Entity
@Table(name = "recentlyviewedproduct")
public class RecentlyViewedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "viewed_at", nullable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    void setViewedAt() {
        if (viewedAt == null) viewedAt = OffsetDateTime.now();
    }
}
