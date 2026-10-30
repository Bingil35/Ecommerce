package com.example.backend.exception;

/**
 * Tài khoản đăng nhập đúng thông tin nhưng không có vai trò SHOP_OWNER
 * nên không được vào kênh người bán (trả về HTTP 403).
 */
public class ShopOwnerRequiredException extends RuntimeException {

    public ShopOwnerRequiredException(String message) {
        super(message);
    }
}
