package com.example.backend.dto.request;

import com.example.backend.entity.CouponStatus;
import com.example.backend.entity.DiscountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ShopCouponRequest(String code, DiscountType discountType, Integer discountValue,
        BigDecimal minimumOrder, BigDecimal maximumDiscount, Integer usageLimit,
        OffsetDateTime startAt, OffsetDateTime endAt, CouponStatus status) {}
