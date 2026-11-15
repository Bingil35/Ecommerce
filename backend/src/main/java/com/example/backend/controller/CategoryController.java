package com.example.backend.controller;

import com.example.backend.dto.request.CreateCategoryRequest;
import com.example.backend.dto.request.UpdateCategoryRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CategoryResponse;
import com.example.backend.service.CategoryService;
import com.example.backend.service.LocalStorageService;
import com.example.backend.service.MediaFileInspector;
import com.example.backend.entity.ProductMediaType;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.entity.Category;
import com.example.backend.entity.CategoryStatus;
import com.example.backend.repository.CategoryRepository;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final LocalStorageService storage;
    private final MediaFileInspector inspector;

    public CategoryController(
            CategoryService categoryService,
            CategoryRepository categoryRepository,
            LocalStorageService storage,
            MediaFileInspector inspector
    ) {
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.storage = storage;
        this.inspector = inspector;
    }

    @PostMapping(value="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String,String>> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        MediaFileInspector.Detected detected=inspector.inspect(file);
        if(detected.type()!=ProductMediaType.IMAGE) throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
        return ApiResponse.success("Đã tải ảnh danh mục.",Map.of("url",storage.storeCategoryImage(file,detected.extension())));
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

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> reorderRootCategories(@RequestBody CategoryOrderRequest request) {
        List<CategoryResponse> categories = categoryService.reorderRootCategories(
                request == null ? null : request.ids()
        );
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật thứ tự danh mục gốc.", categories));
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateStatus(@PathVariable UUID id, @RequestBody CategoryStatusRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));
        category.setStatus(CategoryStatus.valueOf(request.status()));
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái danh mục.", new CategoryResponse(categoryRepository.save(category))));
    }

    public record CategoryStatusRequest(String status) {}
    public record CategoryOrderRequest(List<UUID> ids) {}
}
