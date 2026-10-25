package com.example.backend.controller;

import com.example.backend.dto.request.BrandRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.Brand;
import com.example.backend.repository.BrandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class BrandController {
    private final BrandRepository brands;
    public BrandController(BrandRepository brands){this.brands=brands;}

    @GetMapping({"/api/brands", "/api/admin/brands"})
    public ApiResponse<List<Brand>> getAll(){return ApiResponse.success("Lấy danh sách thương hiệu thành công.", brands.findAll());}

    @GetMapping("/api/admin/brands/{id}")
    public ApiResponse<Brand> getById(@PathVariable UUID id){return ApiResponse.success("Lấy thương hiệu thành công.", brands.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy thương hiệu.")));}

    @PostMapping("/api/admin/brands")
    public ResponseEntity<ApiResponse<Brand>> create(@RequestBody BrandRequest request){
        validate(request); if(brands.existsBySlug(request.getSlug().trim())) throw new IllegalArgumentException("Slug thương hiệu đã tồn tại.");
        Brand b=new Brand(); b.setName(request.getName().trim()); b.setSlug(request.getSlug().trim()); b.setLogoUrl(request.getLogoUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo thương hiệu thành công.", brands.save(b)));
    }

    @PutMapping("/api/admin/brands/{id}")
    public ApiResponse<Brand> update(@PathVariable UUID id,@RequestBody BrandRequest request){
        validate(request); Brand b=brands.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy thương hiệu."));
        brands.findBySlug(request.getSlug().trim()).filter(other->!other.getId().equals(id)).ifPresent(other->{throw new IllegalArgumentException("Slug thương hiệu đã tồn tại.");});
        b.setName(request.getName().trim()); b.setSlug(request.getSlug().trim()); b.setLogoUrl(request.getLogoUrl()); return ApiResponse.success("Cập nhật thương hiệu thành công.",brands.save(b));
    }
    @DeleteMapping("/api/admin/brands/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id){if(!brands.existsById(id)) throw new IllegalArgumentException("Không tìm thấy thương hiệu."); brands.deleteById(id); return ApiResponse.success("Xóa thương hiệu thành công.",null);}
    private void validate(BrandRequest r){if(r==null||r.getName()==null||r.getName().isBlank()||r.getSlug()==null||r.getSlug().isBlank()) throw new IllegalArgumentException("Tên và slug thương hiệu là bắt buộc.");}
}
