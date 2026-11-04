package com.example.backend.controller;

import com.example.backend.dto.request.CreateProductReviewRequest;
import com.example.backend.dto.request.ReviewReplyRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductReviewResponse;
import com.example.backend.service.ProductReviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductReviewController {
    private final ProductReviewService service;
    public ProductReviewController(ProductReviewService service) { this.service = service; }

    @GetMapping("/api/products/{productId}/reviews")
    public ApiResponse<List<ProductReviewResponse>> productReviews(@PathVariable UUID productId) {
        return ApiResponse.success("Lấy đánh giá thành công.", service.productReviews(productId));
    }

    @PostMapping("/api/customer/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductReviewResponse> create(@PathVariable UUID productId, Authentication auth,
            @Valid @RequestBody CreateProductReviewRequest request) {
        return ApiResponse.success("Gửi đánh giá thành công.", service.create(auth.getName(), productId, request));
    }

    @GetMapping("/api/shop-owner/reviews")
    public ApiResponse<List<ProductReviewResponse>> ownerReviews(Authentication auth) {
        return ApiResponse.success("Lấy đánh giá cửa hàng thành công.", service.ownerReviews(auth.getName()));
    }

    @PostMapping("/api/shop-owner/reviews/{reviewId}/replies")
    public ApiResponse<ProductReviewResponse> reply(@PathVariable UUID reviewId, Authentication auth,
            @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.success("Đã phản hồi đánh giá.", service.reply(auth.getName(), reviewId, request.content()));
    }
}
