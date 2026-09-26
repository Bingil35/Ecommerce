package com.example.backend.controller;

import com.example.backend.dto.request.CreateCategoryRequest;
import com.example.backend.dto.request.UpdateCategoryRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CategoryResponse;
import com.example.backend.service.CategoryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response =
                categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Tạo danh mục thành công.",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categories =
                categoryService.getAllCategories();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách danh mục thành công.",
                        categories
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable UUID id
    ) {
        CategoryResponse response =
                categoryService.getCategoryById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy thông tin danh mục thành công.",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response =
                categoryService.updateCategory(
                        id,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật danh mục thành công.",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id
    ) {
        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Xóa danh mục thành công.",
                        null
                )
        );
    }
}