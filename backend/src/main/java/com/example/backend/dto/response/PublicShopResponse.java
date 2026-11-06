package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.backend.entity.Shop;

public record PublicShopResponse(UUID id, String shopName, String slug, String logoUrl,
        String bannerUrl, String address, BigDecimal ratingAverage, Integer ratingCount,
        Integer followersCount) {
    public static PublicShopResponse from(Shop shop) {
        return new PublicShopResponse(shop.getId(), shop.getShopName(), shop.getSlug(),
                shop.getLogoUrl(), shop.getBannerUrl(), shop.getAddress(), shop.getRatingAverage(),
                shop.getRatingCount(), shop.getFollowersCount());
    }
}
