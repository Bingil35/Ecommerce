package com.example.backend.dto.response;

import com.example.backend.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CancelSubOrderResponse(UUID orderId, UUID subOrderId, OrderStatus status, BigDecimal orderTotalAmount, String message) {}
