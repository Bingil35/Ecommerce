package com.example.backend.entity;
import jakarta.persistence.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="coupons",indexes={@Index(name="idx_coupons_code",columnList="code")})
public class Coupon {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id") private Shop shop;
 @Column(name="code",length=30) private String code;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="scope",nullable=false,columnDefinition="coupon_scope_enum") private CouponScope scope;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="discount_type",columnDefinition="discount_type_enum") private DiscountType discountType;
 @Column(name="discount_value") private Integer discountValue=0;
 @Column(name="minimum_order",precision=18,scale=2) private BigDecimal minimumOrder;
 @Column(name="maximum_discount",precision=18,scale=2) private BigDecimal maximumDiscount;
 @Column(name="usage_limit") private Integer usageLimit;
 @Column(name="used_count") private Integer usedCount=0;
 @Column(name="start_at") private OffsetDateTime startAt; @Column(name="end_at") private OffsetDateTime endAt;
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="status",columnDefinition="coupon_status_enum") private CouponStatus status;
 @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt; @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void create(){OffsetDateTime n=OffsetDateTime.now();createdAt=n;updatedAt=n;if(usedCount==null)usedCount=0;} @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
 public Coupon(){} public UUID getId(){return id;} public Shop getShop(){return shop;} public void setShop(Shop v){shop=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public CouponScope getScope(){return scope;} public void setScope(CouponScope v){scope=v;} public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType v){discountType=v;} public Integer getDiscountValue(){return discountValue;} public void setDiscountValue(Integer v){discountValue=v;} public BigDecimal getMinimumOrder(){return minimumOrder;} public void setMinimumOrder(BigDecimal v){minimumOrder=v;} public BigDecimal getMaximumDiscount(){return maximumDiscount;} public void setMaximumDiscount(BigDecimal v){maximumDiscount=v;} public Integer getUsageLimit(){return usageLimit;} public void setUsageLimit(Integer v){usageLimit=v;} public Integer getUsedCount(){return usedCount;} public void setUsedCount(Integer v){usedCount=v;} public OffsetDateTime getStartAt(){return startAt;} public void setStartAt(OffsetDateTime v){startAt=v;} public OffsetDateTime getEndAt(){return endAt;} public void setEndAt(OffsetDateTime v){endAt=v;} public CouponStatus getStatus(){return status;} public void setStatus(CouponStatus v){status=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}