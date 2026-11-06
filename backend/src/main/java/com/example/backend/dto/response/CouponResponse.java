package com.example.backend.dto.response;

import com.example.backend.entity.Coupon;
import com.example.backend.entity.CouponScope;
import com.example.backend.entity.DiscountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CouponResponse(UUID id, String code, CouponScope scope, UUID shopId, String shopName,
        DiscountType discountType, Integer discountValue, BigDecimal minimumOrder,
        BigDecimal maximumDiscount, Integer usageLimit, Integer usedCount,
        OffsetDateTime startAt, OffsetDateTime endAt) {
    public static CouponResponse from(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getScope(), c.getShop() == null ? null : c.getShop().getId(),
                c.getShop() == null ? null : c.getShop().getShopName(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinimumOrder(), c.getMaximumDiscount(), c.getUsageLimit(), c.getUsedCount(), c.getStartAt(), c.getEndAt());
    }
}
