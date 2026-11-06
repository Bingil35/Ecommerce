package com.example.backend.dto.response;

/**
 * Số lượng sản phẩm cho các tab của trang quản lý sản phẩm.
 *
 * all        = pending + rejected + selling + outOfStock + hidden
 * pending    : PENDING (chờ Admin duyệt)
 * rejected   : REJECTED (bị từ chối)
 * selling    : ACTIVE và còn hàng (tồn khả dụng > 0)
 * outOfStock : ACTIVE và hết hàng (tồn khả dụng = 0)
 * hidden     : INACTIVE (đã duyệt, đang ẩn)
 * lowStock   : ACTIVE và sắp hết hàng (tập con của selling)
 *
 * Tồn khả dụng = quantity - reservedQuantity (mỗi biến thể ACTIVE, không âm).
 */
public record ProductStatsResponse(
        long all,
        long pending,
        long rejected,
        long selling,
        long outOfStock,
        long hidden,
        long lowStock
) {
}
