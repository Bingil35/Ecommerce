package com.example.backend.dto.response;

import com.example.backend.entity.ProductOption;

import java.util.UUID;

public class ProductOptionResponse {

    private UUID id;
    private UUID productId;
    private String name;

    public ProductOptionResponse() {
    }

    public ProductOptionResponse(ProductOption option) {
        this.id = option.getId();
        this.productId =
                option.getProduct().getId();
        this.name = option.getName();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }
}