package com.example.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductReviewRequest(@NotNull @Min(1) @Max(5) Short rating,
        @Size(max = 3000) String content, @NotNull UUID subOrderId, UUID variantId) {}
