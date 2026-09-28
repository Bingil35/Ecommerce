package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.service.ProductSearchService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai cho khách hàng tìm kiếm sản phẩm.
 * GET /api/products/search?keyword=áo thun&page=0&size=20&sort=newest
 */
@RestController
@RequestMapping("/api/products")
public class ProductSearchController {

    private final ProductSearchService searchService;

    public ProductSearchController(
            ProductSearchService searchService
    ) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Page<Product> result =
                searchService.search(keyword, page, size, sort);

        PageResponse<ProductResponse> data =
                PageResponse.from(result, ProductResponse::new);

        String message = result.isEmpty()
                ? "Không tìm thấy sản phẩm phù hợp."
                : "Tìm kiếm sản phẩm thành công.";

        return ResponseEntity.ok(
                ApiResponse.success(message, data)
        );
    }
}