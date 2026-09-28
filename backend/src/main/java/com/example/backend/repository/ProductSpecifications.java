package com.example.backend.repository;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private static final int MAX_LENGTH = 100;
    private static final int MAX_TOKENS = 5;

    private ProductSpecifications() {
    }

    // =========================================================
    // SEARCH PRODUCT
    // =========================================================

    /**
     * Sản phẩm hiển thị cho khách hàng (không phải INACTIVE)
     * mà MỖI từ khóa đều xuất hiện trong tên, slug (không dấu)
     * hoặc mô tả.
     */
    public static Specification<Product> searchByKeywords(
            List<String> tokens
    ) {
        return (root, query, cb) -> {

            // Tránh N+1 khi đọc category;
            // bỏ qua ở câu lệnh count.
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {

                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Không hiển thị sản phẩm INACTIVE
            predicates.add(
                    cb.notEqual(
                            root.get("status"),
                            ProductStatus.INACTIVE
                    )
            );

            // Mỗi token phải xuất hiện ở ít nhất một field
            for (String token : tokens) {

                String pattern =
                        "%" + escapeLike(token) + "%";

                List<Predicate> anyField = new ArrayList<>();

                // Tìm trong tên
                anyField.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                pattern,
                                ESCAPE
                        )
                );

                // Tìm trong mô tả
                anyField.add(
                        cb.like(
                                cb.lower(root.get("description")),
                                pattern,
                                ESCAPE
                        )
                );

                // Tìm trong slug không dấu
                String ascii = toAscii(token);

                if (!ascii.isEmpty()) {
                    anyField.add(
                            cb.like(
                                    root.get("slug"),
                                    "%" + escapeLike(ascii) + "%",
                                    ESCAPE
                            )
                    );
                }

                predicates.add(
                        cb.or(
                                anyField.toArray(new Predicate[0])
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    // =========================================================
    // KEYWORD
    // =========================================================

    /**
     * Tách từ khóa thành tối đa MAX_TOKENS từ.
     *
     * <p>
     * - Chuyển thành chữ thường
     * - Loại bỏ từ trùng
     * - Giới hạn số từ
     * </p>
     *
     * @throws IllegalArgumentException nếu từ khóa rỗng
     *                                  hoặc quá dài
     */
    public static List<String> tokenize(String raw) {

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "Keyword is required"
            );
        }

        String trimmed = raw.trim();

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Keyword must not exceed "
                            + MAX_LENGTH
                            + " characters"
            );
        }

        return Arrays.stream(
                        trimmed
                                .toLowerCase(Locale.ROOT)
                                .split("\\s+")
                )
                .distinct()
                .limit(MAX_TOKENS)
                .toList();
    }

    /**
     * Bỏ dấu để so khớp với cột slug.
     *
     * <p>
     * Ví dụ:
     * "điện thoại" -> "dien-thoai"
     * </p>
     */
    public static String toAscii(String token) {

        String withoutAccents = Normalizer
                .normalize(
                        token,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                )
                .replace('đ', 'd');

        return withoutAccents
                .replaceAll("[^a-z0-9-]+", "")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    /**
     * Escape %, _ và \ để người dùng
     * không thể tự chèn wildcard vào LIKE.
     */
    public static String escapeLike(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}