package com.example.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.NotificationResponse;
import com.example.backend.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(Authentication auth) {
        return ApiResponse.success("Thông báo", service.list(auth.getName()).stream().map(NotificationResponse::from).toList());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(Authentication auth) {
        return ApiResponse.success("Số thông báo chưa đọc", Map.of("count", service.unreadCount(auth.getName())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(Authentication auth, @PathVariable UUID id) {
        service.markRead(auth.getName(), id);
        return ApiResponse.success("Đã đánh dấu đã đọc.", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(Authentication auth) {
        service.markAllRead(auth.getName());
        return ApiResponse.success("Đã đọc tất cả thông báo.", null);
    }
}
