package com.example.backend.repository;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /**
     * Sản phẩm hiển thị cho khách hàng (không phải INACTIVE) mà MỖI từ khóa
     * đều xuất hiện trong tên, slug (không dấu) hoặc mô tả.
     */
    public static Specification<Product> searchByKeywords(
            List<String> tokens
    ) {
        return (root, query, cb) -> {

            // Tránh N+1 khi đọc category; bỏ qua ở câu lệnh count.
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.notEqual(
                            root.get("status"),
                            ProductStatus.INACTIVE
                    )
            );

            for (String token : tokens) {

                String pattern =
                        "%" + SearchKeyword.escapeLike(token) + "%";

                List<Predicate> anyField = new ArrayList<>();

                anyField.add(cb.like(
                        cb.lower(root.get("name")),
                        pattern,
                        ESCAPE
                ));

                anyField.add(cb.like(
                        cb.lower(root.get("description")),
                        pattern,
                        ESCAPE
                ));

                String ascii = SearchKeyword.toAscii(token);

                if (!ascii.isEmpty()) {
                    anyField.add(cb.like(
                            root.get("slug"),
                            "%" + SearchKeyword.escapeLike(ascii) + "%",
                            ESCAPE
                    ));
                }

                predicates.add(
                        cb.or(anyField.toArray(new Predicate[0]))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}