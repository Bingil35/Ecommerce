package com.example.backend.repository;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Chuẩn hóa từ khóa tìm kiếm: tách từ, chuyển chữ thường,
 * bỏ dấu tiếng Việt và escape ký tự đặc biệt của LIKE.
 */
public final class SearchKeyword {

    public static final int MAX_LENGTH = 100;
    public static final int MAX_TOKENS = 5;

    private SearchKeyword() {
    }

    /**
     * Tách từ khóa thành tối đa MAX_TOKENS từ (chữ thường, không trùng).
     *
     * @throws IllegalArgumentException nếu từ khóa rỗng hoặc quá dài
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
                            + MAX_LENGTH + " characters"
            );
        }

        return Arrays.stream(
                        trimmed.toLowerCase(Locale.ROOT)
                                .split("\\s+")
                )
                .distinct()
                .limit(MAX_TOKENS)
                .toList();
    }

    /**
     * Bỏ dấu để so khớp với cột slug (giống cách ProductService tạo slug).
     * Trả về chuỗi rỗng nếu không còn ký tự hợp lệ.
     */
    public static String toAscii(String token) {

        String withoutAccents = Normalizer
                .normalize(token, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd');

        return withoutAccents
                .replaceAll("[^a-z0-9-]+", "")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    /**
     * Escape %, _ và \ để người dùng không tự chèn wildcard.
     */
    public static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}