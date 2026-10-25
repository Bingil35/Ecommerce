package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopAddressOptionResponse;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.repository.ShopRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Public, read-only shop address options used by the customer Search page.
 * Only active shops and non-empty addresses are exposed; owner contact data is not returned.
 */
@RestController
@RequestMapping("/api/shops")
public class ShopPublicController {
    private final ShopRepository shops;

    public ShopPublicController(ShopRepository shops) {
        this.shops = shops;
    }

    @GetMapping
    public ApiResponse<List<ShopAddressOptionResponse>> listPublicShopAddresses() {
        Map<String, ShopAddressOptionResponse> uniqueAddresses = new LinkedHashMap<>();
        for (Shop shop : shops.findByStatusOrderByShopNameAsc(ShopStatus.ACTIVE)) {
            String address = shop.getAddress() == null ? "" : shop.getAddress().trim();
            if (address.isEmpty()) {
                continue;
            }
            String key = address.toLowerCase(Locale.ROOT);
            uniqueAddresses.putIfAbsent(key, new ShopAddressOptionResponse(address, shop.getShopName()));
        }
        return ApiResponse.success(
                "Lấy danh sách địa chỉ cửa hàng thành công.",
                List.copyOf(uniqueAddresses.values())
        );
    }
}
