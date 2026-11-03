package com.example.backend.entity;
import java.time.OffsetDateTime;
 import java.util.UUID;

 import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
@Entity @Table(name="couponredemtions",indexes={@Index(name="idx_couponredemtions_user_coupon",columnList="user_id,coupon_id")})
public class CouponRedemption {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="coupon_id",nullable=false) private Coupon coupon;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private Order order;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="suborder_id") private SubOrder subOrder;
 @Column(name="redeemed_at",nullable=false,updatable=false) private OffsetDateTime redeemedAt;
 @PrePersist void create(){if(redeemedAt==null)redeemedAt=OffsetDateTime.now();}
 public CouponRedemption(){} public UUID getId(){return id;} public Coupon getCoupon(){return coupon;} public void setCoupon(Coupon v){coupon=v;} public User getUser(){return user;} public void setUser(User v){user=v;} public Order getOrder(){return order;} public void setOrder(Order v){order=v;} public SubOrder getSubOrder(){return subOrder;} public void setSubOrder(SubOrder v){subOrder=v;} public OffsetDateTime getRedeemedAt(){return redeemedAt;}
}