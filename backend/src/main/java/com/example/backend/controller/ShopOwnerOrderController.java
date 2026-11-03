package com.example.backend.controller;

import com.example.backend.dto.request.UpdateSubOrderStatusRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.service.ShopOwnerOrderService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-owner/orders/sub-orders")
public class ShopOwnerOrderController {
    private final ShopOwnerOrderService service;
    public ShopOwnerOrderController(ShopOwnerOrderService service) { this.service=service; }
    @PatchMapping("/{subOrderId}/status")
    public ResponseEntity<ApiResponse<Map<String,String>>> updateStatus(Authentication authentication,
            @PathVariable UUID subOrderId, @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        service.updateStatus(authentication.getName(),subOrderId,request.status(),request.note());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công.",Map.of("status",request.status().name())));
    }
}
