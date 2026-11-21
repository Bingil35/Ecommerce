package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShopOwnerProductStatsResponse(
        List<Item> items, int page, int size, long totalElements, int totalPages,
        String sort, String shopName
) {
    public record Item(UUID id, String name, String thumbnailUrl, String slug,
            BigDecimal currentPrice, long soldLifetime, long sold30Days,
            BigDecimal revenueLifetime, BigDecimal revenue30Days,
            BigDecimal ratingAverage, int ratingCount, BigDecimal salesPace) {}
}
