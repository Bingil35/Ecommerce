package com.example.backend.service;

import com.example.backend.dto.response.ProductStatsResponse;
import com.example.backend.dto.response.ShopOwnerDashboardResponse;
import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.Shop;
import com.example.backend.entity.SubOrder;
import com.example.backend.repository.ProductReviewRepository;
import com.example.backend.repository.SubOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopOwnerDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShopService shops;
    private final ProductManagementService products;
    private final SubOrderRepository orders;
    private final ProductReviewRepository reviews;

    public ShopOwnerDashboardService(ShopService shops, ProductManagementService products,
            SubOrderRepository orders, ProductReviewRepository reviews) {
        this.shops = shops;
        this.products = products;
        this.orders = orders;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public ShopOwnerDashboardResponse get(String email, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) throw new IllegalArgumentException("Start date must be before end date.");
        if (end.isAfter(today)) throw new IllegalArgumentException("End date cannot be in the future.");
        if (start.plusDays(6).isAfter(end)) throw new IllegalArgumentException("The selected period must be at least 7 days.");

        Shop shop = shops.getMyShop(email);
        OffsetDateTime fromTime = start.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = end.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();

        List<SubOrder> periodOrders = orders.findDashboardOrders(shop.getId(), fromTime, toExclusive);
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        Map<LocalDate, Long> orderCountsByDay = new java.util.HashMap<>();
        for (SubOrder order : periodOrders) {
            statusCounts.merge(order.getStatus(), 1L, Long::sum);
            LocalDate date = order.getCreatedAt().atZoneSameInstant(ZONE).toLocalDate();
            orderCountsByDay.merge(date, 1L, Long::sum);
        }

        List<SubOrder> deliveredInPeriod = orders.findByShop_IdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtAsc(
                shop.getId(), OrderStatus.DELIVERED, fromTime, toExclusive);
        Map<LocalDate, BigDecimal> revenueByDay = new java.util.HashMap<>();
        BigDecimal revenue = BigDecimal.ZERO;
        for (SubOrder order : deliveredInPeriod) {
            BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            LocalDate date = order.getUpdatedAt().atZoneSameInstant(ZONE).toLocalDate();
            revenueByDay.merge(date, amount, BigDecimal::add);
            revenue = revenue.add(amount);
        }

        List<ShopOwnerDashboardResponse.DailyPerformance> series = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            series.add(new ShopOwnerDashboardResponse.DailyPerformance(date,
                    orderCountsByDay.getOrDefault(date, 0L), revenueByDay.getOrDefault(date, BigDecimal.ZERO)));
        }
        List<ShopOwnerDashboardResponse.StatusCount> breakdown = List.of(OrderStatus.values()).stream()
                .map(status -> new ShopOwnerDashboardResponse.StatusCount(status, statusCounts.getOrDefault(status, 0L)))
                .toList();
        ProductStatsResponse productStats = products.stats(shop.getId());
        BigDecimal average = deliveredInPeriod.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(deliveredInPeriod.size()), 0, RoundingMode.HALF_UP);
        var recent = orders.findRecentDashboardOrders(shop.getId(), PageRequest.of(0, 5)).stream()
                .map(order -> new ShopOwnerDashboardResponse.RecentOrder(order.getId(), order.getOrder().getOrderCode(),
                        order.getStatus(), order.getOrder().getUser().getFullName(), order.getCreatedAt()))
                .toList();

        return new ShopOwnerDashboardResponse(start, end, periodOrders.size(), deliveredInPeriod.size(),
                statusCounts.getOrDefault(OrderStatus.PENDING, 0L), revenue, average, series, breakdown, recent,
                new ShopOwnerDashboardResponse.Attention(productStats.lowStock(), productStats.outOfStock(),
                        productStats.pending(), reviews.countUnansweredByShopId(shop.getId())));
    }
}
