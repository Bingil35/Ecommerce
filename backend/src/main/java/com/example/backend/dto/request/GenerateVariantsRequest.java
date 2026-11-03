package com.example.backend.dto.request;

import java.util.List;

/** Các biến thể tường minh được tạo ra từ tích Descartes do khách hàng chuẩn bị. */
public class GenerateVariantsRequest {
    private List<CreateProductVariantRequest> variants;
    public List<CreateProductVariantRequest> getVariants() { return variants; }
    public void setVariants(List<CreateProductVariantRequest> variants) { this.variants = variants; }
}
