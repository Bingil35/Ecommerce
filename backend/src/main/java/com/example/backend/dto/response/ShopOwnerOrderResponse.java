package com.example.backend.dto.response;

import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.PaymentMethod;
import com.example.backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShopOwnerOrderResponse(UUID id, String orderCode, String customerName, OrderStatus status,
        PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount, OffsetDateTime createdAt,
        OrderDetailResponse.AddressSnapshot shippingAddress, List<OrderDetailResponse.StatusHistoryView> statusHistory,
        List<OrderDetailResponse.OrderLineView> items) {}
