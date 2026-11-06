package com.example.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductReviewResponse(UUID id, UUID productId, String productName, String customerName,
        Short rating, String content, OffsetDateTime createdAt, List<Reply> replies) {
    public record Reply(UUID id, String content, OffsetDateTime createdAt) {}
}
