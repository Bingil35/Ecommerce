package com.example.backend.entity;
import jakarta.persistence.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;
@Entity @Table(name="product_variants")
public class ProductVariant {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") private Product product;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="primary_media_id") private ProductMedia primaryMedia;
    @Column(name="sku",length=255) private String sku;
    @Column(name="price",precision=18,scale=2) private BigDecimal price;
    @Column(name="compare_price",precision=18,scale=2) private BigDecimal comparePrice;
    @Column(name="cost_price",precision=18,scale=2) private BigDecimal costPrice;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="status",columnDefinition="product_status_enum") private ProductStatus status=ProductStatus.ACTIVE;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Transient private Integer legacyStockQuantity;
    public ProductVariant(){} @PrePersist protected void onCreate(){OffsetDateTime n=OffsetDateTime.now();createdAt=n;updatedAt=n;} @PreUpdate protected void onUpdate(){updatedAt=OffsetDateTime.now();}
    public UUID getId(){return id;} public Product getProduct(){return product;} public ProductMedia getPrimaryMedia(){return primaryMedia;} public String getSku(){return sku;} public BigDecimal getPrice(){return price;} public BigDecimal getComparePrice(){return comparePrice;} public BigDecimal getCostPrice(){return costPrice;} public ProductStatus getStatus(){return status;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
    public void setProduct(Product v){product=v;} public void setPrimaryMedia(ProductMedia v){primaryMedia=v;} public void setSku(String v){sku=v;} public void setPrice(BigDecimal v){price=v;} public void setComparePrice(BigDecimal v){comparePrice=v;} public void setCostPrice(BigDecimal v){costPrice=v;} public void setStatus(ProductStatus v){status=v;}
    // Temporary API compatibility: stock is persisted in Inventories, not product_variants.
    public Integer getStockQuantity(){return legacyStockQuantity;} public void setStockQuantity(Integer v){legacyStockQuantity=v;}
    public String getImageUrl(){return primaryMedia==null?null:primaryMedia.getUrl();} public void setImageUrl(String v) { /* use primary_media_id and ProductMedia instead */ }
}
