package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthorizationTestController {

    @GetMapping("/customer/test")
    public ApiResponse<String> customerTest() {
        return ApiResponse.success(
                "Customer authorization successful.",
                "You have CUSTOMER access."
        );
    }

    @GetMapping("/shop-owner/test")
    public ApiResponse<String> shopOwnerTest() {
        return ApiResponse.success(
                "Shop Owner authorization successful.",
                "You have SHOP_OWNER access."
        );
    }

    @GetMapping("/admin/test")
    public ApiResponse<String> adminTest() {
        return ApiResponse.success(
                "Admin authorization successful.",
                "You have ADMIN access."
        );
    }
}