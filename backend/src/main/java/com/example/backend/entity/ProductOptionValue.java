package com.example.backend.entity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="product_option_values")
public class ProductOptionValue {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_option_id") private ProductOption productOption;
    @Column(name="value",columnDefinition="text") private String value;
    @Column(name="sort_order") private Short sortOrder=0;
    public ProductOptionValue(){} public UUID getId(){return id;} public ProductOption getProductOption(){return productOption;} public ProductOption getOption(){return productOption;} public String getValue(){return value;} public Short getSortOrder(){return sortOrder;} public void setProductOption(ProductOption v){productOption=v;} public void setOption(ProductOption v){productOption=v;} public void setValue(String v){value=v;} public void setSortOrder(Short v){sortOrder=v;}
}
