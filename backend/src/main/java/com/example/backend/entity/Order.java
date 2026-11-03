package com.example.backend.entity;
import jakarta.persistence.*; 
import java.math.BigDecimal; 
import java.time.OffsetDateTime; 
import java.util.UUID;

@Entity @Table(name="orders", indexes={@Index(name="idx_orders_user_created",columnList="user_id,created_at"),@Index(name="idx_orders_order_code",columnList="order_code")})
public class Order {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") private Payment payment;
 @Column(name="order_code",length=255) private String orderCode;
 @Column(name="subtotal",precision=18,scale=2) private BigDecimal subtotal=BigDecimal.ZERO;
 @Column(name="discount_amount",precision=18,scale=2) private BigDecimal discountAmount=BigDecimal.ZERO;
 @Column(name="total_amount",precision=18,scale=2) private BigDecimal totalAmount=BigDecimal.ZERO;
 @Enumerated(EnumType.STRING) @Column(name="payment_status",length=20,nullable=false) private PaymentStatus paymentStatus=PaymentStatus.PENDING;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void create(){OffsetDateTime n=OffsetDateTime.now();createdAt=n;updatedAt=n;if(paymentStatus==null)paymentStatus=PaymentStatus.PENDING;} @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
 public Order(){} 
 public UUID getId(){return id;} 
 public User getUser(){return user;} 
 public void setUser(User v){user=v;} 
 public Payment getPayment(){return payment;} 
 public void setPayment(Payment v){payment=v;} 
 public String getOrderCode(){return orderCode;} 
 public void setOrderCode(String v){orderCode=v;} 
 public BigDecimal getSubtotal(){return subtotal;} 
 public void setSubtotal(BigDecimal v){subtotal=v;} 
 public BigDecimal getDiscountAmount(){return discountAmount;} 
 public void setDiscountAmount(BigDecimal v){discountAmount=v;} 
 public BigDecimal getTotalAmount(){return totalAmount;} 
 public void setTotalAmount(BigDecimal v){totalAmount=v;} 
 public PaymentStatus getPaymentStatus(){return paymentStatus;} 
 public void setPaymentStatus(PaymentStatus v){paymentStatus=v;}
 public OffsetDateTime getCreatedAt(){return createdAt;} 
 public OffsetDateTime getUpdatedAt(){return updatedAt;}
}