package com.example.backend.dto.response;

import java.math.BigDecimal;

/**
 * Tóm tắt giá/tồn kho của một sản phẩm cho màn hình quản lý của shop owner.
 *
 * - variantCount: tổng số variant (mọi trạng thái).
 * - sku: SKU của variant tạo sớm nhất (đại diện hiển thị trên thẻ sản phẩm).
 * - minPrice, maxPrice, totalStock: chỉ tính trên variant ACTIVE.
 *   totalStock là tổng TỒN KHẢ DỤNG = max(0, quantity - reservedQuantity) của từng biến thể.
 */
public record ProductSummary(
        int variantCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock,
        String sku
) {
    public static final ProductSummary EMPTY =
            new ProductSummary(0, null, null, 0, null);
}
