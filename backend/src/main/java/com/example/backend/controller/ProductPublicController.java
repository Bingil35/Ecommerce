package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.service.ProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductPublicController {
    private final ProductService service; public ProductPublicController(ProductService service){this.service=service;}
    @GetMapping public ApiResponse<List<ProductResponse>> list(@RequestParam(required=false) UUID shopId,@RequestParam(required=false) UUID categoryId){List<Product> products=categoryId!=null?service.getActiveProductsByCategory(categoryId):shopId!=null?service.getActiveProductsByShop(shopId):service.getActiveProducts();return ApiResponse.success("Lấy danh sách sản phẩm công khai thành công.",products.stream().map(ProductResponse::new).toList());}
    @GetMapping("/{id}") public ApiResponse<ProductResponse> detail(@PathVariable UUID id){Product p=service.getActiveProductById(id);return ApiResponse.success("Lấy thông tin sản phẩm thành công.",new ProductResponse(p));}
}
