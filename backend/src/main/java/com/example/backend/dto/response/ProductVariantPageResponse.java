package com.example.backend.dto.response;

import com.example.backend.repository.ProductVariantRepository.VariantPriceSummary;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/** Một trang biến thể kèm số liệu tổng hợp của toàn bộ biến thể trong sản phẩm. */
public class ProductVariantPageResponse {

    private List<ProductVariantResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minComparePrice;
    private BigDecimal maxComparePrice;

    public ProductVariantPageResponse() {
    }

    public ProductVariantPageResponse(
            Page<?> page,
            List<ProductVariantResponse> items,
            VariantPriceSummary summary
    ) {
        this.items = items;
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
        if (summary != null) {
            this.minPrice = summary.getMinPrice();
            this.maxPrice = summary.getMaxPrice();
            this.minComparePrice = summary.getMinComparePrice();
            this.maxComparePrice = summary.getMaxComparePrice();
        }
    }

    public List<ProductVariantResponse> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLast() { return last; }
    public BigDecimal getMinPrice() { return minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public BigDecimal getMinComparePrice() { return minComparePrice; }
    public BigDecimal getMaxComparePrice() { return maxComparePrice; }
}