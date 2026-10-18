package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductCardResponse;
import com.example.backend.dto.response.ProductDetailResponse;
import com.example.backend.service.ProductCatalogService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public read-only product APIs shared by guest and customer. */
@RestController
@RequestMapping("/api/products")
public class ProductPublicController {
    private final ProductCatalogService catalog;
    public ProductPublicController(ProductCatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    public ApiResponse<List<ProductCardResponse>> list(
            @RequestParam(required=false) UUID shopId,
            @RequestParam(required=false) UUID categoryId,
            @RequestParam(required=false, name="q") String keyword,
            @RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,
            @RequestParam(required=false) Boolean featured,
            @RequestParam(required=false, defaultValue="newest") String sortBy) {
        return ApiResponse.success("Lấy danh sách sản phẩm thành công.",
                catalog.list(shopId, categoryId, keyword, minPrice, maxPrice, featured, sortBy));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công.", catalog.detail(id));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<ProductCardResponse>> related(
            @PathVariable UUID id,
            @RequestParam(required=false, defaultValue="8") int limit) {
        return ApiResponse.success("Lấy sản phẩm liên quan thành công.", catalog.related(id, limit));
    }
}
