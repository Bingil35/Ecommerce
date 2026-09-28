package com.example.backend.service;

import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size),
                resolveSort(sort)
        );

        return productRepository.findAll(
                ProductSpecifications.searchByKeywords(tokens),
                pageable
        );
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Sort resolveSort(String sort) {

        String key = sort == null ? "newest" : sort.trim().toLowerCase();

        Sort primary = switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new IllegalArgumentException(
                    "Invalid sort. Allowed: newest, price_asc, "
                            + "price_desc, name_asc"
            );
        };

        // Thứ tự phụ để phân trang ổn định.
        return primary.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}