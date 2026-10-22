package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cart lines grouped under one shop, with totals over the available lines only. */
public class SubCartResponse {

    private UUID subCartId;
    private UUID shopId;
    /** Slug of the shop, for linking to the shop page. */
    private String shopSlug;
    private String shopName;
    private List<CartItemResponse> items;
    /** Sum of quantities over available lines of this shop. */
    private Integer totalQuantity;
    /** Sum of lineSubtotal over available lines of this shop, scale 2. */
    private BigDecimal subtotal;

    public SubCartResponse(
            UUID subCartId,
            UUID shopId,
            String shopSlug,
            String shopName,
            List<CartItemResponse> items,
            Integer totalQuantity,
            BigDecimal subtotal
    ) {
        this.subCartId = subCartId;
        this.shopId = shopId;
        this.shopSlug = shopSlug;
        this.shopName = shopName;
        this.items = items;
        this.totalQuantity = totalQuantity;
        this.subtotal = subtotal;
    }

    public UUID getSubCartId() { return subCartId; }
    public UUID getShopId() { return shopId; }
    public String getShopSlug() { return shopSlug; }
    public String getShopName() { return shopName; }
    public List<CartItemResponse> getItems() { return items; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
