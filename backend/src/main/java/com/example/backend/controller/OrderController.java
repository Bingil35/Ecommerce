package com.example.backend.controller;

import com.example.backend.dto.request.PlaceOrderRequest;
import com.example.backend.dto.response.*;
import com.example.backend.service.OrderCancellationService;
import com.example.backend.service.OrderQueryService;
import com.example.backend.service.OrderService;
import com.example.backend.service.CodPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import com.example.backend.entity.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final OrderCancellationService orderCancellationService;
    private final CodPaymentService codPaymentService;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService, OrderCancellationService orderCancellationService, CodPaymentService codPaymentService) {
        this.orderService = orderService; this.orderQueryService = orderQueryService; this.orderCancellationService = orderCancellationService; this.codPaymentService = codPaymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlacedOrderResponse>> placeOrder(Authentication authentication, @Valid @RequestBody PlaceOrderRequest request) {
        PlacedOrderResponse response = orderService.placeOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt hàng thành công.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderListItemResponse>>> getMyOrders(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status, @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) OffsetDateTime from, @RequestParam(required = false) OffsetDateTime to) {
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Thông số phân trang không hợp lệ (size tối đa 50).");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công.",
                orderQueryService.getMyOrders(authentication.getName(), status, orderCode, from, to, pageable)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getMyOrder(Authentication authentication, @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công.", orderQueryService.getMyOrder(authentication.getName(), orderId)));
    }

    @PostMapping("/sub-orders/{subOrderId}/cancel")
    public ResponseEntity<ApiResponse<CancelSubOrderResponse>> cancelSubOrder(Authentication authentication, @PathVariable UUID subOrderId, @Valid @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công.", orderCancellationService.cancelMySubOrder(authentication.getName(), subOrderId, reason)));
    }


    @PostMapping("/{orderId}/cod/confirm-received")
    public ResponseEntity<ApiResponse<PaymentConfirmationResponse>> confirmCodReceived(Authentication authentication, @PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán COD thành công.",
                codPaymentService.confirmReceived(authentication.getName(), orderId)));
    }

    @PostMapping("/sub-orders/{subOrderId}/confirm-received")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> confirmSubOrderReceived(Authentication authentication, @PathVariable UUID subOrderId) {
        var status = codPaymentService.confirmSubOrderReceived(authentication.getName(), subOrderId);
        return ResponseEntity.ok(ApiResponse.success("Receipt confirmed.", java.util.Map.of("status", status.name())));
    }

    public record CancelRequest(@Size(max = 500) String reason) {}
}
