package com.example.backend.dto.response;

import java.util.UUID;


public class WishlistStatusResponse {

    private final UUID productId;
    private final boolean inWishlist;

    public WishlistStatusResponse(UUID productId, boolean inWishlist) {
        this.productId = productId;
        this.inWishlist = inWishlist;
    }

    public UUID getProductId() {
        return productId;
    }

    public boolean isInWishlist() {
        return inWishlist;
    }
}
