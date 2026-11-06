package com.example.backend.service;

import com.example.backend.dto.response.ShopOwnerDashboardResponse;
import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.Shop;
import com.example.backend.repository.ProductReviewRepository;
import com.example.backend.repository.SubOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopOwnerDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShopService shops;
    private final ProductManagementService products;
    private final SubOrderRepository orders;
    private final ProductReviewRepository reviews;

    public ShopOwnerDashboardService(ShopService shops, ProductManagementService products, SubOrderRepository orders, ProductReviewRepository reviews) {
        this.shops = shops; this.products = products; this.orders = orders; this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public ShopOwnerDashboardResponse get(String email, int days) {
        if (days != 7 && days != 30) throw new IllegalArgumentException("Dashboard interval must be 7 or 30 days.");
        Shop shop = shops.getMyShop(email);
        var productStats = products.stats(shop.getId());
        var allReviews = reviews.findByShopId(shop.getId());
        BigDecimal rating = allReviews.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(allReviews.stream().mapToInt(r -> r.getRating()).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now(ZONE);
        OffsetDateTime from = today.minusDays(days - 1L).atStartOfDay(ZONE).toOffsetDateTime();
        record DayStats(long count, BigDecimal revenue) {}
        Map<LocalDate, DayStats> daily = orders.findByShop_IdAndStatusAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(shop.getId(), OrderStatus.DELIVERED, from).stream()
                .collect(Collectors.groupingBy(s -> s.getUpdatedAt().atZoneSameInstant(ZONE).toLocalDate(),
                        Collectors.reducing(new DayStats(0, BigDecimal.ZERO), s -> new DayStats(1, s.getTotalAmount() == null ? BigDecimal.ZERO : s.getTotalAmount()),
                                (a, b) -> new DayStats(a.count() + b.count(), a.revenue().add(b.revenue())))));
        var series = new ArrayList<ShopOwnerDashboardResponse.DailyPerformance>();
        for (int day = days - 1; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            DayStats dayStats = daily.getOrDefault(date, new DayStats(0, BigDecimal.ZERO));
            series.add(new ShopOwnerDashboardResponse.DailyPerformance(date, dayStats.count(), dayStats.revenue()));
        }
        BigDecimal revenue = orders.sumTotalByShopIdAndStatus(shop.getId(), OrderStatus.DELIVERED);
        return new ShopOwnerDashboardResponse(productStats,
                orders.countByShop_Id(shop.getId()),
                orders.countByShop_IdAndStatus(shop.getId(), OrderStatus.PENDING),
                orders.countByShop_IdAndStatus(shop.getId(), OrderStatus.SHIPPING),
                orders.countByShop_IdAndStatus(shop.getId(), OrderStatus.DELIVERED),
                revenue == null ? BigDecimal.ZERO : revenue, rating, allReviews.size(), series);
    }
}
