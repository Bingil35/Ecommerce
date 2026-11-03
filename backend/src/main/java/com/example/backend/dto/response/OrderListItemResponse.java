package com.example.backend.dto.response;

import com.example.backend.entity.Order;
import com.example.backend.entity.PaymentMethod;
import com.example.backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderListItemResponse(UUID id, String orderCode, PaymentMethod paymentMethod, PaymentStatus paymentStatus, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount, OffsetDateTime createdAt, long subOrderCount) {
    public static OrderListItemResponse from(Order order, long subOrderCount) {
        return new OrderListItemResponse(order.getId(), order.getOrderCode(), order.getPayment() == null ? null : order.getPayment().getPaymentMethod(), order.getPaymentStatus(), order.getSubtotal(), order.getDiscountAmount(), order.getTotalAmount(), order.getCreatedAt(), subOrderCount);
    }
}
