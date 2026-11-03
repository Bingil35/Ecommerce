package com.example.backend.dto.request;

import com.example.backend.entity.ProductStatus;

/** Chỉ chấp nhận ACTIVE hoặc INACTIVE (kiểm tra ở ProductStatusService). */
public record ChangeProductStatusRequest(ProductStatus status) {
}
