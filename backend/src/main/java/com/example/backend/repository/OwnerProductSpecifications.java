package com.example.backend.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.backend.entity.Inventory;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.StockStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Bộ lọc/sắp xếp cho trang quản lý sản phẩm của shop owner.
 * Luôn giới hạn theo shopId; không lọc theo trạng thái shop hay sản phẩm
 * (owner thấy cả PENDING, REJECTED, ACTIVE, INACTIVE).
 *
 * Định nghĩa tồn kho (thống nhất với InventoryService và ProductSummary):
 *   tồn khả dụng của một biến thể = max(0, quantity - reservedQuantity)
 *   tồn khả dụng của sản phẩm     = tổng tồn khả dụng các biến thể ACTIVE
 *   ngưỡng cảnh báo của biến thể  = low_stock_threshold nếu > 0, ngược lại
 *                                   ngưỡng mặc định (app.inventory.low-stock-threshold)
 */
public final class OwnerProductSpecifications {

    private static final char ESCAPE = '\\';
    private static final int MAX_KEYWORD_LENGTH = 100;

    private OwnerProductSpecifications() {
    }

    /**
     * @param createdFrom        bao gồm
     * @param createdToExclusive không bao gồm
     * @param sortKey            price_asc, price_desc, stock_asc, stock_desc sẽ được áp dụng
     *                           ở đây; các khóa khác sắp xếp qua Pageable.
     */
    public static Specification<Product> of(
            UUID shopId,
            String keyword,
            UUID categoryId,
            ProductStatus status,
            StockStatus stockStatus,
            int lowStockThreshold,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            OffsetDateTime createdFrom,
            OffsetDateTime createdToExclusive,
            String sortKey
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("shopId"), shopId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            String kw = normalizeKeyword(keyword);
            if (kw != null) {
                String pattern = "%" + escapeLike(kw) + "%";

                Subquery<Integer> skuMatch = query.subquery(Integer.class);
                Root<ProductVariant> skuVariant = skuMatch.from(ProductVariant.class);
                skuMatch.select(cb.literal(1))
                        .where(cb.equal(skuVariant.get("product").get("id"), root.get("id")),
                                cb.like(cb.lower(skuVariant.get("sku")), pattern, ESCAPE));

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, ESCAPE),
                        cb.exists(skuMatch)
                ));
            }

            // Khoảng giá: phải tồn tại MỘT biến thể ACTIVE có giá nằm trong
            // [minPrice, maxPrice]. Cả hai cận được áp dụng lên CÙNG một biến thể.
            if (minPrice != null || maxPrice != null) {
                Subquery<Integer> priced = query.subquery(Integer.class);
                Root<ProductVariant> v = priced.from(ProductVariant.class);

                List<Predicate> conditions = new ArrayList<>();
                conditions.add(cb.equal(v.get("product").get("id"), root.get("id")));
                conditions.add(cb.equal(v.get("status"), ProductStatus.ACTIVE));
                conditions.add(cb.isNotNull(v.get("price")));

                if (minPrice != null) {
                    conditions.add(cb.greaterThanOrEqualTo(v.get("price"), minPrice));
                }

                if (maxPrice != null) {
                    conditions.add(cb.lessThanOrEqualTo(v.get("price"), maxPrice));
                }

                priced.select(cb.literal(1))
                        .where(conditions.toArray(new Predicate[0]));

                predicates.add(cb.exists(priced));
            }

            if (stockStatus != null) {
                switch (stockStatus) {
                    case IN_STOCK -> predicates.add(
                            cb.greaterThan(totalAvailable(query, cb, root), 0L));
                    case OUT_OF_STOCK -> predicates.add(
                            cb.equal(totalAvailable(query, cb, root), 0L));
                    case LOW_STOCK -> predicates.add(
                            cb.exists(lowStockVariant(query, cb, root, lowStockThreshold)));
                }
            }

            if (createdFrom != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.<OffsetDateTime>get("createdAt"), createdFrom));
            }

            if (createdToExclusive != null) {
                predicates.add(
                        cb.lessThan(
                                root.<OffsetDateTime>get("createdAt"), createdToExclusive));
            }

            // Sắp xếp theo giá/tồn kho cần subquery; bỏ qua ở câu lệnh count.
            if (query.getResultType() != Long.class
                    && query.getResultType() != long.class
                    && sortKey != null) {
                applyAggregateSort(query, cb, root, sortKey);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================================================
    // SUBQUERIES
    // =========================================================

    /** Tồn khả dụng của một dòng Inventory: quantity - reserved (null coi là 0). */
    private static Expression<Integer> available(
            CriteriaBuilder cb,
            Root<Inventory> inventory
    ) {
        return cb.diff(
                cb.coalesce(inventory.<Integer>get("quantity"), 0),
                cb.coalesce(inventory.<Integer>get("reservedQuantity"), 0)
        );
    }

    /** Tổng tồn khả dụng (mỗi biến thể ACTIVE, không âm); 0 nếu chưa có biến thể. */
    private static Subquery<Long> totalAvailable(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<Product> root
    ) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Inventory> inventory = sub.from(Inventory.class);
        Join<Inventory, ProductVariant> variant = inventory.join("variant");

        Expression<Integer> available = available(cb, inventory);

        Expression<Integer> nonNegative = cb.<Integer>selectCase()
                .when(cb.greaterThan(available, 0), available)
                .otherwise(0);

        sub.select(cb.coalesce(cb.sumAsLong(nonNegative), 0L))
                .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                        cb.equal(variant.get("status"), ProductStatus.ACTIVE));
        return sub;
    }

    /**
     * Sản phẩm "sắp hết": có ít nhất một biến thể ACTIVE còn hàng (khả dụng > 0) nhưng
     * không vượt ngưỡng cảnh báo của chính biến thể đó.
     */
    private static Subquery<Integer> lowStockVariant(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<Product> root,
            int defaultThreshold
    ) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<Inventory> inventory = sub.from(Inventory.class);
        Join<Inventory, ProductVariant> variant = inventory.join("variant");

        Expression<Integer> available = available(cb, inventory);

        Expression<Integer> ownThreshold =
                cb.coalesce(inventory.<Integer>get("lowStockThreshold"), 0);

        // low_stock_threshold có DEFAULT 0 trong DB: 0 nghĩa là "chưa đặt" -> dùng mặc định.
        Expression<Integer> effective = cb.<Integer>selectCase()
                .when(cb.greaterThan(ownThreshold, 0), ownThreshold)
                .otherwise(defaultThreshold);

        sub.select(cb.literal(1))
                .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                        cb.equal(variant.get("status"), ProductStatus.ACTIVE),
                        cb.greaterThan(available, 0),
                        cb.lessThanOrEqualTo(available, effective));
        return sub;
    }

    private static void applyAggregateSort(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<Product> root,
            String sortKey
    ) {
        switch (sortKey) {
            case "stock_asc" -> query.orderBy(
                    cb.asc(totalAvailable(query, cb, root)),
                    cb.asc(root.get("id")));
            case "stock_desc" -> query.orderBy(
                    cb.desc(totalAvailable(query, cb, root)),
                    cb.asc(root.get("id")));
            case "price_asc", "price_desc" -> {
                Subquery<BigDecimal> minPrice = query.subquery(BigDecimal.class);
                Root<ProductVariant> v = minPrice.from(ProductVariant.class);
                minPrice.select(cb.min(v.get("price")))
                        .where(cb.equal(v.get("product").get("id"), root.get("id")),
                                cb.equal(v.get("status"), ProductStatus.ACTIVE),
                                cb.isNotNull(v.get("price")));

                if ("price_asc".equals(sortKey)) {
                    // Sản phẩm chưa có giá (NULL) nằm cuối khi tăng dần.
                    query.orderBy(cb.asc(minPrice), cb.asc(root.get("id")));
                } else {
                    // coalesce để sản phẩm chưa có giá nằm cuối khi giảm dần.
                    query.orderBy(
                            cb.desc(cb.coalesce(minPrice, BigDecimal.ZERO)),
                            cb.asc(root.get("id")));
                }
            }
            default -> {
                // newest, oldest, name_*: sắp xếp qua Pageable.
            }
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String k = keyword.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) {
            return null;
        }
        return k.length() > MAX_KEYWORD_LENGTH
                ? k.substring(0, MAX_KEYWORD_LENGTH)
                : k;
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
