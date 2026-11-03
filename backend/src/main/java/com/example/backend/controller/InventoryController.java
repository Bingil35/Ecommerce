package com.example.backend.controller;

import com.example.backend.dto.request.LowStockThresholdRequest;
import com.example.backend.dto.request.StockAdjustRequest;
import com.example.backend.dto.request.StockInRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.InventoryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Shop;
import com.example.backend.service.InventoryService;
import com.example.backend.service.ProductOwnershipService;
import com.example.backend.service.ShopService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Quản lý tồn kho của shop owner (dùng bảng Inventories hiện có).
 * Mọi thao tác theo variant đều kiểm tra variant thuộc shop của owner;
 * API toàn shop luôn giới hạn theo shop của tài khoản đăng nhập.
 */
@RestController
@RequestMapping("/api/shop-owner")
public class InventoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryService inventoryService;
    private final ProductOwnershipService ownership;
    private final ShopService shopService;

    public InventoryController(
            InventoryService inventoryService,
            ProductOwnershipService ownership,
            ShopService shopService
    ) {
        this.inventoryService = inventoryService;
        this.ownership = ownership;
        this.shopService = shopService;
    }

    @GetMapping("/variants/{variantId}/inventory")
    public ApiResponse<InventoryResponse> getInventory(
            @PathVariable UUID variantId,
            Authentication authentication
    ) {
        ownership.assertVariant(variantId, authentication.getName());

        return ApiResponse.success(
                "Lấy thông tin tồn kho thành công.",
                inventoryService.getInventory(variantId)
        );
    }

    @PostMapping("/variants/{variantId}/inventory/stock-in")
    public ApiResponse<InventoryResponse> stockIn(
            @PathVariable UUID variantId,
            @RequestBody StockInRequest request,
            Authentication authentication
    ) {
        ownership.assertVariant(variantId, authentication.getName());

        return ApiResponse.success(
                "Nhập kho thành công.",
                inventoryService.stockIn(
                        variantId,
                        request == null ? null : request.quantity(),
                        request == null ? null : request.note()
                )
        );
    }

    @PostMapping("/variants/{variantId}/inventory/adjust")
    public ApiResponse<InventoryResponse> adjust(
            @PathVariable UUID variantId,
            @RequestBody StockAdjustRequest request,
            Authentication authentication
    ) {
        ownership.assertVariant(variantId, authentication.getName());

        return ApiResponse.success(
                "Điều chỉnh tồn kho thành công.",
                inventoryService.adjust(
                        variantId,
                        request == null ? null : request.delta(),
                        request == null ? null : request.newQuantity(),
                        request == null ? null : request.reason()
                )
        );
    }

    @PutMapping("/variants/{variantId}/inventory/low-stock-threshold")
    public ApiResponse<InventoryResponse> setLowStockThreshold(
            @PathVariable UUID variantId,
            @RequestBody LowStockThresholdRequest request,
            Authentication authentication
    ) {
        ownership.assertVariant(variantId, authentication.getName());

        return ApiResponse.success(
                "Cập nhật ngưỡng cảnh báo thành công.",
                inventoryService.setLowStockThreshold(
                        variantId,
                        request == null ? null : request.lowStockThreshold()
                )
        );
    }

    /** Cảnh báo tồn thấp: biến thể ACTIVE có tồn khả dụng <= ngưỡng. */
    @GetMapping("/inventory/low-stock")
    public ApiResponse<PageResponse<InventoryResponse>> getLowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return ApiResponse.success(
                "Lấy danh sách cảnh báo tồn kho thấp thành công.",
                PageResponse.from(
                        inventoryService.lowStock(myShop.getId(), safePage, safeSize),
                        r -> r
                )
        );
    }
}
