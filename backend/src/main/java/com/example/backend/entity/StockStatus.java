package com.example.backend.entity;

/**
 * Tình trạng tồn kho của sản phẩm, tính theo tổng tồn kho của các variant ACTIVE.
 *
 * IN_STOCK     : tổng tồn > 0 (bao gồm cả sắp hết hàng)
 * LOW_STOCK    : 0 < tổng tồn <= ngưỡng sắp hết (app.inventory.low-stock-threshold)
 * OUT_OF_STOCK : tổng tồn = 0 (kể cả sản phẩm chưa có variant)
 */
public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}
