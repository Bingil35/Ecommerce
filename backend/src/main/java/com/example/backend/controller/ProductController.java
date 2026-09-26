package com.example.backend.controller;

import com.example.backend.dto.request.CreateProductRequest;
import com.example.backend.dto.request.UpdateProductRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shop/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    // =========================
    // CREATE
    // =========================

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody CreateProductRequest request
    ) {

        Product product =
                productService.createProduct(request);

        ProductResponse response =
                new ProductResponse(product);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Tạo sản phẩm thành công.",
                                response
                        )
                );
    }

    // =========================
    // GET ALL BY SHOP
    // =========================

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam UUID shopId
    ) {

        List<ProductResponse> products =
                productService
                        .getProductsByShop(shopId)
                        .stream()
                        .map(ProductResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách sản phẩm thành công.",
                        products
                )
        );
    }

    // =========================
    // GET BY SHOP + STATUS
    // =========================

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByStatus(
            @RequestParam UUID shopId,
            @RequestParam ProductStatus status
    ) {

        List<ProductResponse> products =
                productService
                        .getProductsByShopAndStatus(
                                shopId,
                                status
                        )
                        .stream()
                        .map(ProductResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách sản phẩm theo trạng thái thành công.",
                        products
                )
        );
    }

    // =========================
    // GET BY ID
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable UUID id
    ) {

        Product product =
                productService.getProductById(id);

        ProductResponse response =
                new ProductResponse(product);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy thông tin sản phẩm thành công.",
                        response
                )
        );
    }

    // =========================
    // UPDATE
    // =========================

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request
    ) {

        Product product =
                productService.updateProduct(
                        id,
                        request
                );

        ProductResponse response =
                new ProductResponse(product);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật sản phẩm thành công.",
                        response
                )
        );
    }

    // =========================
    // DELETE
    // =========================

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Xóa sản phẩm thành công.",
                        null
                )
        );
    }
}