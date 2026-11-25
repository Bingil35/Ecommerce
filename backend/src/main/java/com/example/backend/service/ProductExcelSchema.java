package com.example.backend.service;

import java.util.*;
import java.math.BigDecimal;

/** The only column definition used by the template, export and reader. */
public final class ProductExcelSchema {
    private ProductExcelSchema() {}
    public static final String VERSION = "KALA_PRODUCT_V3";
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_PRODUCTS = 100;
    public static final int MAX_ROWS = 30000;
    public static final int MAX_CELLS = 200000;
    public static final Map<String, List<String>> COLUMNS;
    static {
        Map<String, List<String>> columns = new LinkedHashMap<>();
        columns.put("Products", List.of("product_id", "product_code", "name", "category_name", "brand_name", "description", "thumbnail_url"));
        columns.put("Options", List.of("product_code", "option_name", "option_value", "sort_order"));
        columns.put("Variants", List.of("product_code", "sku", "option_values", "price", "compare_price", "stock_quantity"));
        columns.put("Media", List.of("product_code", "media_type", "url", "sort_order", "is_thumbnail"));
        columns.put("Guide", List.of("key", "value"));
        COLUMNS = Collections.unmodifiableMap(columns);
    }
    public record ExcelRow(String sheet, int row, Map<String, String> cells) {
        public String get(String column) { return cells.getOrDefault(column, ""); }
    }
    public record Issue(String productCode, String sheet, int row, String column, String message) {}
    public record ProductPreview(String productCode, String name, String categoryName, String brandName,
                                 String thumbnailUrl, int optionCount, int variantCount, long stockQuantity) {}
    public record VariantPreview(String productCode, int row, String sku, Map<String,String> optionValues,
                                 BigDecimal price, BigDecimal comparePrice, int stockQuantity) {}
    public record Preview(int total, int valid, int invalid, List<ProductPreview> products,
                          List<VariantPreview> variants, List<Issue> errors, String previewToken, String expiresAt,
                          int variantCount, long stockQuantity, String status) {}
    public record Result(int created, int variantCount, long stockQuantity, String status) {}
}
