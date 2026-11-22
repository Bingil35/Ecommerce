package com.example.backend.dto.response;

import com.example.backend.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShopOwnerDashboardResponse(
        LocalDate from,
        LocalDate to,
        long orderCount,
        long deliveredOrders,
        long pendingOrders,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        List<DailyPerformance> dailyPerformance,
        List<StatusCount> orderStatusBreakdown,
        List<RecentOrder> recentOrders,
        Attention attention
) {
    public record DailyPerformance(LocalDate date, long orderCount, BigDecimal revenue) {}
    public record StatusCount(OrderStatus status, long count) {}
    public record RecentOrder(UUID id, String orderCode, OrderStatus status, String customerName, OffsetDateTime createdAt) {}
    public record Attention(long lowStock, long outOfStock, long pendingProducts, long unansweredReviews) {}
}
