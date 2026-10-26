package com.example.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.backend.entity.UserAddress;

public record CustomerAddressResponse(
    UUID id, String recipientName, String recipientPhone,
    String provinceCity, String ward, String addressDetail,
    boolean isDefault, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CustomerAddressResponse from(UserAddress a) {
        return new CustomerAddressResponse(a.getId(), a.getRecipientName(),
            a.getRecipientPhone(), a.getProvinceCity(), a.getWard(),
            a.getAddressDetail(), Boolean.TRUE.equals(a.getIsDefault()),
            a.getCreatedAt(), a.getUpdatedAt());
    }
}
