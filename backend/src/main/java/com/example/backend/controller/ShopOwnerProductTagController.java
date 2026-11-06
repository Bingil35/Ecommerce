package com.example.backend.controller;

import java.util.List;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.ProductTag;
import com.example.backend.repository.ProductTagRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop-owner/product-tags")
public class ShopOwnerProductTagController {
    private final ProductTagRepository tags;

    public ShopOwnerProductTagController(ProductTagRepository tags) {
        this.tags = tags;
    }

    @GetMapping
    public ApiResponse<List<ProductTag>> suggestions() {
        return ApiResponse.success("Tag gợi ý cho sản phẩm", tags.findAll(org.springframework.data.domain.Sort.by("name")));
    }
}
