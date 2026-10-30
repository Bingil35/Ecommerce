package com.example.backend.controller;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.WishlistItemResponse;
import com.example.backend.dto.response.WishlistStatusResponse;
import com.example.backend.service.WishlistService;

/**
 * Customer wishlist. /api/customer/** is restricted to CUSTOMER in SecurityConfig.
 * The caller is always resolved from the authenticated principal, never from the path.
 *
 * <pre>
 * POST   /api/customer/wishlist/{productId}         201 added, 200 already saved
 * GET    /api/customer/wishlist?page=0&amp;size=20      newest first, size max 50
 * DELETE /api/customer/wishlist/{productId}         200 removed, 404 not in your wishlist
 * GET    /api/customer/wishlist/{productId}/status  data.inWishlist true/false
 * </pre>
 *
 * <pre>
 * curl -X POST http://localhost:8080/api/customer/wishlist/PRODUCT_UUID \
 *      -H "Authorization: Bearer TOKEN"
 * curl "http://localhost:8080/api/customer/wishlist?page=0&amp;size=20" \
 *      -H "Authorization: Bearer TOKEN"
 * curl http://localhost:8080/api/customer/wishlist/PRODUCT_UUID/status \
 *      -H "Authorization: Bearer TOKEN"
 * curl -X DELETE http://localhost:8080/api/customer/wishlist/PRODUCT_UUID \
 *      -H "Authorization: Bearer TOKEN"
 * </pre>
 *
 * List items: data.items[].id, addedAt, product{id, name, slug, thumbnailUrl, minPrice, price,
 * comparePrice, ...}; plus page, size, totalElements, totalPages, last.
 */
@RestController
@RequestMapping("/api/customer/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /** 201 when the product was newly saved, 200 when it was already in the wishlist. */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistStatusResponse>> add(
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        WishlistService.AddResult result =
                wishlistService.addProduct(authentication.getName(), productId);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created()
                ? "Thêm sản phẩm vào danh sách yêu thích thành công."
                : "Sản phẩm đã có trong danh sách yêu thích.";

        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, result.status()));
    }

    @GetMapping
    public ApiResponse<PageResponse<WishlistItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy danh sách yêu thích thành công.",
                wishlistService.getWishlist(authentication.getName(), page, size)
        );
    }

    @GetMapping("/{productId}/status")
    public ApiResponse<WishlistStatusResponse> status(
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Kiểm tra danh sách yêu thích thành công.",
                wishlistService.getStatus(authentication.getName(), productId)
        );
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<WishlistStatusResponse> remove(
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Xóa sản phẩm khỏi danh sách yêu thích thành công.",
                wishlistService.removeProduct(authentication.getName(), productId)
        );
    }

    /**
     * A concurrent first-time wishlist creation or duplicate insert that slipped past the
     * customer lock is reported as a conflict, not as an internal error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The wishlist was updated concurrently. Please retry."));
    }
}
