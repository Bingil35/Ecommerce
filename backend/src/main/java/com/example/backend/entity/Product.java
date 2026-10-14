package com.example.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shop_id")
    private UUID shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "product_status_enum")
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "is_featured")
    private Boolean featured;

    @Column(name = "rating_average", precision = 2, scale = 1)
    private java.math.BigDecimal ratingAverage;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Product() {}
    @PrePersist protected void onCreate() { OffsetDateTime now=OffsetDateTime.now(); createdAt=now; updatedAt=now; if(status==null) status=ProductStatus.ACTIVE; if(ratingCount==null) ratingCount=0; }
    @PreUpdate protected void onUpdate() { updatedAt=OffsetDateTime.now(); }
    public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
    public UUID getShopId(){return shopId;} public void setShopId(UUID v){shopId=v;}
    public Category getCategory(){return category;} public void setCategory(Category v){category=v;}
    public UUID getBrandId(){return brandId;} public void setBrandId(UUID v){brandId=v;}
    public String getThumbnailUrl(){return thumbnailUrl;} public void setThumbnailUrl(String v){thumbnailUrl=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public ProductStatus getStatus(){return status;} public void setStatus(ProductStatus v){status=v;}
    public Boolean getFeatured(){return featured;} public void setFeatured(Boolean v){featured=v;}
    public java.math.BigDecimal getRatingAverage(){return ratingAverage;} public void setRatingAverage(java.math.BigDecimal v){ratingAverage=v;}
    public Integer getRatingCount(){return ratingCount;} public void setRatingCount(Integer v){ratingCount=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
