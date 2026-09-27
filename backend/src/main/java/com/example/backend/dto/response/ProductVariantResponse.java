package com.example.backend.dto.response;

import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.VariantOptionValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductVariantResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private String status;
    private List<UUID> optionValueIds;

    public ProductVariantResponse() {
    }

    public ProductVariantResponse(
            ProductVariant variant,
            List<VariantOptionValue> optionValues
    ) {
        this.id = variant.getId();
        this.productId =
                variant.getProduct().getId();
        this.sku = variant.getSku();
        this.price = variant.getPrice();
        this.stockQuantity =
                variant.getStockQuantity();
        this.imageUrl = variant.getImageUrl();
        this.status =
                variant.getStatus().name();

        this.optionValueIds =
                optionValues.stream()
                        .map(item ->
                                item.getOptionValue().getId()
                        )
                        .toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public List<UUID> getOptionValueIds() {
        return optionValueIds;
    }
}