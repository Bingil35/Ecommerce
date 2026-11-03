package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Exact per-suborder discount contribution of a coupon, used for reconciliation and cancellation rules. */
@Entity
@Table(name = "ordercouponallocations", uniqueConstraints = @UniqueConstraint(
        name = "uq_order_coupon_allocation", columnNames = {"coupon_id", "suborder_id"}))
public class OrderCouponAllocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suborder_id", nullable = false)
    private SubOrder subOrder;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public Coupon getCoupon() { return coupon; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public SubOrder getSubOrder() { return subOrder; }
    public void setSubOrder(SubOrder subOrder) { this.subOrder = subOrder; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
