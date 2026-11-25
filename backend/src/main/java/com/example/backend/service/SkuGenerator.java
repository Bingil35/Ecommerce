package com.example.backend.service;

import java.util.function.Predicate;

final class SkuGenerator {
    private SkuGenerator() {}

    static String copyOf(String original, Predicate<String> exists) {
        String base = original == null ? "" : original.trim().replaceAll("[^A-Za-z0-9_-]", "-");
        if (base.isEmpty() || !Character.isLetterOrDigit(base.charAt(0))) base = "SKU-" + base;
        for (int index = 1; ; index++) {
            String suffix = index == 1 ? "-COPY" : "-COPY-" + index;
            String candidate = base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix;
            if (!exists.test(candidate)) return candidate;
        }
    }
}
