package com.example.backend.dto.response;

import com.example.backend.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;

/** Public, read-only product representation for customer/guest catalog cards. */
public class ProductCardResponse {
    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private UUID brandId;
    private String name;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer stockQuantity;
    private Boolean featured;
    private BigDecimal ratingAverage;
    private Integer ratingCount;

    public ProductCardResponse(Product product, BigDecimal minPrice, BigDecimal maxPrice, Integer stockQuantity) {
        this.id = product.getId();
        this.shopId = product.getShopId();
        this.categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        this.brandId = product.getBrandId();
        this.name = product.getName();
        this.slug = product.getSlug();
        this.description = product.getDescription();
        this.thumbnailUrl = product.getThumbnailUrl();
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.stockQuantity = stockQuantity;
        this.featured = product.getFeatured();
        this.ratingAverage = product.getRatingAverage();
        this.ratingCount = product.getRatingCount();
    }
    public UUID getId(){return id;} public UUID getShopId(){return shopId;} public UUID getCategoryId(){return categoryId;}
    public UUID getBrandId(){return brandId;} public String getName(){return name;} public String getSlug(){return slug;}
    public String getDescription(){return description;} public String getThumbnailUrl(){return thumbnailUrl;}
    public BigDecimal getMinPrice(){return minPrice;} public BigDecimal getMaxPrice(){return maxPrice;} public BigDecimal getPrice(){return minPrice;}
    public Integer getStockQuantity(){return stockQuantity;} public Boolean getFeatured(){return featured;}
    public BigDecimal getRatingAverage(){return ratingAverage;} public Integer getRatingCount(){return ratingCount;}
}
