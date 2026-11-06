package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewResponse(UUID addressId, String recipientName, String recipientPhone,
        String recipientCity, String ward, String addressDetail, List<ShopPreview> shops,
        BigDecimal subtotal, BigDecimal shopDiscount, BigDecimal platformDiscount,
        BigDecimal totalDiscount, BigDecimal totalAmount, List<String> appliedCoupons) {
    public record ShopPreview(UUID shopId, String shopName, int itemCount, BigDecimal subtotal,
            BigDecimal shopCouponDiscount, BigDecimal platformCouponDiscount, BigDecimal discount,
            BigDecimal totalAmount, List<ItemPreview> items) {}
    public record ItemPreview(UUID cartItemId, BigDecimal price, BigDecimal comparePrice, Integer quantity,
            BigDecimal subtotal, BigDecimal shopCouponDiscount, BigDecimal platformCouponDiscount,
            BigDecimal totalAmount) {}
}
