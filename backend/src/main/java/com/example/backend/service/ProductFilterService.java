package com.example.backend.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.repository.ProductFilterRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class ProductFilterService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_KEYWORD_TOKENS = 5;
    private static final char LIKE_ESCAPE = '\\';

    private final ProductFilterRepository productFilterRepository;

    public ProductFilterService(
            ProductFilterRepository productFilterRepository
    ) {
        this.productFilterRepository = productFilterRepository;
    }

    /**
     * Mọi tham số đều là chuỗi và đều tùy chọn; chuỗi rỗng = không lọc.
     * Tham số sai định dạng sẽ ném IllegalArgumentException (trả về 400).
     */
    @Transactional(readOnly = true)
    public Page<Product> filterProducts(
            String keyword,
            String categoryId,
            String brandId,
            String shopAddress,
            String minPrice,
            String maxPrice,
            String status,
            String rating,
            String page,
            String size,
            String sort
    ) {
        List<String> tokens = tokenizeKeyword(keyword);
        UUID category = parseUuid("categoryId", categoryId);
        UUID brand = parseUuid("brandId", brandId);
        String normalizedShopAddress = normalizeTextFilter(shopAddress);
        BigDecimal min = parsePrice("minPrice", minPrice);
        BigDecimal max = parsePrice("maxPrice", maxPrice);
        ProductStatus productStatus = parseStatus(status);
        BigDecimal minRating = parseRating(rating);

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "minPrice must not be greater than maxPrice"
            );
        }

        int pageNumber = Math.max(parseInt("page", page, 0), 0);
        int pageSize = parseInt("size", size, DEFAULT_PAGE_SIZE);
        pageSize = pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);

        String sortKey = normalizeSortKey(sort);
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                isPriceSort(sortKey) ? Sort.unsorted() : resolveSort(sortKey)
        );

        Specification<Product> spec = buildSpec(
                tokens, category, brand, normalizedShopAddress, min, max,
                productStatus, minRating, sortKey
        );

        return productFilterRepository.findAll(spec, pageable);
    }

    // =========================
    // BUILD SPECIFICATION
    // =========================

    private Specification<Product> buildSpec(
            List<String> tokens,
            UUID categoryId,
            UUID brandId,
            String shopAddress,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            BigDecimal minRating,
            String sortKey
    ) {
        return (root, query, cb) -> {

            // Tránh N+1 khi đọc category; bỏ qua ở câu lệnh count.
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Khách hàng không bao giờ thấy sản phẩm INACTIVE.
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

            Subquery<UUID> activeShop = query.subquery(UUID.class);
            Root<Shop> shopRoot = activeShop.from(Shop.class);
            activeShop.select(shopRoot.get("id"))
                    .where(cb.equal(shopRoot.get("id"), root.get("shopId")),
                            cb.equal(shopRoot.get("status"), ShopStatus.ACTIVE));
            predicates.add(cb.exists(activeShop));

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }

            // Lọc theo địa chỉ shop; địa chỉ thuộc Shops, không phải Products.
            if (shopAddress != null) {
                Subquery<UUID> matchingShop = query.subquery(UUID.class);
                Root<Shop> addressShop = matchingShop.from(Shop.class);
                matchingShop.select(addressShop.get("id"))
                        .where(cb.equal(addressShop.get("id"), root.get("shopId")),
                                cb.equal(addressShop.get("status"), ShopStatus.ACTIVE),
                                cb.like(cb.lower(addressShop.get("address")),
                                        "%" + escapeLike(shopAddress.toLowerCase(Locale.ROOT)) + "%", LIKE_ESCAPE));
                predicates.add(cb.exists(matchingShop));
            }

            if (minPrice != null) {
                // A product matches the lower bound if at least one active variant
                // reaches that price. Price is stored on ProductVariant, not Product.
                Subquery<Integer> hasVariantAtOrAboveMin = query.subquery(Integer.class);
                Root<ProductVariant> variant = hasVariantAtOrAboveMin.from(ProductVariant.class);
                hasVariantAtOrAboveMin.select(cb.literal(1))
                        .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                                cb.equal(variant.get("status"), ProductStatus.ACTIVE),
                                cb.isNotNull(variant.get("price")),
                                cb.greaterThanOrEqualTo(variant.get("price"), minPrice));
                predicates.add(cb.exists(hasVariantAtOrAboveMin));
            }

            if (maxPrice != null) {
                // A product matches the upper bound if at least one active variant
                // is priced at or below it. Together, both bounds mean the variant
                // price range overlaps the requested range, matching /api/products.
                Subquery<Integer> hasVariantAtOrBelowMax = query.subquery(Integer.class);
                Root<ProductVariant> variant = hasVariantAtOrBelowMax.from(ProductVariant.class);
                hasVariantAtOrBelowMax.select(cb.literal(1))
                        .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                                cb.equal(variant.get("status"), ProductStatus.ACTIVE),
                                cb.isNotNull(variant.get("price")),
                                cb.lessThanOrEqualTo(variant.get("price"), maxPrice));
                predicates.add(cb.exists(hasVariantAtOrBelowMax));
            }

            if (status != null && status != ProductStatus.ACTIVE) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                if ("price_asc".equals(sortKey) || "price_desc".equals(sortKey)) {
                    Subquery<BigDecimal> minVariantPrice = query.subquery(BigDecimal.class);
                    Root<ProductVariant> variant = minVariantPrice.from(ProductVariant.class);
                    minVariantPrice.select(cb.min(variant.get("price")))
                            .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                                    cb.equal(variant.get("status"), ProductStatus.ACTIVE),
                                    cb.isNotNull(variant.get("price")));
                    query.orderBy("price_asc".equals(sortKey)
                            ? cb.asc(minVariantPrice) : cb.desc(minVariantPrice), cb.asc(root.get("id")));
                }
            }

            // rating = điểm trung bình tối thiểu (ví dụ rating=4 -> từ 4.0 trở lên)
            if (minRating != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("ratingAverage"), minRating)
                );
            }

            // Từ khóa (tùy chọn): mỗi từ phải có trong tên, mô tả hoặc slug.
            for (String token : tokens) {

                String pattern = "%" + escapeLike(token) + "%";

                List<Predicate> anyField = new ArrayList<>();

                anyField.add(cb.like(
                        cb.lower(root.get("name")), pattern, LIKE_ESCAPE));
                anyField.add(cb.like(
                        cb.lower(root.get("description")), pattern, LIKE_ESCAPE));

                String ascii = toAscii(token);

                if (!ascii.isEmpty()) {
                    anyField.add(cb.like(
                            root.get("slug"),
                            "%" + escapeLike(ascii) + "%",
                            LIKE_ESCAPE));
                }

                predicates.add(cb.or(anyField.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // =========================
    // SORT
    // =========================

    private String normalizeSortKey(String sort) {
        String key = (sort == null || sort.isBlank()) ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        if (!List.of("newest", "price_asc", "price_desc", "name_asc").contains(key)) {
            throw new IllegalArgumentException("Invalid sort. Allowed: newest, price_asc, price_desc, name_asc");
        }
        return key;
    }

    private boolean isPriceSort(String sort) {
        return "price_asc".equals(sort) || "price_desc".equals(sort);
    }

    private Sort resolveSort(String key) {
        Sort primary = "name_asc".equals(key)
                ? Sort.by(Sort.Direction.ASC, "name")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return primary.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private String normalizeTextFilter(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 150) throw new IllegalArgumentException("shopAddress must be at most 150 characters");
        return normalized;
    }

    // =========================
    // PARSE PARAMETERS
    // =========================

    private UUID parseUuid(String name, String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    name + " must be a valid UUID"
            );
        }
    }

    private BigDecimal parsePrice(String name, String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        BigDecimal price;

        try {
            price = new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a number"
            );
        }

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }

        return price;
    }

    private ProductStatus parseStatus(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            ProductStatus status = ProductStatus.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );

            // Public catalog only exposes active products. The status parameter is retained
            // for API compatibility, but INACTIVE is intentionally rejected.
            if (status != ProductStatus.ACTIVE) {
                throw new IllegalArgumentException("status must be ACTIVE");
            }
            return status;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("status must be ACTIVE");
        }
    }

    private BigDecimal parseRating(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        BigDecimal rating;

        try {
            rating = new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "rating must be a number"
            );
        }

        if (rating.signum() < 0
                || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException(
                    "rating must be between 0 and 5"
            );
        }

        return rating;
    }

    private int parseInt(String name, String value, int defaultValue) {

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be an integer"
            );
        }
    }

    private List<String> tokenizeKeyword(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String trimmed = raw.trim();

        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Keyword must not exceed "
                            + MAX_KEYWORD_LENGTH + " characters");
        }

        return Arrays.stream(
                        trimmed.toLowerCase(Locale.ROOT).split("\\s+"))
                .distinct()
                .limit(MAX_KEYWORD_TOKENS)
                .toList();
    }

    // Bỏ dấu để khớp với cột slug; trả về "" nếu không còn ký tự hợp lệ.
    private String toAscii(String token) {
        return Normalizer.normalize(token, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9-]+", "")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    // Escape %, _ và \ để người dùng không tự chèn wildcard.
    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}