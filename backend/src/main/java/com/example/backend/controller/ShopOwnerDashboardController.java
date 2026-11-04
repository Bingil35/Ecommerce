package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopOwnerDashboardResponse;
import com.example.backend.service.ShopOwnerDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop-owner/dashboard")
public class ShopOwnerDashboardController {
    private final ShopOwnerDashboardService service;
    public ShopOwnerDashboardController(ShopOwnerDashboardService service) { this.service = service; }
    @GetMapping
    public ApiResponse<ShopOwnerDashboardResponse> get(Authentication authentication, @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success("Dashboard statistics loaded.", service.get(authentication.getName(), days));
    }
}
