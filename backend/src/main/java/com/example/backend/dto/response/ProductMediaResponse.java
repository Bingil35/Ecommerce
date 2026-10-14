package com.example.backend.dto.response;

import com.example.backend.entity.ProductMedia;

import java.util.UUID;

public class ProductMediaResponse {

    private UUID id;
    private UUID productId;
    private String mediaType;
    private String mediaUrl;
    private Integer displayOrder;

    public ProductMediaResponse() {
    }

    public ProductMediaResponse(ProductMedia media) {
        this.id = media.getId();
        this.productId = media.getProduct().getId();
        this.mediaType = media.getMediaType().name();
        this.mediaUrl = media.getMediaUrl();
        this.displayOrder = media.getDisplayOrder();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}