package com.example.backend.dto.response;

import com.example.backend.entity.ChatMessage;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(UUID id, UUID conversationId, UUID senderId, String content,
        String messageType, boolean read, OffsetDateTime createdAt) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getConversation().getId(), message.getSenderId(),
                message.getContent(), message.getMessageType().name(), message.isRead(), message.getCreatedAt());
    }
}
