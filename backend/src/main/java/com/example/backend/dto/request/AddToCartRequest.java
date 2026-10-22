package com.example.backend.dto.request;

import java.util.UUID;

public class AddToCartRequest {

    private UUID productId;
    private UUID variantId;
    private Integer quantity;

    public AddToCartRequest() {
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
