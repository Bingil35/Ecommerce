package com.example.backend.dto.response;

import com.example.backend.entity.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentConfirmationResponse(UUID orderId, String orderCode, PaymentStatus paymentStatus,
        OffsetDateTime confirmedAt, String message) {}
