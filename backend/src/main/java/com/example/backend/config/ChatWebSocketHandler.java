package com.example.backend.config;

import com.example.backend.dto.response.ChatMessageResponse;
import com.example.backend.entity.UserStatus;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ChatService;
import com.example.backend.service.JwtService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper json;
    private final JwtService jwt;
    private final UserRepository users;
    private final ChatService chat;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper json, JwtService jwt, UserRepository users, ChatService chat) {
        this.json = json; this.jwt = jwt; this.users = users; this.chat = chat;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage frame) {
        try {
            JsonNode input = json.readTree(frame.getPayload());
            String type = input.path("type").asText();
            if ("AUTH".equals(type)) { authenticate(session, input.path("token").asText()); return; }
            UUID userId = (UUID) session.getAttributes().get("userId");
            if (userId == null) { sendError(session, "Xác thực kết nối trước khi chat."); return; }
            if ("SUBSCRIBE".equals(type)) { subscribe(session, userId, UUID.fromString(input.path("conversationId").asText())); return; }
            if ("UNSUBSCRIBE".equals(type)) { unsubscribe(session); sendJson(session, json.createObjectNode().put("type", "UNSUBSCRIBED")); return; }
            if ("SEND".equals(type)) { sendMessage(session, userId, input); return; }
            sendError(session, "Loại sự kiện chat không được hỗ trợ.");
        } catch (Exception error) {
            sendError(session, error instanceof IllegalArgumentException ? error.getMessage() : "Không thể xử lý yêu cầu chat.");
        }
    }

    private void authenticate(WebSocketSession session, String token) throws Exception {
        unsubscribe(session);
        session.getAttributes().remove("userId");
        if (!jwt.isTokenValid(token)) { sendError(session, "Phiên đăng nhập đã hết hạn."); session.close(CloseStatus.NOT_ACCEPTABLE); return; }
        UUID userId = UUID.fromString(jwt.extractUserId(token));
        var user = users.findById(userId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElse(null);
        if (user == null) { sendError(session, "Tài khoản không hoạt động."); session.close(CloseStatus.NOT_ACCEPTABLE); return; }
        session.getAttributes().put("userId", userId);
        sendJson(session, json.createObjectNode().put("type", "AUTH_OK").put("userId", userId.toString()));
    }

    private void subscribe(WebSocketSession session, UUID userId, UUID conversationId) throws Exception {
        if (!chat.canAccess(conversationId, userId)) { sendError(session, "Bạn không có quyền tham gia cuộc trò chuyện này."); return; }
        unsubscribe(session);
        session.getAttributes().put("conversationId", conversationId);
        subscribers.computeIfAbsent(conversationId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        sendJson(session, json.createObjectNode().put("type", "SUBSCRIBED").put("conversationId", conversationId.toString()));
    }

    private void sendMessage(WebSocketSession session, UUID userId, JsonNode input) throws Exception {
        UUID conversationId = UUID.fromString(input.path("conversationId").asText());
        if (!conversationId.equals(session.getAttributes().get("conversationId"))) { sendError(session, "Tham gia hội thoại trước khi gửi tin nhắn."); return; }
        ChatMessageResponse message = chat.send(conversationId, userId, input.path("content").asText(""), input.path("messageType").asText("TEXT"));
        JsonNode event = json.createObjectNode().put("type", "MESSAGE").set("message", json.valueToTree(message));
        String payload = json.writeValueAsString(event);
        for (WebSocketSession receiver : subscribers.getOrDefault(conversationId, Set.of())) {
            if (receiver.isOpen()) synchronized (receiver) { receiver.sendMessage(new TextMessage(payload)); }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { unsubscribe(session); }

    private void unsubscribe(WebSocketSession session) {
        Object id = session.getAttributes().remove("conversationId");
        if (id instanceof UUID conversationId) {
            Set<WebSocketSession> sessions = subscribers.get(conversationId);
            if (sessions != null) { sessions.remove(session); if (sessions.isEmpty()) subscribers.remove(conversationId, sessions); }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try { sendJson(session, json.createObjectNode().put("type", "ERROR").put("message", message == null ? "Lỗi chat." : message)); }
        catch (Exception ignored) { }
    }
    private void sendJson(WebSocketSession session, JsonNode payload) throws Exception {
        if (session.isOpen()) synchronized (session) { session.sendMessage(new TextMessage(json.writeValueAsString(payload))); }
    }
}
