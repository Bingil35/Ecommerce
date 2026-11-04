package com.example.backend.entity;

/**
 * Trạng thái sản phẩm (và biến thể, dùng chung kiểu product_status_enum ở DB).
 *
 * PENDING  : chờ Admin duyệt (sản phẩm mới, hoặc sản phẩm đã sửa nội dung quan trọng)
 * REJECTED : Admin từ chối
 * ACTIVE   : đã được duyệt và đang bán
 * INACTIVE : đã được duyệt nhưng đang ẩn
 *
 * Biến thể (ProductVariant) chỉ dùng ACTIVE / INACTIVE.
 * Yêu cầu schema: product_status_enum phải có đủ 4 giá trị
 * (apply query db/08 - Migration product status enum.sql; Hibernate ddl-auto
 * does not add values to an existing PostgreSQL named enum).
 */
public enum ProductStatus {
    PENDING,
    REJECTED,
    ACTIVE,
    INACTIVE
}
