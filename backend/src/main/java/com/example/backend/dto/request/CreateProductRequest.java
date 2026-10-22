package com.example.backend.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateProductRequest {

    private UUID shopId;
    private UUID categoryId;
    private UUID brandId;
    private String thumbnailUrl;
    private Boolean featured;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;

    public CreateProductRequest() {
    }

    public UUID getShopId() {
        return shopId;
    }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getBrandId(){return brandId;}
    public void setBrandId(UUID v){brandId=v;}
    public String getThumbnailUrl(){return thumbnailUrl;}
    public void setThumbnailUrl(String v){thumbnailUrl=v;}
    public Boolean getFeatured(){return featured;}
    public void setFeatured(Boolean v){featured=v;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}