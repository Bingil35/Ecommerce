package com.example.backend.dto.response;

/** Minimal public DTO for SearchPage's address checkbox options. */
public class ShopAddressOptionResponse {
    private final String id;
    private final String name;
    private final String address;

    public ShopAddressOptionResponse(String address, String name) {
        this.id = address;
        this.name = name;
        this.address = address;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
}
