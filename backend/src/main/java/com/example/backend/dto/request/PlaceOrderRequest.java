package com.example.backend.dto.request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.backend.entity.PaymentMethod;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Request for placing an order from selected cart lines. Prices are never accepted from the client. */
public class PlaceOrderRequest {
    @NotNull
    private UUID addressId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotEmpty
    private List<@NotNull UUID> itemIds;

    private String platformCouponCode;

    /** Shop ID -> coupon code. Only shops represented in itemIds may be specified. */
    private Map<UUID, String> shopCouponCodes;

    public UUID getAddressId() { return addressId; }
    public void setAddressId(UUID addressId) { this.addressId = addressId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<UUID> getItemIds() { return itemIds; }
    public void setItemIds(List<UUID> itemIds) { this.itemIds = itemIds; }
    public String getPlatformCouponCode() { return platformCouponCode; }
    public void setPlatformCouponCode(String platformCouponCode) { this.platformCouponCode = platformCouponCode; }
    public Map<UUID, String> getShopCouponCodes() { return shopCouponCodes; }
    public void setShopCouponCodes(Map<UUID, String> shopCouponCodes) { this.shopCouponCodes = shopCouponCodes; }
}
