package com.example.backend.dto.response;

import com.example.backend.entity.ProductOptionValue;

import java.util.UUID;

public class ProductOptionValueResponse {

    private UUID id;
    private UUID optionId;
    private String value;

    public ProductOptionValueResponse() {
    }

    public ProductOptionValueResponse(
            ProductOptionValue optionValue
    ) {
        this.id = optionValue.getId();
        this.optionId =
                optionValue.getOption().getId();
        this.value = optionValue.getValue();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOptionId() {
        return optionId;
    }

    public String getValue() {
        return value;
    }
}