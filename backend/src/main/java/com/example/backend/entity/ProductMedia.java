package com.example.backend.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="product_media")
public class ProductMedia {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") private Product product;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name="media_type",columnDefinition="media_type_enum") private ProductMediaType mediaType;
    @Column(name="url",columnDefinition="text") private String url;
    @Column(name="sort_order") private Short sortOrder=0;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    public ProductMedia(){} @PrePersist protected void onCreate(){createdAt=OffsetDateTime.now();}
    public UUID getId(){return id;} public Product getProduct(){return product;} public ProductMediaType getMediaType(){return mediaType;} public String getUrl(){return url;} public Short getSortOrder(){return sortOrder;} public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setProduct(Product v){product=v;} public void setMediaType(ProductMediaType v){mediaType=v;} public void setUrl(String v){url=v;} public void setSortOrder(Short v){sortOrder=v;}
    // Backward-compatible Java accessors; map to baseline columns url/sort_order.
    public String getMediaUrl(){return url;} public void setMediaUrl(String v){url=v;} public Integer getDisplayOrder(){return sortOrder==null?null:sortOrder.intValue();} public void setDisplayOrder(Integer v){sortOrder=v==null?null:v.shortValue();}
}
