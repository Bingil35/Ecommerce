package com.example.backend.dto.request;

import com.example.backend.entity.ProductVariantStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class UpdateProductVariantRequest {

    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private ProductVariantStatus status;
    private List<UUID> optionValueIds;

    public UpdateProductVariantRequest() {
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ProductVariantStatus getStatus() {
        return status;
    }

    public void setStatus(ProductVariantStatus status) {
        this.status = status;
    }

    public List<UUID> getOptionValueIds() {
        return optionValueIds;
    }

    public void setOptionValueIds(List<UUID> optionValueIds) {
        this.optionValueIds = optionValueIds;
    }
}