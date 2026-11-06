package com.example.backend.dto.request;

/** Ngưỡng cảnh báo tồn thấp riêng của biến thể. null hoặc 0 = dùng ngưỡng mặc định. */
public record LowStockThresholdRequest(Integer lowStockThreshold) {
}
