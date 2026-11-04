package com.example.backend.repository;

import com.example.backend.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    Optional<ChatMessage> findFirstByConversation_IdOrderByCreatedAtDesc(UUID conversationId);
    long countByConversation_IdAndSenderIdNotAndReadFalse(UUID conversationId, UUID senderId);
}
