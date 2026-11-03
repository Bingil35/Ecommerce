package com.example.backend.controller;

import com.example.backend.dto.request.CheckoutPreviewRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CheckoutPreviewResponse;
import com.example.backend.dto.response.CouponResponse;
import com.example.backend.entity.CouponScope;
import com.example.backend.service.CheckoutPreviewService;
import com.example.backend.service.CouponQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/coupons")
public class CouponController {
    private final CouponQueryService couponQueryService;
    private final CheckoutPreviewService checkoutPreviewService;
    public CouponController(CouponQueryService couponQueryService, CheckoutPreviewService checkoutPreviewService) {
        this.couponQueryService = couponQueryService; this.checkoutPreviewService = checkoutPreviewService;
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailable(Authentication authentication,
            @RequestParam(required = false) CouponScope scope) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách coupon khả dụng thành công.",
                couponQueryService.getAvailable(authentication.getName(), scope)));
    }
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> validateForCart(Authentication authentication,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon hợp lệ với giỏ hàng hiện tại.",
                checkoutPreviewService.preview(authentication.getName(), request)));
    }
}
