package com.example.backend.service;

import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.SubOrderRepository;
import com.example.backend.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AdminDashboardService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Bangkok");
    private final OrderRepository orders;
    private final UserRoleRepository userRoles;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final SubOrderRepository subOrders;

    public AdminDashboardService(OrderRepository orders, UserRoleRepository userRoles, ShopRepository shops, ProductRepository products, SubOrderRepository subOrders) {
        this.orders = orders;
        this.userRoles = userRoles;
        this.shops = shops;
        this.products = products;
        this.subOrders = subOrders;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(APP_ZONE);
        if (from == null || to == null) {
            to = today;
            from = to.minusDays(29);
        }
        if (to.isBefore(from)) throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        long dayCount = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (dayCount > 3660) throw new IllegalArgumentException("Khoảng thời gian tối đa là 10 năm.");

        OffsetDateTime start = from.atStartOfDay(APP_ZONE).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
        LocalDate previousFromDate = from.minusDays(dayCount);
        OffsetDateTime previousStart = previousFromDate.atStartOfDay(APP_ZONE).toOffsetDateTime();
        OffsetDateTime snapshotCurrent = end;
        OffsetDateTime snapshotPrevious = start;

        long orderCount = orders.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long previousOrderCount = orders.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(previousStart, start);
        BigDecimal revenue = zero(orders.sumTotalAmountBetween(start, end));
        BigDecimal previousRevenue = zero(orders.sumTotalAmountBetween(previousStart, start));
        long customers = userRoles.countCustomersCreatedBefore(snapshotCurrent);
        long previousCustomers = userRoles.countCustomersCreatedBefore(snapshotPrevious);
        long storeCount = shops.countByCreatedAtBefore(snapshotCurrent);
        long previousStoreCount = shops.countByCreatedAtBefore(snapshotPrevious);
        long productCount = products.countByCreatedAtBefore(snapshotCurrent);
        long previousProductCount = products.countByCreatedAtBefore(snapshotPrevious);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("bucket", dayCount <= 90 ? "day" : "month");
        result.put("kpis", Map.of(
                "revenue", kpi(revenue, percentChange(revenue, previousRevenue)),
                "orders", kpi(orderCount, percentChange(orderCount, previousOrderCount)),
                "customers", kpi(customers, percentChange(customers, previousCustomers)),
                "shops", kpi(storeCount, percentChange(storeCount, previousStoreCount)),
                "products", kpi(productCount, percentChange(productCount, previousProductCount))
        ));
        List<Object[]> rows = dayCount <= 90 ? orders.revenueByDay(start, end) : orders.revenueByMonth(start, end);
        List<Map<String, Object>> revenueSeries = new ArrayList<>();
        for (Object[] row : rows) revenueSeries.add(Map.of("period", String.valueOf(row[0]), "value", zero((BigDecimal) row[1])));
        result.put("revenueSeries", revenueSeries);
        List<Map<String, Object>> orderStatusBreakdown = new ArrayList<>();
        for (Object[] row : subOrders.countByOrderStatusForOrdersCreatedBetween(start, end)) {
            orderStatusBreakdown.add(Map.of("status", String.valueOf(row[0]), "count", ((Number) row[1]).longValue()));
        }
        result.put("orderStatusBreakdown", orderStatusBreakdown);
        List<Map<String, Object>> paymentMethodBreakdown = new ArrayList<>();
        for (Object[] row : orders.countByPaymentMethodForOrdersCreatedBetween(start, end)) {
            paymentMethodBreakdown.add(Map.of("method", String.valueOf(row[0]), "count", ((Number) row[1]).longValue()));
        }
        result.put("paymentMethodBreakdown", paymentMethodBreakdown);
        result.put("pendingShops", shops.findByStatusOrderByShopNameAsc(com.example.backend.entity.ShopStatus.PENDING).size());
        return result;
    }

    private Map<String, Object> kpi(Object value, BigDecimal change) {
        return Map.of("value", value, "changePercent", change);
    }
    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return current.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous.abs(), 1, java.math.RoundingMode.HALF_UP);
    }
    private BigDecimal percentChange(long current, long previous) {
        return percentChange(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
