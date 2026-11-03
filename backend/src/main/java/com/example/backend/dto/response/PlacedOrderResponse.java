package com.example.backend.dto.response;

import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.PaymentMethod;
import com.example.backend.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PlacedOrderResponse {
    private UUID id;
    private String orderCode;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private List<SubOrderItem> subOrders;

    public PlacedOrderResponse(UUID id, String orderCode, PaymentMethod paymentMethod,
            PaymentStatus paymentStatus, BigDecimal subtotal, BigDecimal discountAmount,
            BigDecimal totalAmount, OffsetDateTime createdAt, List<SubOrderItem> subOrders) {
        this.id = id;
        this.orderCode = orderCode;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.subOrders = subOrders;
    }

    public UUID getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<SubOrderItem> getSubOrders() { return subOrders; }

    public static class SubOrderItem {
        private UUID id;
        private UUID shopId;
        private String shopName;
        private OrderStatus status;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private List<OrderLine> items;

        public SubOrderItem(UUID id, UUID shopId, String shopName, OrderStatus status,
                BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount,
                List<OrderLine> items) {
            this.id = id; this.shopId = shopId; this.shopName = shopName; this.status = status;
            this.subtotal = subtotal; this.discountAmount = discountAmount;
            this.totalAmount = totalAmount; this.items = items;
        }
        public UUID getId() { return id; }
        public UUID getShopId() { return shopId; }
        public String getShopName() { return shopName; }
        public OrderStatus getStatus() { return status; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public List<OrderLine> getItems() { return items; }
    }

    public static class OrderLine {
        private UUID id;
        private UUID variantId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;

        public OrderLine(UUID id, UUID variantId, String productName, BigDecimal price,
                Integer quantity, BigDecimal discountAmount, BigDecimal totalAmount) {
            this.id = id; this.variantId = variantId; this.productName = productName;
            this.price = price; this.quantity = quantity; this.discountAmount = discountAmount;
            this.totalAmount = totalAmount;
        }
        public UUID getId() { return id; }
        public UUID getVariantId() { return variantId; }
        public String getProductName() { return productName; }
        public BigDecimal getPrice() { return price; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}
