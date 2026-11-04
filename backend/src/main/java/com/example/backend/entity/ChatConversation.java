package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ChatConversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(OffsetDateTime value) { lastMessageAt = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
