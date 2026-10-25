package com.example.backend.controller;

import com.example.backend.dto.request.ProductTagRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.ProductTag;
import com.example.backend.repository.ProductTagRepository;
import com.example.backend.repository.ProductTagMappingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/product-tags")
public class ProductTagController {
    private final ProductTagRepository tags; private final ProductTagMappingRepository mappings;
    public ProductTagController(ProductTagRepository tags,ProductTagMappingRepository mappings){this.tags=tags;this.mappings=mappings;}
    @GetMapping public ApiResponse<List<ProductTag>> getAll(){return ApiResponse.success("Lấy danh sách thẻ sản phẩm thành công.",tags.findAll());}
    @GetMapping("/{id}") public ApiResponse<ProductTag> getById(@PathVariable UUID id){return ApiResponse.success("Lấy thẻ sản phẩm thành công.",find(id));}
    @PostMapping public ResponseEntity<ApiResponse<ProductTag>> create(@RequestBody ProductTagRequest r){validate(r);if(tags.existsBySlug(r.getSlug().trim()))throw new IllegalArgumentException("Slug thẻ sản phẩm đã tồn tại.");ProductTag t=new ProductTag();t.setName(r.getName().trim());t.setSlug(r.getSlug().trim());return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo thẻ sản phẩm thành công.",tags.save(t)));}
    @PutMapping("/{id}") public ApiResponse<ProductTag> update(@PathVariable UUID id,@RequestBody ProductTagRequest r){validate(r);ProductTag t=find(id);if(tags.existsBySlug(r.getSlug().trim())&&!t.getSlug().equals(r.getSlug().trim()))throw new IllegalArgumentException("Slug thẻ sản phẩm đã tồn tại.");t.setName(r.getName().trim());t.setSlug(r.getSlug().trim());return ApiResponse.success("Cập nhật thẻ sản phẩm thành công.",tags.save(t));}
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable UUID id){ProductTag t=find(id);if(mappings.findAll().stream().anyMatch(m->m.getTagId().equals(id)))throw new IllegalArgumentException("Không thể xóa thẻ đang được gắn với sản phẩm.");tags.delete(t);return ApiResponse.success("Xóa thẻ sản phẩm thành công.",null);}
    private ProductTag find(UUID id){return tags.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy thẻ sản phẩm."));}
    private void validate(ProductTagRequest r){if(r==null||r.getName()==null||r.getName().isBlank()||r.getSlug()==null||r.getSlug().isBlank())throw new IllegalArgumentException("Tên và slug thẻ sản phẩm là bắt buộc.");}
}
