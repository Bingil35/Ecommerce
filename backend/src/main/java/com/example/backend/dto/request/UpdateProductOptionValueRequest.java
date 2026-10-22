package com.example.backend.dto.request;

public class UpdateProductOptionValueRequest {

    private String value;

    public UpdateProductOptionValueRequest() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}