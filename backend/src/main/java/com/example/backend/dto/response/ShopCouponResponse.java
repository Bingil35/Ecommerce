package com.example.backend.dto.response;

import com.example.backend.entity.Coupon;
import com.example.backend.entity.CouponStatus;
import com.example.backend.entity.DiscountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShopCouponResponse(UUID id, String code, DiscountType discountType, Integer discountValue,
        BigDecimal minimumOrder, BigDecimal maximumDiscount, Integer usageLimit, Integer usedCount,
        OffsetDateTime startAt, OffsetDateTime endAt, CouponStatus status) {
    public static ShopCouponResponse from(Coupon c) {
        return new ShopCouponResponse(c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinimumOrder(), c.getMaximumDiscount(), c.getUsageLimit(), c.getUsedCount(),
                c.getStartAt(), c.getEndAt(), c.getStatus());
    }
}
