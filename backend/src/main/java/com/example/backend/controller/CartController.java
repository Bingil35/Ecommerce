package com.example.backend.controller;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CartResponse;
import com.example.backend.service.CartService;

/**
 * Customer shopping cart. /api/customer/** is restricted to CUSTOMER in SecurityConfig.
 * Ownership is enforced in CartService: the caller is always resolved from the
 * authenticated principal, never from the request body or path.
 *
 * <p>Every endpoint returns the whole cart in the same shape (ApiResponse&lt;CartResponse&gt;),
 * so the client can render straight from the JSON without any other request:
 * <pre>
 * data.cartId, data.totalQuantity, data.subtotal
 * data.shops[].subCartId, shopId, shopSlug, shopName, totalQuantity, subtotal
 * data.shops[].items[].id, productId, productSlug, productName,
 *                      variantId, variantName ("Màu: Đen, Size: L"), selectedOptions[{optionName, value, ...}],
 *                      sku, imageUrl, unitPrice, comparePrice, quantity, lineSubtotal,
 *                      availableStock, available
 * </pre>
 * Money is BigDecimal at scale 2 (HALF_UP). Totals count available lines only. A line whose
 * product, variant or shop is inactive, or whose stock is below its quantity, is still
 * returned with available = false instead of being removed.
 *
 * <p>Example calls (replace TOKEN with the accessToken from POST /api/auth/login):
 * <pre>
 * # login: body {"identifier":"customer@example.com","password":"..."} -> data.accessToken
 * curl -X POST http://localhost:8080/api/auth/login \
 *      -H "Content-Type: application/json" \
 *      -d '{"identifier":"customer@example.com","password":"secret"}'
 *
 * # get the cart
 * curl http://localhost:8080/api/customer/cart \
 *      -H "Authorization: Bearer TOKEN"
 *
 * # add a variant (201 for a new line, 200 when the quantity of an existing line grows)
 * curl -X POST http://localhost:8080/api/customer/cart/items \
 *      -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" \
 *      -d '{"productId":"PRODUCT_UUID","variantId":"VARIANT_UUID","quantity":2}'
 *
 * # set the quantity of a line (itemId = data.shops[].items[].id)
 * curl -X PATCH http://localhost:8080/api/customer/cart/items/ITEM_UUID \
 *      -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" \
 *      -d '{"quantity":3}'
 *
 * # remove a line
 * curl -X DELETE http://localhost:8080/api/customer/cart/items/ITEM_UUID \
 *      -H "Authorization: Bearer TOKEN"
 * </pre>
 *
 * <p>Sample item in the response:
 * <pre>
 * {
 *   "id": "...", "productId": "...", "productSlug": "ao-thun-basic",
 *   "productName": "Áo thun basic", "variantId": "...", "variantName": "Màu: Đen, Size: L",
 *   "selectedOptions": [
 *     { "optionName": "Màu", "value": "Đen" },
 *     { "optionName": "Size", "value": "L" }
 *   ],
 *   "sku": "AT-DEN-L", "imageUrl": "https://...",
 *   "unitPrice": 250000.00, "comparePrice": 300000.00,
 *   "quantity": 3, "lineSubtotal": 750000.00,
 *   "availableStock": 12, "available": true
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        return ApiResponse.success(
                "Lấy giỏ hàng thành công.",
                cartService.getCart(authentication.getName())
        );
    }

    /** 201 when a new line was created, 200 when the quantity of an existing line was increased. */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestBody AddToCartRequest request,
            Authentication authentication
    ) {
        CartService.AddItemResult result =
                cartService.addItem(authentication.getName(), request);

        HttpStatus status = result.newItem() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.newItem()
                ? "Thêm sản phẩm vào giỏ hàng thành công."
                : "Cập nhật số lượng sản phẩm trong giỏ hàng thành công.";

        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, result.cart()));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable UUID itemId,
            @RequestBody UpdateCartItemRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật số lượng thành công.",
                cartService.updateItemQuantity(authentication.getName(), itemId, request)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Xóa sản phẩm khỏi giỏ hàng thành công.",
                cartService.removeItem(authentication.getName(), itemId)
        );
    }

    /**
     * Local handlers take precedence over the global advice for this controller only.
     * A missing or malformed JSON body returns 400 instead of the global 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Request body is missing or malformed."));
    }

    /**
     * Concurrent first-time cart creation or line insertion that slipped past the
     * customer lock is reported as a conflict, not as an internal error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The cart was updated concurrently. Please retry."));
    }
}
