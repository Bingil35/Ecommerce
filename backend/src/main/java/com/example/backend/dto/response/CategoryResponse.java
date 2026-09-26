package com.example.backend.dto.response;

import com.example.backend.entity.Category;

import java.util.UUID;

public class CategoryResponse {

    private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String imageUrl;

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
}