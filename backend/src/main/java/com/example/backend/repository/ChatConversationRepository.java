package com.example.backend.repository;

import com.example.backend.entity.ChatConversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    Optional<ChatConversation> findByCustomer_IdAndShop_Id(UUID customerId, UUID shopId);
    List<ChatConversation> findByCustomer_IdOrderByLastMessageAtDesc(UUID customerId);
    List<ChatConversation> findByShop_IdOrderByLastMessageAtDesc(UUID shopId);
}
