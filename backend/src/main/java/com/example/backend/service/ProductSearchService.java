package com.example.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductSpecifications;

@Service
public class ProductSearchService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;

    public ProductSearchService(
            ProductRepository productRepository
    ) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(
            String keyword,
            int page,
            int size,
            String sort
    ) {
        List<String> tokens = ProductSpecifications.tokenize(keyword);

        String sortKey = normalizeSortKey(sort);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size),
                isPriceSort(sortKey) ? Sort.unsorted() : resolveSort(sortKey)
        );

        return productRepository.findAll(
                ProductSpecifications.searchByKeywords(tokens, sortKey),
                pageable
        );
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeSortKey(String sort) {
        String key = sort == null || sort.isBlank() ? "newest" : sort.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("newest", "price_asc", "price_desc", "name_asc").contains(key)) {
            throw new IllegalArgumentException("Invalid sort. Allowed: newest, price_asc, price_desc, name_asc");
        }
        return key;
    }

    private boolean isPriceSort(String sort) {
        return "price_asc".equals(sort) || "price_desc".equals(sort);
    }

    private Sort resolveSort(String key) {
        Sort primary = switch (key) {
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return primary.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}