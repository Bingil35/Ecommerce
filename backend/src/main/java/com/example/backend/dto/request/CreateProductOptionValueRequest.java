package com.example.backend.dto.request;

public class CreateProductOptionValueRequest {

    private String value;

    public CreateProductOptionValueRequest() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}