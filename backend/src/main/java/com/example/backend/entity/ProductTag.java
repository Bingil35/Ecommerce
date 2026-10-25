package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_tags")
public class ProductTag {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name", length = 255)
    private String name;
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate(){ if(createdAt==null) createdAt=OffsetDateTime.now(); }
    public ProductTag() {}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
}
