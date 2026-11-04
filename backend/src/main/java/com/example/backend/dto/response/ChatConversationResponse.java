package com.example.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatConversationResponse(UUID id, UUID customerId, UUID shopId, String otherName,
        String otherAvatarUrl, String lastMessage, OffsetDateTime lastMessageAt, long unreadCount,
        String shopName, String shopOwnerName, String shopSlug) {}
