package com.example.backend.dto.request;

/**
 * Nhập kho: quantity > 0.
 * note tùy chọn (số phiếu nhập, nhà cung cấp...), chỉ được ghi vào nhật ký ứng dụng.
 */
public record StockInRequest(Integer quantity, String note) {
}
