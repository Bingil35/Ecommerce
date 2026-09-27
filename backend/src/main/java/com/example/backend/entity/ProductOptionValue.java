package com.example.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "product_option_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_product_option_values",
                        columnNames = {
                                "option_id",
                                "value"
                        }
                )
        }
)
public class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "option_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_option_values_option"
            )
    )
    private ProductOption option;

    @Column(
            name = "value",
            nullable = false,
            length = 255
    )
    private String value;

    public ProductOptionValue() {
    }

    public UUID getId() {
        return id;
    }

    public ProductOption getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }

    public void setOption(ProductOption option) {
        this.option = option;
    }

    public void setValue(String value) {
        this.value = value;
    }
}