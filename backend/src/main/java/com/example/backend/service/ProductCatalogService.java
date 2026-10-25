package com.example.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.response.ProductCardResponse;
import com.example.backend.dto.response.ProductDetailResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductMedia;
import com.example.backend.entity.ProductMediaType;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.repository.ProductMediaRepository;
import com.example.backend.repository.ProductOptionRepository;
import com.example.backend.repository.ProductOptionValueRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.ProductTagMappingRepository;
import com.example.backend.repository.ProductTagRepository;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.VariantOptionValueRepository;

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
    private final ProductTagMappingRepository productTagMappings;
    private final ProductTagRepository productTags;

    public ProductCatalogService(ProductRepository products, ProductMediaRepository media,
                                 ProductVariantRepository variants, InventoryRepository inventories,
                                 ProductOptionRepository options, ProductOptionValueRepository optionValues,
                                 VariantOptionValueRepository variantOptionValues, ShopRepository shops,
                                 ProductTagMappingRepository productTagMappings, ProductTagRepository productTags) {
        this.products=products; this.media=media; this.variants=variants; this.inventories=inventories;
        this.options=options; this.optionValues=optionValues; this.variantOptionValues=variantOptionValues; this.shops=shops;
        this.productTagMappings=productTagMappings; this.productTags=productTags;
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
            result.add(toCard(p, metrics));
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
        return detail(p);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse detailBySlug(String slug) {
        Product p = products.findBySlug(slug)
                .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        return detail(p);
    }

    private ProductDetailResponse detail(Product p) {
        UUID id = p.getId();
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
        List<ProductDetailResponse.Tag> tagResponse = productTagMappings.findAllByProductId(id).stream()
                .map(mapping -> productTags.findById(mapping.getTagId()).orElse(null))
                .filter(Objects::nonNull)
                .map(ProductDetailResponse.Tag::new)
                .toList();
        ProductMetrics metrics = metrics(id);
        return new ProductDetailResponse(p, new ProductDetailResponse.PublicShop(shop), mediaResponse,
                optionResponse, variantResponse, tagResponse, metrics.minPrice, metrics.maxPrice, metrics.stock);
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
            result.add(toCard(p, m));
            if (result.size() >= safeLimit) break;
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> relatedBySlug(String slug, int limit) {
        Product current = products.findBySlug(slug)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        return related(current.getId(), limit);
    }

    /** Maps a product returned by public search/filter endpoints to the same card contract as the catalog API. */
    @Transactional(readOnly = true)
    public ProductCardResponse toPublicCard(Product product) {
        return toCard(product, metrics(product.getId()));
    }

    private ProductCardResponse toCard(Product product, ProductMetrics metrics) {
        String thumbnail = product.getThumbnailUrl();
        if (thumbnail == null || thumbnail.isBlank()) {
            thumbnail = media.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                    .filter(m -> m.getSortOrder() != null && m.getSortOrder() == 1)
                    .filter(m -> m.getMediaType() == ProductMediaType.IMAGE)
                    .map(ProductMedia::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst().orElse(null);
        }
        return new ProductCardResponse(product, metrics.minPrice, metrics.maxPrice,
                metrics.representativeComparePrice, metrics.stock, thumbnail);
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
        BigDecimal min = null, max = null, representativeComparePrice = null;
        UUID representativeVariantId = null;
        int stock = 0;
        for (ProductVariant v : active) {
            if (v.getPrice() != null) {
                if (min == null || v.getPrice().compareTo(min) < 0
                        || (v.getPrice().compareTo(min) == 0 && representativeVariantId != null
                        && v.getId().compareTo(representativeVariantId) < 0)) {
                    min = v.getPrice();
                    representativeComparePrice = v.getComparePrice();
                    representativeVariantId = v.getId();
                }
                if (max == null || v.getPrice().compareTo(max) > 0) max=v.getPrice();
            }
            stock += availableStock(v.getId());
        }
        return new ProductMetrics(min,max,representativeComparePrice,stock);
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
    private record ProductMetrics(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal representativeComparePrice, Integer stock) {}
}
