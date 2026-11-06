package com.example.backend.dto.request;

import com.example.backend.entity.ProductStatus;

import java.util.List;
import java.util.UUID;

/**
 * Thao tác hàng loạt trên sản phẩm.
 * status chỉ dùng cho bulk-status, bỏ qua ở bulk-delete.
 */
public record BulkProductRequest(
        List<UUID> ids,
        ProductStatus status
) {
}
