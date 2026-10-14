package com.example.backend.dto.response;

import com.example.backend.entity.Category;
import com.example.backend.entity.CategoryStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CategoryResponse {

    private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String imageUrl;
    private CategoryStatus status;
    private Short sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CategoryResponse() {
    }

    public CategoryResponse(Category category) {
        this.id = category.getId();

        if (category.getParent() != null) {
            this.parentId = category.getParent().getId();
        }

        this.name = category.getName();
        this.slug = category.getSlug();
        this.imageUrl = category.getImageUrl();
        this.status = category.getStatus();
        this.sortOrder = category.getSortOrder();
        this.createdAt = category.getCreatedAt();
        this.updatedAt = category.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}