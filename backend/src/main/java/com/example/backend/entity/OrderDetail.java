package com.example.backend.entity;
import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
@Entity @Table(name="orderdetails")
public class OrderDetail {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="suborder_id") private SubOrder subOrder;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id") private ProductVariant variant;
 @Column(name="product_name",length=255) private String productName;
 @Column(name="price",precision=18,scale=2) private BigDecimal price;
 @Column(name="quantity") private Integer quantity=0;
 @Column(name="discount_amount",precision=18,scale=2) private BigDecimal discountAmount=BigDecimal.ZERO;
 @Column(name="total_amount",precision=18,scale=2) private BigDecimal totalAmount=BigDecimal.ZERO;
 public OrderDetail(){} public UUID getId(){return id;} public SubOrder getSubOrder(){return subOrder;} public void setSubOrder(SubOrder v){subOrder=v;} public ProductVariant getVariant(){return variant;} public void setVariant(ProductVariant v){variant=v;} public String getProductName(){return productName;} public void setProductName(String v){productName=v;} public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;} public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;} public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;}
}