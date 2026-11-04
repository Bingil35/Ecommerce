package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "message_type", nullable = false, columnDefinition = "message_type_enum")
    private ChatMessageType messageType = ChatMessageType.TEXT;
    @Column(name = "content", columnDefinition = "text")
    private String content;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public ChatConversation getConversation() { return conversation; }
    public void setConversation(ChatConversation value) { conversation = value; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID value) { senderId = value; }
    public ChatMessageType getMessageType() { return messageType; }
    public void setMessageType(ChatMessageType value) { messageType = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public boolean isRead() { return read; }
    public void setRead(boolean value) { read = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
