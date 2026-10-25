package com.example.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public class WishlistItemResponse {

    private final UUID id;
    private final OffsetDateTime addedAt;
    private final ProductCardResponse product;

    public WishlistItemResponse(UUID id, OffsetDateTime addedAt, ProductCardResponse product) {
        this.id = id;
        this.addedAt = addedAt;
        this.product = product;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public ProductCardResponse getProduct() {
        return product;
    }
}
