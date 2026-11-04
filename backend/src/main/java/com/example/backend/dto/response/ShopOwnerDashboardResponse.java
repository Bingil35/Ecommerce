package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ShopOwnerDashboardResponse(
        ProductStatsResponse products,
        long totalOrders,
        long pendingOrders,
        long shippingOrders,
        long deliveredOrders,
        BigDecimal revenue,
        BigDecimal ratingAverage,
        long reviewCount,
        List<DailyPerformance> dailyPerformance
) {
    public record DailyPerformance(LocalDate date, long deliveredOrders, BigDecimal revenue) {}
}
