package com.example.backend.entity;
import java.math.BigDecimal;
 import java.time.OffsetDateTime;
 import java.util.UUID;

 import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
@Entity @Table(name="ordercancellationaudits",indexes={@Index(name="idx_order_cancel_audit_user_created",columnList="user_id,created_at"),@Index(name="idx_order_cancel_audit_order",columnList="order_id")},uniqueConstraints=@UniqueConstraint(name="uq_order_cancel_audit_event_key",columnNames="event_key"))
public class OrderCancellationAudit {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private Order order;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="suborder_id",nullable=false) private SubOrder subOrder;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="coupon_id") private Coupon coupon;
 @Column(name="coupon_discount_retained",precision=18,scale=2,nullable=false) private BigDecimal couponDiscountRetained=BigDecimal.ZERO;
 @Column(name="cancellation_reason",columnDefinition="text") private String cancellationReason;
 @Column(name="window_count",nullable=false) private Integer windowCount=1;
 @Enumerated(EnumType.STRING) @Column(name="risk_level",length=20,nullable=false) private CancellationRiskLevel riskLevel=CancellationRiskLevel.LEVEL_1;
 @Column(name="event_key",length=100,nullable=false) private String eventKey;
 @Column(name="minutes_to_cancel") private Long minutesToCancel=0L;
 @Column(name="weekly_event_count") private Integer weeklyEventCount=1;
 @Column(name="monthly_event_count") private Integer monthlyEventCount=1;
 @Column(name="retained_discount_7d",precision=18,scale=2) private BigDecimal retainedDiscount7d=BigDecimal.ZERO;
 @Column(name="retained_discount_30d",precision=18,scale=2) private BigDecimal retainedDiscount30d=BigDecimal.ZERO;
 @Column(name="risk_score") private Integer riskScore=0;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
 @PrePersist void create(){if(createdAt==null)createdAt=OffsetDateTime.now();if(couponDiscountRetained==null)couponDiscountRetained=BigDecimal.ZERO;if(windowCount==null)windowCount=1;if(riskLevel==null)riskLevel=CancellationRiskLevel.LEVEL_1;if(minutesToCancel==null)minutesToCancel=0L;if(weeklyEventCount==null)weeklyEventCount=1;if(monthlyEventCount==null)monthlyEventCount=1;if(retainedDiscount7d==null)retainedDiscount7d=BigDecimal.ZERO;if(retainedDiscount30d==null)retainedDiscount30d=BigDecimal.ZERO;if(riskScore==null)riskScore=0;}
 public OrderCancellationAudit(){} public UUID getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public Order getOrder(){return order;} public void setOrder(Order v){order=v;} public SubOrder getSubOrder(){return subOrder;} public void setSubOrder(SubOrder v){subOrder=v;} public Coupon getCoupon(){return coupon;} public void setCoupon(Coupon v){coupon=v;} public BigDecimal getCouponDiscountRetained(){return couponDiscountRetained;} public void setCouponDiscountRetained(BigDecimal v){couponDiscountRetained=v;} public String getCancellationReason(){return cancellationReason;} public void setCancellationReason(String v){cancellationReason=v;} public Integer getWindowCount(){return windowCount;} public void setWindowCount(Integer v){windowCount=v;} public CancellationRiskLevel getRiskLevel(){return riskLevel;} public void setRiskLevel(CancellationRiskLevel v){riskLevel=v;} public String getEventKey(){return eventKey;} public void setEventKey(String v){eventKey=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
 public Long getMinutesToCancel(){return minutesToCancel;} public void setMinutesToCancel(Long v){minutesToCancel=v;}
 public Integer getWeeklyEventCount(){return weeklyEventCount;} public void setWeeklyEventCount(Integer v){weeklyEventCount=v;}
 public Integer getMonthlyEventCount(){return monthlyEventCount;} public void setMonthlyEventCount(Integer v){monthlyEventCount=v;}
 public BigDecimal getRetainedDiscount7d(){return retainedDiscount7d;} public void setRetainedDiscount7d(BigDecimal v){retainedDiscount7d=v;}
 public BigDecimal getRetainedDiscount30d(){return retainedDiscount30d;} public void setRetainedDiscount30d(BigDecimal v){retainedDiscount30d=v;}
 public Integer getRiskScore(){return riskScore;} public void setRiskScore(Integer v){riskScore=v;}
}