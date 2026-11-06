package com.example.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.example.backend.entity.Notification;
import com.example.backend.entity.NotificationType;

public record NotificationResponse(UUID id, String title, String content,
        NotificationType referenceType, UUID referenceId, Boolean read, OffsetDateTime createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getContent(),
                notification.getReferenceType(), notification.getReferenceId(), notification.getRead(), notification.getCreatedAt());
    }
}
