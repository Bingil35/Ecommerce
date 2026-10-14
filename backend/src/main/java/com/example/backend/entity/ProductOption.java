package com.example.backend.entity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="product_options")
public class ProductOption {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") private Product product;
    @Column(name="name",length=255) private String name;
    @Column(name="sort_order") private Short sortOrder=0;
    public ProductOption(){} public UUID getId(){return id;} public Product getProduct(){return product;} public String getName(){return name;} public Short getSortOrder(){return sortOrder;} public void setProduct(Product v){product=v;} public void setName(String v){name=v;} public void setSortOrder(Short v){sortOrder=v;}
}
