package com.example.backend.dto.response;

import com.example.backend.entity.SubOrder;
import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.PaymentMethod;
import com.example.backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderListItemResponse(UUID id, UUID orderId, String orderCode, String shopName, OrderStatus status,
        PaymentMethod paymentMethod, PaymentStatus paymentStatus, BigDecimal subtotal, BigDecimal discountAmount,
        BigDecimal totalAmount, OffsetDateTime createdAt) {
    public static OrderListItemResponse from(SubOrder subOrder) {
        var order = subOrder.getOrder();
        return new OrderListItemResponse(subOrder.getId(), order.getId(), order.getOrderCode(),
                subOrder.getShop() == null ? null : subOrder.getShop().getShopName(), subOrder.getStatus(),
                order.getPayment() == null ? null : order.getPayment().getPaymentMethod(), order.getPaymentStatus(),
                subOrder.getSubtotal(), subOrder.getDiscountAmount(), subOrder.getTotalAmount(), subOrder.getCreatedAt());
    }
}
