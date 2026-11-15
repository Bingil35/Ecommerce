package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final List<String> allowedOrigins;
    public ChatWebSocketConfig(ChatWebSocketHandler handler,
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = CorsOriginParser.parse(allowedOrigins);
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
