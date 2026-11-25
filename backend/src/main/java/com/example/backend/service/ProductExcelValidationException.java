package com.example.backend.service;

/** Expected confirm-time failure with the same located errors as preview. */
public class ProductExcelValidationException extends RuntimeException {
    private final ProductExcelSchema.Preview preview;
    public ProductExcelValidationException(ProductExcelSchema.Preview preview) {
        super("Dữ liệu đã thay đổi. Vui lòng sửa file và xem trước lại."); this.preview=preview;
    }
    public ProductExcelSchema.Preview getPreview() { return preview; }
}
