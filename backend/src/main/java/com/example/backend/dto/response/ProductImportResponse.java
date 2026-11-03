package com.example.backend.dto.response;

import java.util.List;

/**
 * Kết quả nhập sản phẩm từ CSV. Mỗi dòng được xử lý độc lập:
 * dòng lỗi không làm hỏng các dòng hợp lệ.
 */
public record ProductImportResponse(
        int total,
        int created,
        int failed,
        List<RowError> errors
) {
    /** row: số thứ tự dòng trong file (dòng tiêu đề là dòng 1). */
    public record RowError(int row, String message) {
    }
}
