package com.example.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_tag_mappings")
public class ProductTagMapping {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "tag_id", nullable = false)
    private UUID tagId;
    public ProductTagMapping() {}
    public ProductTagMapping(UUID productId, UUID tagId){this.productId=productId;this.tagId=tagId;}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getProductId(){return productId;} public void setProductId(UUID v){productId=v;}
    public UUID getTagId(){return tagId;} public void setTagId(UUID v){tagId=v;}
}
