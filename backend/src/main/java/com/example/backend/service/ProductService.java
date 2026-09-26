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
        product.setDescription(
                request.getDescription()
        );
        product.setPrice(request.getPrice());
        product.setStockQuantity(
                request.getStockQuantity()
        );

        product.setStatus(
                determineStatus(request.getStockQuantity())
        );

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

        if (request.getDescription() != null) {
            product.setDescription(
                    request.getDescription()
            );
        }

        if (request.getPrice() != null) {
            product.setPrice(
                    request.getPrice()
            );
        }

        if (request.getStockQuantity() != null) {

            int newStock = request.getStockQuantity();

            product.setStockQuantity(newStock);

            /*
             * Không tự động ACTIVE lại sản phẩm
             * đang INACTIVE.
             */
            if (product.getStatus() != ProductStatus.INACTIVE) {

                product.setStatus(
                        determineStatus(newStock)
                );
            }
        }

        /*
         * Shop Owner có thể chủ động thay đổi status.
         */
        if (request.getStatus() != null) {

            ProductStatus requestedStatus =
                    request.getStatus();

            /*
             * Không cho phép ACTIVE khi stock = 0.
             */
            if (requestedStatus == ProductStatus.ACTIVE
                    && product.getStockQuantity() != null
                    && product.getStockQuantity() == 0) {

                product.setStatus(
                        ProductStatus.OUT_OF_STOCK
                );

            } else {

                product.setStatus(
                        requestedStatus
                );
            }
        }

        /*
         * Đảm bảo trạng thái cuối cùng hợp lệ
         * với stock.
         */
        if (product.getStatus() == ProductStatus.ACTIVE
                && product.getStockQuantity() != null
                && product.getStockQuantity() == 0) {

            product.setStatus(
                    ProductStatus.OUT_OF_STOCK
            );
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

        if (request.getPrice() == null) {
            throw new IllegalArgumentException(
                    "Product price is required"
            );
        }

        if (request.getPrice().signum() < 0) {
            throw new IllegalArgumentException(
                    "Product price cannot be negative"
            );
        }

        if (request.getStockQuantity() == null) {
            throw new IllegalArgumentException(
                    "Stock quantity is required"
            );
        }

        if (request.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative"
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

        if (request.getPrice() != null
                && request.getPrice().signum() < 0) {

            throw new IllegalArgumentException(
                    "Product price cannot be negative"
            );
        }

        if (request.getStockQuantity() != null
                && request.getStockQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative"
            );
        }
    }

    // =========================
    // STATUS
    // =========================

    private ProductStatus determineStatus(
            Integer stockQuantity
    ) {

        if (stockQuantity == null || stockQuantity == 0) {
            return ProductStatus.OUT_OF_STOCK;
        }

        return ProductStatus.ACTIVE;
    }

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
                productRepository.existsByShopIdAndSlug(
                        shopId,
                        slug
                )
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
                productRepository
                        .existsByShopIdAndSlugAndIdNot(
                                shopId,
                                slug,
                                productId
                        )
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