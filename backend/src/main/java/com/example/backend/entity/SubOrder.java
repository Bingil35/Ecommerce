package com.example.backend.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal; 
import java.time.OffsetDateTime; 
import java.util.UUID;
@Entity @Table(name="suborders",indexes={@Index(name="idx_suborders_order",columnList="order_id"),@Index(name="idx_suborders_shop_status",columnList="shop_id,status")})
public class SubOrder {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private Order order;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id") private Shop shop;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_address_id") private OrderAddress orderAddress;
 @Column(name="subtotal",precision=18,scale=2) private BigDecimal subtotal=BigDecimal.ZERO;
 @Column(name="discount_amount",precision=18,scale=2) private BigDecimal discountAmount=BigDecimal.ZERO;
 @Column(name="total_amount",precision=18,scale=2) private BigDecimal totalAmount=BigDecimal.ZERO;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="status",columnDefinition="order_status_enum") private OrderStatus status=OrderStatus.PENDING;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void create(){OffsetDateTime n=OffsetDateTime.now();createdAt=n;updatedAt=n;if(status==null)status=OrderStatus.PENDING;} @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
 public SubOrder(){} public UUID getId(){return id;} public Order getOrder(){return order;} public void setOrder(Order v){order=v;} public Shop getShop(){return shop;} public void setShop(Shop v){shop=v;} public OrderAddress getOrderAddress(){return orderAddress;} public void setOrderAddress(OrderAddress v){orderAddress=v;} public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;} public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public OrderStatus getStatus(){return status;} public void setStatus(OrderStatus v){status=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}