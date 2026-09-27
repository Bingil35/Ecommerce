package com.example.backend.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class VariantOptionValueId
        implements Serializable {

    private UUID variantId;
    private UUID optionValueId;

    public VariantOptionValueId() {
    }

    public VariantOptionValueId(
            UUID variantId,
            UUID optionValueId
    ) {
        this.variantId = variantId;
        this.optionValueId = optionValueId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public UUID getOptionValueId() {
        return optionValueId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public void setOptionValueId(UUID optionValueId) {
        this.optionValueId = optionValueId;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof VariantOptionValueId that)) {
            return false;
        }

        return variantId.equals(that.variantId)
                && optionValueId.equals(
                that.optionValueId
        );
    }

    @Override
    public int hashCode() {
        return 31 * variantId.hashCode()
                + optionValueId.hashCode();
    }
}