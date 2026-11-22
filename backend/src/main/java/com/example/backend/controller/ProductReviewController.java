package com.example.backend.controller;

import com.example.backend.dto.request.CreateProductReviewRequest;
import com.example.backend.dto.request.ReviewReplyRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductReviewLikeResponse;
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
    public ApiResponse<List<ProductReviewResponse>> productReviews(@PathVariable UUID productId, Authentication auth) {
        String email = auth == null || "anonymousUser".equals(auth.getName()) ? null : auth.getName();
        return ApiResponse.success("L\u1ea5y \u0111\u00e1nh gi\u00e1 th\u00e0nh c\u00f4ng.", service.productReviews(productId, email));
    }

    @PutMapping("/api/customer/reviews/{reviewId}/like")
    public ApiResponse<ProductReviewLikeResponse> like(@PathVariable UUID reviewId, Authentication auth) {
        return ApiResponse.success("\u0110\u00e3 th\u00edch \u0111\u00e1nh gi\u00e1.", service.like(auth.getName(), reviewId));
    }

    @DeleteMapping("/api/customer/reviews/{reviewId}/like")
    public ApiResponse<ProductReviewLikeResponse> unlike(@PathVariable UUID reviewId, Authentication auth) {
        return ApiResponse.success("\u0110\u00e3 b\u1ecf th\u00edch \u0111\u00e1nh gi\u00e1.", service.unlike(auth.getName(), reviewId));
    }

    @PostMapping("/api/customer/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductReviewResponse> create(@PathVariable UUID productId, Authentication auth,
            @Valid @RequestBody CreateProductReviewRequest request) {
        return ApiResponse.success("G\u1eedi \u0111\u00e1nh gi\u00e1 th\u00e0nh c\u00f4ng.", service.create(auth.getName(), productId, request));
    }

    @GetMapping("/api/shop-owner/reviews")
    public ApiResponse<List<ProductReviewResponse>> ownerReviews(Authentication auth) {
        return ApiResponse.success("L\u1ea5y \u0111\u00e1nh gi\u00e1 c\u1eeda h\u00e0ng th\u00e0nh c\u00f4ng.", service.ownerReviews(auth.getName()));
    }

    @PostMapping("/api/shop-owner/reviews/{reviewId}/replies")
    public ApiResponse<ProductReviewResponse> reply(@PathVariable UUID reviewId, Authentication auth,
            @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.success("\u0110\u00e3 ph\u1ea3n h\u1ed3i \u0111\u00e1nh gi\u00e1.", service.reply(auth.getName(), reviewId, request.content()));
    }
}
