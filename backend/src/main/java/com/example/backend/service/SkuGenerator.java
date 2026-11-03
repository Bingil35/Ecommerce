package com.example.backend.service;

import java.util.function.Predicate;

/** Sinh SKU cho bản sao sản phẩm, bảo đảm không trùng và không vượt 255 ký tự. */
public final class SkuGenerator {

    private static final int MAX_LENGTH = 255;
    private static final String COPY_SUFFIX = "-COPY";
    private static final int MAX_ATTEMPTS = 10_000;

    private SkuGenerator() {
    }

    /**
     * @param taken kiểm tra SKU đã được dùng (trong bộ nhớ và trong DB)
     * @return SKU dạng "<gốc>-COPY", rồi "<gốc>-COPY-2", "-COPY-3"... cho tới khi chưa bị dùng
     */
    public static String copyOf(String sku, Predicate<String> taken) {

        String base = sku == null || sku.isBlank() ? "SKU" : sku.trim();

        String candidate = fit(base, COPY_SUFFIX);

        for (int attempt = 2; taken.test(candidate); attempt++) {

            if (attempt > MAX_ATTEMPTS) {
                throw new IllegalStateException("Không thể tạo SKU duy nhất cho bản sao.");
            }

            candidate = fit(base, COPY_SUFFIX + "-" + attempt);
        }

        return candidate;
    }

    private static String fit(String base, String suffix) {

        int max = MAX_LENGTH - suffix.length();

        return (base.length() > max ? base.substring(0, max) : base) + suffix;
    }
}
