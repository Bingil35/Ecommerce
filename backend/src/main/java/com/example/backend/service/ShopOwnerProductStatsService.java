package com.example.backend.service;

import com.example.backend.dto.response.ShopOwnerProductStatsResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.OrderStatus;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.ShopOwnerProductStatsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopOwnerProductStatsService {
    private final ShopService shops;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ShopOwnerProductStatsRepository stats;

    public ShopOwnerProductStatsService(ShopService shops, ProductRepository products,
            ProductVariantRepository variants, ShopOwnerProductStatsRepository stats) {
        this.shops = shops; this.products = products; this.variants = variants; this.stats = stats;
    }

    @Transactional(readOnly = true)
    public ShopOwnerProductStatsResponse get(String email, String keyword, String sort, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Trang phải lớn hơn hoặc bằng 0.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Kích thước trang phải từ 1 đến 100.");
        String sortKey = sort == null || sort.isBlank() ? "revenueLifetime" : sort;
        if (!List.of("revenueLifetime", "revenue30Days", "soldLifetime", "sold30Days", "salesPace", "rating", "priceAsc", "priceDesc", "name").contains(sortKey))
            throw new IllegalArgumentException("Kiểu sắp xếp không hợp lệ.");
        var shop = shops.getMyShop(email);
        UUID shopId = shop.getId();
        var all = products.findByShopIdOrderByCreatedAtDesc(shopId);
        var variantsByProduct = new HashMap<UUID, BigDecimal>();
        for (var variant : variants.findAllByShopId(shopId)) {
            if (variant.getPrice() != null) variantsByProduct.merge(variant.getProduct().getId(), variant.getPrice(), BigDecimal::min);
        }
        var lifetime = aggregate(stats.lifetimeDelivered(shopId, OrderStatus.DELIVERED));
        var recent = aggregate(stats.deliveredSince(shopId, OrderStatus.DELIVERED, OffsetDateTime.now().minusDays(30)));
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Comparator<ShopOwnerProductStatsResponse.Item> comparator = comparator(sortKey);
        List<ShopOwnerProductStatsResponse.Item> filtered = all.stream()
                .filter(p -> query.isEmpty() || (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(query)))
                .map(p -> item(p, variantsByProduct.get(p.getId()), lifetime.get(p.getId()), recent.get(p.getId())))
                .sorted(comparator.thenComparing(ShopOwnerProductStatsResponse.Item::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        int total = filtered.size();
        int start = (int) Math.min((long) page * size, total);
        int end = Math.min(start + size, total);
        return new ShopOwnerProductStatsResponse(filtered.subList(start, end), page, size, total,
                total == 0 ? 0 : (total + size - 1) / size, sortKey, shop.getShopName());
    }

    private static Map<UUID, Totals> aggregate(List<Object[]> rows) {
        Map<UUID, Totals> result = new HashMap<>();
        for (Object[] row : rows) result.put((UUID) row[0], new Totals(((Number) row[1]).longValue(), (BigDecimal) row[2], ((Number) row[3]).longValue()));
        return result;
    }
    private static ShopOwnerProductStatsResponse.Item item(Product p, BigDecimal price, Totals lifetime, Totals recent) {
        return new ShopOwnerProductStatsResponse.Item(p.getId(), p.getName(), p.getThumbnailUrl(), p.getSlug(), price,
                lifetime == null ? 0 : lifetime.sold, recent == null ? 0 : recent.sold,
                lifetime == null ? BigDecimal.ZERO : lifetime.revenue, recent == null ? BigDecimal.ZERO : recent.revenue,
                p.getRatingAverage() == null ? BigDecimal.ZERO : p.getRatingAverage(), p.getRatingCount() == null ? 0 : p.getRatingCount(),
                lifetime == null || lifetime.orderCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(lifetime.sold).divide(BigDecimal.valueOf(lifetime.orderCount), 2, RoundingMode.HALF_UP));
    }
    private static Comparator<ShopOwnerProductStatsResponse.Item> comparator(String sort) {
        return switch (sort) {
            case "revenue30Days" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::revenue30Days, Comparator.nullsLast(Comparator.reverseOrder()));
            case "soldLifetime" -> Comparator.comparingLong(ShopOwnerProductStatsResponse.Item::soldLifetime).reversed();
            case "sold30Days" -> Comparator.comparingLong(ShopOwnerProductStatsResponse.Item::sold30Days).reversed();
            case "salesPace" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::salesPace).reversed();
            case "rating" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::ratingAverage).reversed();
            case "priceAsc" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::currentPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "priceDesc" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::currentPrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case "name" -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(ShopOwnerProductStatsResponse.Item::revenueLifetime, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
    private record Totals(long sold, BigDecimal revenue, long orderCount) {}
}
