package com.example.backend.controller;

import com.example.backend.dto.request.BrandRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.Brand;
import com.example.backend.entity.ProductMediaType;
import com.example.backend.repository.BrandRepository;
import com.example.backend.service.LocalStorageService;
import com.example.backend.service.MediaFileInspector;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BrandController {
    private final BrandRepository brands;
    private final LocalStorageService storage;
    private final MediaFileInspector inspector;

    public BrandController(BrandRepository brands, LocalStorageService storage, MediaFileInspector inspector) {
        this.brands = brands;
        this.storage = storage;
        this.inspector = inspector;
    }

    @GetMapping({"/api/brands", "/api/admin/brands"})
    public ApiResponse<List<Brand>> getAll() {
        return ApiResponse.success("Lấy danh sách thương hiệu thành công.", brands.findAll());
    }

    @GetMapping("/api/admin/brands/{id}")
    public ApiResponse<Brand> getById(@PathVariable UUID id) {
        return ApiResponse.success("Lấy thương hiệu thành công.", findBrand(id));
    }

    @PostMapping(value = "/api/admin/brands/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        MediaFileInspector.Detected detected = inspector.inspect(file);
        if (detected.type() != ProductMediaType.IMAGE) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
        }
        return ApiResponse.success("Đã tải ảnh thương hiệu.", Map.of("url", storage.storeBrandLogo(file, detected.extension())));
    }

    @PostMapping("/api/admin/brands")
    public ResponseEntity<ApiResponse<Brand>> create(@RequestBody BrandRequest request) {
        validate(request);
        String slug = request.getSlug().trim();
        if (brands.existsBySlug(slug)) throw new IllegalArgumentException("Slug thương hiệu đã tồn tại.");
        Brand brand = new Brand();
        brand.setName(request.getName().trim());
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo thương hiệu thành công.", brands.save(brand)));
    }

    @PutMapping("/api/admin/brands/{id}")
    public ApiResponse<Brand> update(@PathVariable UUID id, @RequestBody BrandRequest request) {
        validate(request);
        Brand brand = findBrand(id);
        String slug = request.getSlug().trim();
        brands.findBySlug(slug).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalArgumentException("Slug thương hiệu đã tồn tại."); });
        String previousLogo = brand.getLogoUrl();
        brand.setName(request.getName().trim());
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl());
        Brand saved = brands.save(brand);
        if (!java.util.Objects.equals(previousLogo, saved.getLogoUrl())) storage.deleteBrandLogo(previousLogo);
        return ApiResponse.success("Cập nhật thương hiệu thành công.", saved);
    }

    @DeleteMapping("/api/admin/brands/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Brand brand = findBrand(id);
        brands.delete(brand);
        storage.deleteBrandLogo(brand.getLogoUrl());
        return ApiResponse.success("Xóa thương hiệu thành công.", null);
    }

    private Brand findBrand(UUID id) {
        return brands.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu."));
    }

    private void validate(BrandRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getSlug() == null || request.getSlug().isBlank()) {
            throw new IllegalArgumentException("Tên và slug thương hiệu là bắt buộc.");
        }
    }
}
