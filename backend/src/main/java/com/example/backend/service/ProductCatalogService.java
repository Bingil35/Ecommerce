package com.example.backend.service;

import com.example.backend.dto.response.ProductCardResponse;
import com.example.backend.dto.response.ProductDetailResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** Read-only catalog business logic for both guests and customers. */
@Service
public class ProductCatalogService {
    private final ProductRepository products;
    private final ProductMediaRepository media;
    private final ProductVariantRepository variants;
    private final InventoryRepository inventories;
    private final ProductOptionRepository options;
    private final ProductOptionValueRepository optionValues;
    private final VariantOptionValueRepository variantOptionValues;
    private final ShopRepository shops;

    public ProductCatalogService(ProductRepository products, ProductMediaRepository media,
                                 ProductVariantRepository variants, InventoryRepository inventories,
                                 ProductOptionRepository options, ProductOptionValueRepository optionValues,
                                 VariantOptionValueRepository variantOptionValues, ShopRepository shops) {
        this.products=products; this.media=media; this.variants=variants; this.inventories=inventories;
        this.options=options; this.optionValues=optionValues; this.variantOptionValues=variantOptionValues; this.shops=shops;
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> list(UUID shopId, UUID categoryId, String keyword,
                                          BigDecimal minPrice, BigDecimal maxPrice, Boolean featured, String sortBy) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new IllegalArgumentException("minPrice không được lớn hơn maxPrice.");
        List<Product> source = categoryId != null
                ? products.findByCategoryIdAndStatusOrderByCreatedAtDesc(categoryId, ProductStatus.ACTIVE)
                : shopId != null
                ? products.findByShopIdAndStatusOrderByCreatedAtDesc(shopId, ProductStatus.ACTIVE)
                : products.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE);

        String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<ProductCardResponse> result = new ArrayList<>();
        for (Product p : source) {
            if (shopId != null && !shopId.equals(p.getShopId())) continue;
            if (categoryId != null && (p.getCategory() == null || !categoryId.equals(p.getCategory().getId()))) continue;
            if (!q.isBlank() && !contains(p.getName(), q) && !contains(p.getSlug(), q) && !contains(p.getDescription(), q)) continue;
            if (featured != null && !featured.equals(Boolean.TRUE.equals(p.getFeatured()))) continue;
            if (!isPublicShop(p.getShopId())) continue;
            ProductMetrics metrics = metrics(p.getId());
            if (minPrice != null && (metrics.maxPrice == null || metrics.maxPrice.compareTo(minPrice) < 0)) continue;
            if (maxPrice != null && (metrics.minPrice == null || metrics.minPrice.compareTo(maxPrice) > 0)) continue;
            result.add(new ProductCardResponse(p, metrics.minPrice, metrics.maxPrice, metrics.stock));
        }
        Comparator<ProductCardResponse> comparator = comparator(sortBy);
        if (comparator != null) result.sort(comparator);
        return result;
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse detail(UUID id) {
        Product p = products.findById(id)
                .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        Shop shop = shops.findById(p.getShopId())
                .filter(s -> s.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));

        List<ProductDetailResponse.Media> mediaResponse = media.findByProductIdOrderBySortOrderAsc(id)
                .stream().map(ProductDetailResponse.Media::new).toList();
        List<ProductDetailResponse.Option> optionResponse = options.findByProductIdOrderBySortOrderAsc(id).stream()
                .map(o -> new ProductDetailResponse.Option(o,
                        optionValues.findByProductOptionIdOrderBySortOrderAsc(o.getId()).stream()
                                .map(ProductDetailResponse.OptionValue::new).toList()))
                .toList();

        List<ProductDetailResponse.Variant> variantResponse = variants
                .findByProductIdAndStatusOrderByCreatedAtAsc(id, ProductStatus.ACTIVE).stream()
                .map(v -> {
                    List<ProductDetailResponse.SelectedOption> selected = variantOptionValues.findByVariantId(v.getId()).stream()
                            .map(link -> link.getOptionValue())
                            .filter(Objects::nonNull)
                            .map(ProductDetailResponse.SelectedOption::new).toList();
                    return new ProductDetailResponse.Variant(v, availableStock(v.getId()), selected);
                }).toList();
        ProductMetrics metrics = metrics(id);
        return new ProductDetailResponse(p, new ProductDetailResponse.PublicShop(shop), mediaResponse,
                optionResponse, variantResponse, metrics.minPrice, metrics.maxPrice, metrics.stock);
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> related(UUID id, int limit) {
        Product current = products.findById(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Product> candidates = current.getCategory() == null
                ? products.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE)
                : products.findByCategoryIdAndStatusOrderByCreatedAtDesc(current.getCategory().getId(), ProductStatus.ACTIVE);
        List<ProductCardResponse> result = new ArrayList<>();
        for (Product p : candidates) {
            if (p.getId().equals(id) || !isPublicShop(p.getShopId())) continue;
            ProductMetrics m = metrics(p.getId());
            result.add(new ProductCardResponse(p,m.minPrice,m.maxPrice,m.stock));
            if (result.size() >= safeLimit) break;
        }
        return result;
    }

    private boolean isPublicShop(UUID shopId) {
        return shopId != null && shops.findById(shopId).map(s -> s.getStatus() == ShopStatus.ACTIVE).orElse(false);
    }
    private boolean contains(String value, String q) { return value != null && value.toLowerCase(Locale.ROOT).contains(q); }
    private Integer availableStock(UUID variantId) {
        return inventories.findFirstByVariantId(variantId)
                .map(i -> Math.max(0, (i.getQuantity()==null?0:i.getQuantity()) - (i.getReservedQuantity()==null?0:i.getReservedQuantity())))
                .orElse(0);
    }
    private ProductMetrics metrics(UUID productId) {
        List<ProductVariant> active = variants.findByProductIdAndStatusOrderByCreatedAtAsc(productId, ProductStatus.ACTIVE);
        BigDecimal min = null, max = null; int stock = 0;
        for (ProductVariant v : active) {
            if (v.getPrice() != null) {
                if (min == null || v.getPrice().compareTo(min) < 0) min=v.getPrice();
                if (max == null || v.getPrice().compareTo(max) > 0) max=v.getPrice();
            }
            stock += availableStock(v.getId());
        }
        return new ProductMetrics(min,max,stock);
    }
    private Comparator<ProductCardResponse> comparator(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || sortBy.equalsIgnoreCase("newest")) return null; // repository defaults newest first
        return switch (sortBy.toLowerCase(Locale.ROOT)) {
            case "price_asc" -> Comparator.comparing(ProductCardResponse::getMinPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "price_desc" -> Comparator.comparing(ProductCardResponse::getMaxPrice, Comparator.nullsFirst(BigDecimal::compareTo)).reversed();
            case "rating_desc" -> Comparator.comparing(ProductCardResponse::getRatingAverage, Comparator.nullsFirst(BigDecimal::compareTo)).reversed();
            case "name_asc" -> Comparator.comparing(ProductCardResponse::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> throw new IllegalArgumentException("sortBy chỉ nhận newest, price_asc, price_desc, rating_desc hoặc name_asc.");
        };
    }
    private record ProductMetrics(BigDecimal minPrice, BigDecimal maxPrice, Integer stock) {}
}
