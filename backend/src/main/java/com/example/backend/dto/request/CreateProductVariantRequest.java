package com.example.backend.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateProductVariantRequest {

    private UUID productId;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private List<UUID> optionValueIds;

    public CreateProductVariantRequest() {
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
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

    public List<UUID> getOptionValueIds() {
        return optionValueIds;
    }

    public void setOptionValueIds(List<UUID> optionValueIds) {
        this.optionValueIds = optionValueIds;
    }
}