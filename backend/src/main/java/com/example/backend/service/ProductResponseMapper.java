package com.example.backend.service;

import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.dto.response.ProductSummary;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.repository.CategoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dựng ProductResponse cho shop owner, kèm tóm tắt giá/tồn kho và tên danh mục.
 * Dữ liệu phụ được nạp theo lô (không N+1).
 */
@Component
public class ProductResponseMapper {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    public ProductResponseMapper(
            ProductService productService,
            CategoryRepository categoryRepository
    ) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }

    private record Context(
            Map<UUID, ProductSummary> summaries,
            Map<UUID, String> categoryNames
    ) {
    }

    public ProductResponse toResponse(Product product) {
        return toResponses(List.of(product)).get(0);
    }

    public List<ProductResponse> toResponses(List<Product> products) {
        Context context = load(products);
        return products.stream().map(p -> build(p, context)).toList();
    }

    public PageResponse<ProductResponse> toPage(Page<Product> page) {
        Context context = load(page.getContent());
        return PageResponse.from(page, p -> build(p, context));
    }

    /** Tên danh mục theo id, dùng cho xuất CSV. */
    public Map<UUID, String> categoryNames(Collection<Product> products) {

        Set<UUID> categoryIds = new HashSet<>();

        for (Product product : products) {
            if (product.getCategory() != null) {
                categoryIds.add(product.getCategory().getId());
            }
        }

        Map<UUID, String> names = new HashMap<>();

        if (!categoryIds.isEmpty()) {
            for (Category category : categoryRepository.findAllById(categoryIds)) {
                names.put(category.getId(), category.getName());
            }
        }

        return names;
    }

    private Context load(List<Product> products) {
        return new Context(
                productService.getSummaries(
                        products.stream().map(Product::getId).toList()
                ),
                categoryNames(products)
        );
    }

    private ProductResponse build(Product product, Context context) {

        String categoryName = product.getCategory() == null
                ? null
                : context.categoryNames().get(product.getCategory().getId());

        return new ProductResponse(
                product,
                context.summaries().getOrDefault(product.getId(), ProductSummary.EMPTY),
                categoryName
        );
    }
}
