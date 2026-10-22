package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The customer's cart, grouped by shop. Totals include only available lines, so an
 * unavailable line is shown to the customer but never added to a total.
 */
public class CartResponse {

    /** Null when the customer has never added anything to the cart. */
    private UUID cartId;
    private List<SubCartResponse> shops;
    /** Sum of quantities over available lines of all shops. */
    private Integer totalQuantity;
    /** Sum of subtotals over available lines of all shops, scale 2. */
    private BigDecimal subtotal;

    public CartResponse(
            UUID cartId,
            List<SubCartResponse> shops,
            Integer totalQuantity,
            BigDecimal subtotal
    ) {
        this.cartId = cartId;
        this.shops = shops;
        this.totalQuantity = totalQuantity;
        this.subtotal = subtotal;
    }

    public static CartResponse empty(UUID cartId) {
        return new CartResponse(cartId, List.of(), 0, BigDecimal.ZERO.setScale(2));
    }

    public UUID getCartId() { return cartId; }
    public List<SubCartResponse> getShops() { return shops; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
