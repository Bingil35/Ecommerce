package com.example.backend.entity;
import jakarta.persistence.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="payments")
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="payment_method",columnDefinition="payment_method_enum") private PaymentMethod paymentMethod=PaymentMethod.COD;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void create(){OffsetDateTime n=OffsetDateTime.now();createdAt=n;updatedAt=n;if(paymentMethod==null)paymentMethod=PaymentMethod.COD;} @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
 public Payment(){} public UUID getId(){return id;} public PaymentMethod getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(PaymentMethod v){paymentMethod=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}