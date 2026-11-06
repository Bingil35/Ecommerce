package com.example.backend.controller;

import com.example.backend.dto.request.CheckoutPreviewRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CheckoutPreviewResponse;
import com.example.backend.service.CheckoutPreviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/checkout")
public class CheckoutController {
    private final CheckoutPreviewService service;
    public CheckoutController(CheckoutPreviewService service) { this.service=service; }
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> preview(Authentication authentication,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tính toán checkout thành công. Giá và tồn kho sẽ được xác minh lại khi đặt hàng.",
                service.preview(authentication.getName(),request)));
    }
}
