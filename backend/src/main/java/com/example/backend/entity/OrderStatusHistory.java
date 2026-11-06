package com.example.backend.entity;
import jakarta.persistence.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="orderstatushistories")
public class OrderStatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="suborder_id") private SubOrder subOrder;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="old_status",columnDefinition="order_status_enum") private OrderStatus oldStatus;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="new_status",columnDefinition="order_status_enum") private OrderStatus newStatus;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by") private User changedBy;
 @Column(name="note",columnDefinition="text") private String note;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @PrePersist void create(){createdAt=OffsetDateTime.now();}
 public OrderStatusHistory(){} public UUID getId(){return id;} public SubOrder getSubOrder(){return subOrder;} public void setSubOrder(SubOrder v){subOrder=v;} public OrderStatus getOldStatus(){return oldStatus;} public void setOldStatus(OrderStatus v){oldStatus=v;} public OrderStatus getNewStatus(){return newStatus;} public void setNewStatus(OrderStatus v){newStatus=v;} public User getChangedBy(){return changedBy;} public void setChangedBy(User v){changedBy=v;} public String getNote(){return note;} public void setNote(String v){note=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}