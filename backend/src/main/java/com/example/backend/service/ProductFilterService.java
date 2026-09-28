package com.example.backend.service;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.repository.ProductFilterRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                resolveSort(sort)
        );

        Specification<Product> spec = buildSpec(
                tokens, category, brand, min, max,
                productStatus, minRating
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
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            BigDecimal minRating
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
            predicates.add(
                    cb.notEqual(root.get("status"), ProductStatus.INACTIVE)
            );

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }

            if (minPrice != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("price"), minPrice)
                );
            }

            if (maxPrice != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice)
                );
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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

    private Sort resolveSort(String sort) {

        String key = (sort == null || sort.isBlank())
                ? "newest"
                : sort.trim().toLowerCase(Locale.ROOT);

        Sort primary = switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new IllegalArgumentException(
                    "Invalid sort. Allowed: newest, price_asc, "
                            + "price_desc, name_asc");
        };

        // Thứ tự phụ để phân trang ổn định.
        return primary.and(Sort.by(Sort.Direction.ASC, "id"));
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

            // Khách hàng không được xem sản phẩm đã ngừng bán.
            if (status == ProductStatus.INACTIVE) {
                throw new IllegalArgumentException(
                        "status must be ACTIVE or OUT_OF_STOCK"
                );
            }

            return status;

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "status must be ACTIVE or OUT_OF_STOCK"
            );
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