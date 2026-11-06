package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="shopfollowers")
public class ShopFollower {
    @Id @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id", nullable=false)
    private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt;

    @PrePersist void prePersist(){if(createdAt==null)createdAt=OffsetDateTime.now();}
    public UUID getId(){return id;}
    public Shop getShop(){return shop;}
    public void setShop(Shop shop){this.shop=shop;}
    public User getUser(){return user;}
    public void setUser(User user){this.user=user;}
}
