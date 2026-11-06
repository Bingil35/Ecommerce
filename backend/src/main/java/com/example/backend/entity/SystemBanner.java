package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;

@Entity
@Table(name = "systembanners")
public class SystemBanner {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(length = 255) private String title;
    @Column(name = "image_url", columnDefinition = "text") private String imageUrl;
    private Short priority;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "banner_status_enum") private BannerStatus status = BannerStatus.ACTIVE;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = BannerStatus.ACTIVE; if (priority == null) priority = 0; }
    public UUID getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public Short getPriority(){return priority;} public void setPriority(Short v){priority=v;}
    public BannerStatus getStatus(){return status;} public void setStatus(BannerStatus v){status=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
