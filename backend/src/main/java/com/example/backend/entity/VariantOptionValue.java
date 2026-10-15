package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "variant_option_values"
)
public class VariantOptionValue {

    @EmbeddedId
    private VariantOptionValueId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_variant_option_values_variant"
            )
    )
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("optionValueId")
    @JoinColumn(
            name = "option_value_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_variant_option_values_option_value"
            )
    )
    private ProductOptionValue optionValue;

    public VariantOptionValue() {
    }

    public VariantOptionValue(
            ProductVariant variant,
            ProductOptionValue optionValue
    ) {
        this.variant = variant;
        this.optionValue = optionValue;

        this.id = new VariantOptionValueId(
                variant.getId(),
                optionValue.getId()
        );
    }

    public VariantOptionValueId getId() {
        return id;
    }

    public void setId(VariantOptionValueId id) {
        this.id = id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public ProductOptionValue getOptionValue() {
        return optionValue;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public void setOptionValue(
            ProductOptionValue optionValue
    ) {
        this.optionValue = optionValue;
    }
}