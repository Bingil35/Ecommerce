package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ChatConversationResponse;
import com.example.backend.dto.response.ChatMessageResponse;
import com.example.backend.service.ChatService;
import com.example.backend.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chat;
    private final UserRepository users;
    public ChatController(ChatService chat, UserRepository users) { this.chat = chat; this.users = users; }

    @GetMapping("/customer/conversations")
    public ApiResponse<List<ChatConversationResponse>> customerConversations(Authentication auth) {
        return ApiResponse.success("Lấy hội thoại thành công.", chat.customerConversations(auth.getName()));
    }
    @PostMapping("/customer/conversations")
    public ApiResponse<ChatConversationResponse> start(@RequestParam UUID shopId, Authentication auth) {
        return ApiResponse.success("Mở cuộc trò chuyện thành công.", chat.start(auth.getName(), shopId));
    }
    @GetMapping("/shop-owner/conversations")
    public ApiResponse<List<ChatConversationResponse>> ownerConversations(Authentication auth) {
        return ApiResponse.success("Lấy hội thoại thành công.", chat.ownerConversations(auth.getName()));
    }
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> history(@PathVariable UUID conversationId,
            @RequestParam(defaultValue = "100") int size, Authentication auth) {
        UUID userId = users.findByEmail(auth.getName()).orElseThrow().getId();
        return ApiResponse.success("Lấy tin nhắn thành công.", chat.history(conversationId, userId, size));
    }

    @PostMapping(value = "/conversations/{conversationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChatMediaResponse> uploadMedia(@PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file, Authentication auth) {
        UUID userId = users.findByEmail(auth.getName()).orElseThrow().getId();
        ChatService.StoredChatMedia media = chat.uploadMedia(conversationId, userId, file);
        return ApiResponse.success("Tải media chat thành công.", new ChatMediaResponse(media.url(), media.messageType()));
    }

    public record ChatMediaResponse(String url, String messageType) {}
}
