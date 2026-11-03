package com.example.backend.dto.request;

import com.example.backend.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSubOrderStatusRequest(@NotNull OrderStatus status, String note) {}
