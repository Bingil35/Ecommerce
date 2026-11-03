package com.example.backend.exception;

/**
 * Vi phạm quy tắc nghiệp vụ (trạng thái không hợp lệ, dữ liệu giao dịch cần bảo toàn...).
 * Trả về HTTP 409 (Conflict).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
