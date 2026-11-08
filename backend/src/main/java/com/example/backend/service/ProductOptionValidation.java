package com.example.backend.service;

import com.example.backend.dto.request.SubmitProductRequest;
import java.text.Normalizer;
import java.util.*;

/** Shared normalization for option names and values; accents remain significant. */
public final class ProductOptionValidation {
    private ProductOptionValidation() {}
    public static String clean(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .replaceAll("[\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+", " ").replaceAll("^ +| +$", "");
    }
    public static String key(String value) {
        return Normalizer.normalize(clean(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }
    public static String required(String value, String label) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) throw new IllegalArgumentException(label + " không được để trống.");
        return cleaned;
    }
    public static void unique(String value, Collection<String> others, String label) {
        required(value, label);
        if (others.stream().anyMatch(other -> key(other).equals(key(value))))
            throw new IllegalArgumentException(label + " '" + clean(value) + "' đã tồn tại.");
    }
    public static void validateOptions(List<SubmitProductRequest.OptionInput> options) {
        if (options == null) return;
        Set<String> names = new HashSet<>();
        for (var option : options) {
            if (option == null) throw new IllegalArgumentException("Thuộc tính không được để trống.");
            String name = required(option.getName(), "Tên thuộc tính");
            unique(name, names, "Thuộc tính trong sản phẩm");
            names.add(key(name));
            if (option.getValues() == null || option.getValues().isEmpty())
                throw new IllegalArgumentException("Thuộc tính '" + name + "' cần ít nhất một giá trị.");
            Set<String> values = new HashSet<>();
            for (String value : option.getValues()) {
                unique(value, values, "Giá trị trong thuộc tính '" + name + "'");
                values.add(key(value));
            }
        }
    }
}
