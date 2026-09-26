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
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;

    public ProductResponse() {
    }

    public ProductResponse(Product product) {

        this.id = product.getId();
        this.shopId = product.getShopId();

        if (product.getCategory() != null) {
            this.categoryId =
                    product.getCategory().getId();
        }

        this.name = product.getName();
        this.slug = product.getSlug();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity =
                product.getStockQuantity();

        this.status =
                product.getStatus().name();
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

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public String getStatus() {
        return status;
    }
}