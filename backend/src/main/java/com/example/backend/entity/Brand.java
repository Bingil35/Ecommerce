package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "brands")
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name", length = 255)
    private String name;
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;
    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate(){ OffsetDateTime now=OffsetDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=OffsetDateTime.now(); }
    public Brand() {}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getLogoUrl(){return logoUrl;} public void setLogoUrl(String v){logoUrl=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
