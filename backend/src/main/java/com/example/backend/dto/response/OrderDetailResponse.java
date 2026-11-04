package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.PaymentMethod;
import com.example.backend.entity.PaymentStatus;

public record OrderDetailResponse(UUID id, String orderCode, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount, OffsetDateTime createdAt,
        List<SubOrderView> subOrders) {
    public record AddressSnapshot(String recipientName, String recipientPhone, String recipientCity, String ward, String addressDetail) {}
    public record StatusHistoryView(OrderStatus oldStatus, OrderStatus newStatus, String note, OffsetDateTime createdAt) {}
    public record SubOrderView(UUID id, UUID shopId, String shopName, OrderStatus status, BigDecimal subtotal,
            BigDecimal discountAmount, BigDecimal totalAmount, AddressSnapshot shippingAddress,
            List<StatusHistoryView> statusHistory, List<OrderLineView> items) {}
    public record OrderLineView(UUID id, UUID productId, UUID variantId, String productName, BigDecimal price, Integer quantity,
            BigDecimal discountAmount, BigDecimal totalAmount) {}
}
