package com.example.backend.dto.response;

/**
 * Kết quả khách hàng đăng ký mở shop.
 * Trả kèm token mới và thông tin user (đã có thêm vai trò SHOP_OWNER) để frontend cập nhật phiên đăng nhập.
 */
public class ShopRegistrationResponse {
    private ShopResponse shop;
    private String accessToken;
    private String tokenType;
    private UserResponse user;

    public ShopRegistrationResponse() {
    }

    public ShopRegistrationResponse(
            ShopResponse shop,
            String accessToken,
            String tokenType,
            UserResponse user
    ) {
        this.shop = shop;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.user = user;
    }

    public ShopResponse getShop() { return shop; }
    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public UserResponse getUser() { return user; }
}
