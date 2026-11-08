package com.example.backend.dto.request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** JSON part of the multipart update. Omitted media are retained. */
public record EditProductRequest(
        UpdateProductRequest product,
        List<UUID> deletedMediaIds,
        UUID thumbnailMediaId,
        Integer thumbnailImageIndex,
        Map<UUID, UpdateProductVariantRequest> variants
) {}
