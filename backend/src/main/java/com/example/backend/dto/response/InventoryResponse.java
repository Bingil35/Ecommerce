package com.example.backend.dto.response;

import java.util.UUID;

/**
 * Tồn kho của một biến thể.
 *
 * availableQuantity = quantity - reservedQuantity (không âm), là số khách có thể mua.
 * lowStockThreshold: ngưỡng riêng (null nếu chưa đặt, tức dùng mặc định);
 * effectiveLowStockThreshold: ngưỡng đang áp dụng;
 * lowStock: availableQuantity <= effectiveLowStockThreshold.
 */
public record InventoryResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String sku,
        int quantity,
        int reservedQuantity,
        int soldQuantity,
        int availableQuantity,
        Integer lowStockThreshold,
        int effectiveLowStockThreshold,
        boolean lowStock
) {
}
