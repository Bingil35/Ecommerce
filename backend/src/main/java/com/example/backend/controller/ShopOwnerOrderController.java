package com.example.backend.controller;

import com.example.backend.dto.request.UpdateSubOrderStatusRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ShopOwnerOrderResponse;
import com.example.backend.entity.OrderStatus;
import com.example.backend.service.ShopOwnerOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-owner/orders")
public class ShopOwnerOrderController {
    private final ShopOwnerOrderService service;
    public ShopOwnerOrderController(ShopOwnerOrderService service) { this.service=service; }
    @GetMapping
    public ApiResponse<PageResponse<ShopOwnerOrderResponse>> list(Authentication authentication,
            @RequestParam(required=false) OrderStatus status, @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công.", service.list(authentication.getName(),status,keyword,page,size));
    }
    @GetMapping("/sub-orders/{subOrderId}")
    public ApiResponse<ShopOwnerOrderResponse> get(Authentication authentication, @PathVariable UUID subOrderId) {
        return ApiResponse.success("Order detail loaded.", service.get(authentication.getName(), subOrderId));
    }
    @PatchMapping("/sub-orders/{subOrderId}/status")
    public ResponseEntity<ApiResponse<Map<String,String>>> updateStatus(Authentication authentication,
            @PathVariable UUID subOrderId, @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        service.updateStatus(authentication.getName(),subOrderId,request.status(),request.note());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công.",Map.of("status",OrderStatus.SHIPPING.name())));
    }
}
