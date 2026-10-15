package com.example.backend.entity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="inventories")
public class Inventory {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id") private ProductVariant variant;
    @Column(name="quantity") private Integer quantity=0;
    @Column(name="reserved_quantity") private Integer reservedQuantity=0;
    @Column(name="sold_quantity") private Integer soldQuantity=0;
    @Column(name="low_stock_threshold") private Integer lowStockThreshold;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    public Inventory(){} @PrePersist @PreUpdate protected void updateTimestamp(){updatedAt=OffsetDateTime.now();}
    public UUID getId(){return id;} public ProductVariant getVariant(){return variant;} public Integer getQuantity(){return quantity;} public Integer getReservedQuantity(){return reservedQuantity;} public Integer getSoldQuantity(){return soldQuantity;} public Integer getLowStockThreshold(){return lowStockThreshold;} public void setVariant(ProductVariant v){variant=v;} public void setQuantity(Integer v){quantity=v;} public void setReservedQuantity(Integer v){reservedQuantity=v;} public void setSoldQuantity(Integer v){soldQuantity=v;} public void setLowStockThreshold(Integer v){lowStockThreshold=v;}
}
