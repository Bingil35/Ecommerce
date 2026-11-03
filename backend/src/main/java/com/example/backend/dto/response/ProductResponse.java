package com.example.backend.dto.response;

import com.example.backend.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductResponse {

    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private Integer variantCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private String sku;
    private String categoryName;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime updatedAt;
    private String status;
    private UUID brandId;
    private String thumbnailUrl;
    private Boolean featured;
    private java.math.BigDecimal ratingAverage;
    private Integer ratingCount;

    public ProductResponse() {
    }

    public ProductResponse(Product product) {
        this(product, null);
    }

    public ProductResponse(Product product, ProductSummary summary) {
        this(product, summary, null);
    }

    public ProductResponse(Product product, ProductSummary summary, String categoryName) {

        this.id = product.getId();
        this.shopId = product.getShopId();

        if (product.getCategory() != null) {
            this.categoryId =
                    product.getCategory().getId();
        }

        this.name = product.getName();
        this.slug = product.getSlug();
        this.description = product.getDescription();
        // Giá và tồn kho thuộc ProductVariant/Inventory; chỉ có khi truyền summary.
        if (summary != null) {
            this.variantCount = summary.variantCount();
            this.minPrice = summary.minPrice();
            this.maxPrice = summary.maxPrice();
            this.totalStock = summary.totalStock();
            this.sku = summary.sku();
        }
        this.categoryName = categoryName;
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();

        this.status = product.getStatus().name();
        this.brandId = product.getBrandId();
        this.thumbnailUrl = product.getThumbnailUrl();
        this.featured = product.getFeatured();
        this.ratingAverage = product.getRatingAverage();
        this.ratingCount = product.getRatingCount();
    }

    public UUID getId() {
        return id;
    }

    public UUID getShopId() {
        return shopId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Integer getVariantCount() { return variantCount; }
    public BigDecimal getMinPrice() { return minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public Integer getTotalStock() { return totalStock; }
    public String getSku() { return sku; }
    public String getCategoryName() { return categoryName; }
    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public java.time.OffsetDateTime getUpdatedAt() { return updatedAt; }

    public String getStatus() { return status; }
    public UUID getBrandId(){return brandId;}
    public String getThumbnailUrl(){return thumbnailUrl;}
    public Boolean getFeatured(){return featured;}
    public java.math.BigDecimal getRatingAverage(){return ratingAverage;}
    public Integer getRatingCount(){return ratingCount;}
}