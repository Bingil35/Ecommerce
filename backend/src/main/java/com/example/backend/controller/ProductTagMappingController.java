package com.example.backend.controller;

import com.example.backend.dto.request.ProductTagMappingRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.ProductTagMapping;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductTagRepository;
import com.example.backend.repository.ProductTagMappingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/product-tag-mappings")
public class ProductTagMappingController {
    private final ProductTagMappingRepository mappings; private final ProductRepository products; private final ProductTagRepository tags;
    public ProductTagMappingController(ProductTagMappingRepository mappings,ProductRepository products,ProductTagRepository tags){this.mappings=mappings;this.products=products;this.tags=tags;}
    @GetMapping public ApiResponse<List<ProductTagMapping>> getAll(@RequestParam(required=false) UUID productId){return ApiResponse.success("Lấy liên kết thẻ sản phẩm thành công.",productId==null?mappings.findAll():mappings.findAllByProductId(productId));}
    @PostMapping public ResponseEntity<ApiResponse<ProductTagMapping>> create(@RequestBody ProductTagMappingRequest r){
        if(r==null||r.getProductId()==null||r.getTagId()==null)throw new IllegalArgumentException("productId và tagId là bắt buộc.");
        if(!products.existsById(r.getProductId()))throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        if(!tags.existsById(r.getTagId()))throw new IllegalArgumentException("Không tìm thấy thẻ sản phẩm.");
        if(mappings.existsByProductIdAndTagId(r.getProductId(),r.getTagId()))throw new IllegalArgumentException("Thẻ đã được gắn với sản phẩm.");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Gắn thẻ cho sản phẩm thành công.",mappings.save(new ProductTagMapping(r.getProductId(),r.getTagId()))));
    }
    @DeleteMapping public ApiResponse<Void> delete(@RequestParam UUID productId,@RequestParam UUID tagId){ProductTagMapping m=mappings.findByProductIdAndTagId(productId,tagId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy liên kết thẻ sản phẩm."));mappings.delete(m);return ApiResponse.success("Gỡ thẻ khỏi sản phẩm thành công.",null);}
}
