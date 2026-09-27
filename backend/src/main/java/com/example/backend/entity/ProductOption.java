package com.example.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "product_options",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_product_options_name",
                        columnNames = {
                                "product_id",
                                "name"
                        }
                )
        }
)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_options_product"
            )
    )
    private Product product;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    public ProductOption() {
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setName(String name) {
        this.name = name;
    }
}