package com.example.backend.dto.response;

import com.example.backend.entity.ProductMedia;

import java.util.UUID;

public class ProductMediaResponse {

    private UUID id;
    private UUID productId;
    private String mediaType;
    private String mediaUrl;
    private Integer displayOrder;
    private boolean thumbnail;

    public ProductMediaResponse() {
    }

    public ProductMediaResponse(ProductMedia media, String productThumbnailUrl) {
        this(media);
        this.thumbnail = media.getMediaUrl() != null
                && media.getMediaUrl().equals(productThumbnailUrl);
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

    /** true nếu đây là ảnh đại diện hiện tại của sản phẩm. */
    public boolean isThumbnail() {
        return thumbnail;
    }
}