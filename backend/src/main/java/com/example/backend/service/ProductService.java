package com.example.backend.service;

import com.example.backend.dto.request.CreateProductRequest;
import com.example.backend.dto.request.UpdateProductRequest;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // =========================
    // CREATE
    // =========================

    @Transactional
    public Product createProduct(CreateProductRequest request) {

        validateCreateRequest(request);

        Category category = categoryRepository.findById(
                request.getCategoryId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Category not found"
                )
        );

        String slug = generateUniqueSlug(
                request.getShopId(),
                request.getName()
        );

        Product product = new Product();

        product.setShopId(request.getShopId());
        product.setCategory(category);
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setBrandId(request.getBrandId());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setFeatured(request.getFeatured());
        product.setStatus(ProductStatus.ACTIVE);

        return productRepository.save(product);
    }

    // =========================
    // GET ALL BY SHOP
    // =========================

    public List<Product> getProductsByShop(UUID shopId) {

        if (shopId == null) {
            throw new IllegalArgumentException(
                    "Shop ID is required"
            );
        }

        return productRepository
                .findByShopIdOrderByCreatedAtDesc(shopId);
    }

    // =========================
    // GET BY SHOP + STATUS
    // =========================

    public List<Product> getProductsByShopAndStatus(
            UUID shopId,
            ProductStatus status
    ) {

        if (shopId == null) {
            throw new IllegalArgumentException(
                    "Shop ID is required"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Product status is required"
            );
        }

        return productRepository
                .findByShopIdAndStatusOrderByCreatedAtDesc(
                        shopId,
                        status
                );
    }

    // =========================
    // PUBLIC CATALOG (ACTIVE ONLY)
    // =========================
    public List<Product> getActiveProducts() {
        return productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE);
    }

    public List<Product> getActiveProductsByShop(UUID shopId) {
        return productRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shopId, ProductStatus.ACTIVE);
    }

    public List<Product> getActiveProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryIdAndStatusOrderByCreatedAtDesc(categoryId, ProductStatus.ACTIVE);
    }

    public Product getActiveProductById(UUID id) {
        Product product = getProductById(id);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm.");
        }
        return product;
    }

    // =========================
    // GET BY ID
    // =========================

    public Product getProductById(UUID id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
    }

    // =========================
    // UPDATE
    // =========================

    @Transactional
    public Product updateProduct(
            UUID id,
            UpdateProductRequest request
    ) {

        Product product = getProductById(id);

        validateUpdateRequest(request);

        if (request.getCategoryId() != null) {

            Category category = categoryRepository.findById(
                    request.getCategoryId()
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Category not found"
                    )
            );

            product.setCategory(category);
        }

        if (request.getName() != null
                && !request.getName().isBlank()) {

            String newName = request.getName().trim();

            /*
             * Chỉ tạo slug mới khi tên sản phẩm thay đổi.
             */
            if (!newName.equals(product.getName())) {

                String newSlug = generateUniqueSlugForUpdate(
                        product.getShopId(),
                        newName,
                        product.getId()
                );

                product.setName(newName);
                product.setSlug(newSlug);
            }
        }

        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBrandId() != null) product.setBrandId(request.getBrandId());
        if (request.getThumbnailUrl() != null) product.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());

        /*
         * Shop Owner có thể chủ động thay đổi status.
         */
        if (request.getStatus() != null) {

            ProductStatus requestedStatus =
                    request.getStatus();

            product.setStatus(requestedStatus);
        }

        return productRepository.save(product);
    }

    // =========================
    // DELETE
    // =========================

    @Transactional
    public void deleteProduct(UUID id) {

        Product product = getProductById(id);

        /*
         * Hiện tại chưa kiểm tra Order/OrderDetail
         * vì Order Management chưa được triển khai.
         *
         * Khi triển khai Order Management sẽ bổ sung
         * business rule kiểm tra lịch sử/đơn hàng đang xử lý.
         */

        productRepository.delete(product);
    }

    // =========================
    // VALIDATION
    // =========================

    private void validateCreateRequest(
            CreateProductRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Product data is required"
            );
        }

        if (request.getShopId() == null) {
            throw new IllegalArgumentException(
                    "Shop ID is required"
            );
        }

        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException(
                    "Category ID is required"
            );
        }

        if (request.getName() == null
                || request.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }

    }

    private void validateUpdateRequest(
            UpdateProductRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Product data is required"
            );
        }

        if (request.getName() != null
                && request.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Product name cannot be blank"
            );
        }

    }

    // =========================
    // STATUS
    // =========================

    // =========================
    // SLUG
    // =========================

    private String generateUniqueSlug(
            UUID shopId,
            String name
    ) {

        String baseSlug = generateSlug(name);

        String slug = baseSlug;

        int counter = 2;

        while (
                productRepository.existsBySlug(slug)
        ) {

            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String generateUniqueSlugForUpdate(
            UUID shopId,
            String name,
            UUID productId
    ) {

        String baseSlug = generateSlug(name);

        String slug = baseSlug;

        int counter = 2;

        while (
                productRepository.existsBySlugAndIdNot(slug, productId)
        ) {

            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String generateSlug(String text) {

        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        String withoutAccents = normalized.replaceAll(
                "\\p{InCombiningDiacriticalMarks}+",
                ""
        );

        String slug = withoutAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        /*
         * Trường hợp tên chỉ chứa ký tự đặc biệt.
         */
        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "Product name must contain valid characters"
            );
        }

        return slug;
    }
}