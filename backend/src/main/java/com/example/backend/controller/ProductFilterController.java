package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ProductCardResponse;
import com.example.backend.entity.Product;
import com.example.backend.service.ProductFilterService;
import com.example.backend.service.ProductCatalogService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai cho khách hàng lọc sản phẩm.
 *
 * GET /api/products/filter
 *     ?keyword=áo&categoryId=...&brandId=...
 *     &minPrice=100000&maxPrice=500000
 *     &status=ACTIVE&rating=4
 *     &page=0&size=20&sort=price_asc
 *
 * Tất cả tham số đều tùy chọn và có thể kết hợp với nhau.
 */
@RestController
@RequestMapping("/api/products")
public class ProductFilterController {

    private final ProductFilterService filterService;
    private final ProductCatalogService catalogService;

    public ProductFilterController(ProductFilterService filterService, ProductCatalogService catalogService) {
        this.filterService = filterService;
        this.catalogService = catalogService;
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<ProductCardResponse>>> filter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String rating,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String sort
    ) {

        Page<Product> result = filterService.filterProducts(
                keyword,
                categoryId,
                brandId,
                minPrice,
                maxPrice,
                status,
                rating,
                page,
                size,
                sort
        );

        PageResponse<ProductCardResponse> data =
                PageResponse.from(result, catalogService::toPublicCard);

        String message = result.isEmpty()
                ? "Không có sản phẩm nào phù hợp với bộ lọc."
                : "Lọc sản phẩm thành công.";

        return ResponseEntity.ok(
                ApiResponse.success(message, data)
        );
    }
}