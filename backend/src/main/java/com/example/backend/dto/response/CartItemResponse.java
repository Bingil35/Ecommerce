package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One cart line. The shop is given by the enclosing SubCartResponse.
 *
 * A line is never dropped because it can no longer be bought: when the product, variant
 * or shop is inactive, or free stock is below the quantity, the line is still returned
 * with available = false and is left out of every total.
 */
public class CartItemResponse {

    /** Cart line ID (cartdetails.id). Use it in PATCH and DELETE. */
    private UUID id;
    private UUID productId;
    /** Slug of the product, for linking to the product page. */
    private String productSlug;
    private String productName;
    private UUID variantId;
    /** Option name and value in option order, e.g. "Màu: Đen, Size: L". Null when the variant has no linked option values. */
    private String variantName;
    private List<ProductDetailResponse.SelectedOption> selectedOptions;
    private String sku;
    /** Variant image, or the product thumbnail when the variant has none. */
    private String imageUrl;
    /** Current variant price read from the database. Never taken from the client. */
    private BigDecimal unitPrice;
    /** Original (list) price of the variant, for a struck-through display. Null when not set. */
    private BigDecimal comparePrice;
    private Integer quantity;
    /** unitPrice * quantity, scale 2. Null when the variant has no price. */
    private BigDecimal lineSubtotal;
    /** Free stock = max(0, quantity - reservedQuantity), same formula as the catalog. */
    private Integer availableStock;
    /** False when the product, variant or shop is not purchasable, or stock is below quantity. */
    private Boolean available;

    public CartItemResponse(
            UUID id,
            UUID productId,
            String productSlug,
            String productName,
            UUID variantId,
            String variantName,
            List<ProductDetailResponse.SelectedOption> selectedOptions,
            String sku,
            String imageUrl,
            BigDecimal unitPrice,
            BigDecimal comparePrice,
            Integer quantity,
            BigDecimal lineSubtotal,
            Integer availableStock,
            Boolean available
    ) {
        this.id = id;
        this.productId = productId;
        this.productSlug = productSlug;
        this.productName = productName;
        this.variantId = variantId;
        this.variantName = variantName;
        this.selectedOptions = selectedOptions;
        this.sku = sku;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.comparePrice = comparePrice;
        this.quantity = quantity;
        this.lineSubtotal = lineSubtotal;
        this.availableStock = availableStock;
        this.available = available;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductSlug() { return productSlug; }
    public String getProductName() { return productName; }
    public UUID getVariantId() { return variantId; }
    public String getVariantName() { return variantName; }
    public List<ProductDetailResponse.SelectedOption> getSelectedOptions() { return selectedOptions; }
    public String getSku() { return sku; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getComparePrice() { return comparePrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineSubtotal() { return lineSubtotal; }
    public Integer getAvailableStock() { return availableStock; }
    public Boolean getAvailable() { return available; }
}
