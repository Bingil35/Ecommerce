package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopOwnerProductStatsResponse;
import com.example.backend.service.ShopOwnerProductStatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop-owner/product-stats")
public class ShopOwnerProductStatsController {
    private final ShopOwnerProductStatsService service;
    public ShopOwnerProductStatsController(ShopOwnerProductStatsService service) { this.service = service; }
    @GetMapping
    public ApiResponse<ShopOwnerProductStatsResponse> get(Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "revenueLifetime") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Product statistics loaded.", service.get(authentication.getName(), keyword, sort, page, size));
    }
}
