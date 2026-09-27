package com.example.backend.service;

import com.example.backend.dto.request.CreateProductOptionRequest;
import com.example.backend.dto.request.CreateProductOptionValueRequest;
import com.example.backend.dto.request.CreateProductVariantRequest;
import com.example.backend.dto.request.UpdateProductOptionRequest;
import com.example.backend.dto.request.UpdateProductOptionValueRequest;
import com.example.backend.dto.request.UpdateProductVariantRequest;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductOption;
import com.example.backend.entity.ProductOptionValue;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.ProductVariantStatus;
import com.example.backend.entity.VariantOptionValue;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductOptionRepository;
import com.example.backend.repository.ProductOptionValueRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.VariantOptionValueRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;

    public ProductVariantService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductOptionRepository productOptionRepository,
            ProductOptionValueRepository productOptionValueRepository,
            VariantOptionValueRepository variantOptionValueRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productOptionRepository = productOptionRepository;
        this.productOptionValueRepository = productOptionValueRepository;
        this.variantOptionValueRepository =
                variantOptionValueRepository;
    }

    // =====================================================
    // PRODUCT OPTION
    // =====================================================

    @Transactional
    public ProductOption createOption(
            UUID productId,
            CreateProductOptionRequest request
    ) {

        Product product = getProduct(productId);

        validateOptionName(request.getName());

        String name = request.getName().trim();

        if (productOptionRepository.existsByProductIdAndName(
                productId,
                name
        )) {
            throw new IllegalArgumentException(
                    "Product option already exists"
            );
        }

        ProductOption option = new ProductOption();

        option.setProduct(product);
        option.setName(name);

        return productOptionRepository.save(option);
    }

    public List<ProductOption> getOptions(
            UUID productId
    ) {

        getProduct(productId);

        return productOptionRepository
                .findByProductIdOrderByNameAsc(productId);
    }

    public ProductOption getOptionById(
            UUID optionId
    ) {

        return productOptionRepository.findById(optionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product option not found"
                        )
                );
    }

    @Transactional
    public ProductOption updateOption(
            UUID optionId,
            UpdateProductOptionRequest request
    ) {

        ProductOption option =
                getOptionById(optionId);

        validateOptionName(request.getName());

        String name = request.getName().trim();

        UUID productId =
                option.getProduct().getId();

        if (productOptionRepository
                .existsByProductIdAndNameAndIdNot(
                        productId,
                        name,
                        optionId
                )) {

            throw new IllegalArgumentException(
                    "Product option already exists"
            );
        }

        option.setName(name);

        return productOptionRepository.save(option);
    }

    @Transactional
    public void deleteOption(
            UUID optionId
    ) {

        ProductOption option =
                getOptionById(optionId);

        /*
         * Các ProductOptionValue thuộc option này
         * sẽ được xóa bởi ON DELETE CASCADE ở DB.
         *
         * Các VariantOptionValue liên quan cũng được
         * xóa bởi ON DELETE CASCADE.
         */

        productOptionRepository.delete(option);
    }

    // =====================================================
    // PRODUCT OPTION VALUE
    // =====================================================

    @Transactional
    public ProductOptionValue createOptionValue(
            UUID optionId,
            CreateProductOptionValueRequest request
    ) {

        ProductOption option =
                getOptionById(optionId);

        validateOptionValue(request.getValue());

        String value =
                request.getValue().trim();

        if (productOptionValueRepository
                .existsByOptionIdAndValue(
                        optionId,
                        value
                )) {

            throw new IllegalArgumentException(
                    "Product option value already exists"
            );
        }

        ProductOptionValue optionValue =
                new ProductOptionValue();

        optionValue.setOption(option);
        optionValue.setValue(value);

        return productOptionValueRepository.save(
                optionValue
        );
    }

    public List<ProductOptionValue> getOptionValues(
            UUID optionId
    ) {

        getOptionById(optionId);

        return productOptionValueRepository
                .findByOptionIdOrderByValueAsc(optionId);
    }

    public ProductOptionValue getOptionValueById(
            UUID optionValueId
    ) {

        return productOptionValueRepository
                .findById(optionValueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product option value not found"
                        )
                );
    }

    @Transactional
    public ProductOptionValue updateOptionValue(
            UUID optionValueId,
            UpdateProductOptionValueRequest request
    ) {

        ProductOptionValue optionValue =
                getOptionValueById(optionValueId);

        validateOptionValue(request.getValue());

        String value =
                request.getValue().trim();

        UUID optionId =
                optionValue.getOption().getId();

        if (productOptionValueRepository
                .existsByOptionIdAndValueAndIdNot(
                        optionId,
                        value,
                        optionValueId
                )) {

            throw new IllegalArgumentException(
                    "Product option value already exists"
            );
        }

        optionValue.setValue(value);

        return productOptionValueRepository.save(
                optionValue
        );
    }

    @Transactional
    public void deleteOptionValue(
            UUID optionValueId
    ) {

        ProductOptionValue optionValue =
                getOptionValueById(optionValueId);

        /*
         * Các VariantOptionValue liên quan sẽ được
         * xóa bởi ON DELETE CASCADE.
         */

        productOptionValueRepository.delete(optionValue);
    }

    // =====================================================
    // PRODUCT VARIANT
    // =====================================================

    @Transactional
    public ProductVariant createVariant(
            CreateProductVariantRequest request
    ) {

        validateCreateVariantRequest(request);

        Product product =
                getProduct(request.getProductId());

        String sku =
                request.getSku().trim();

        if (productVariantRepository
                .existsByProductIdAndSku(
                        product.getId(),
                        sku
                )) {

            throw new IllegalArgumentException(
                    "SKU already exists for this product"
            );
        }

        List<ProductOptionValue> optionValues =
                getAndValidateOptionValues(
                        product,
                        request.getOptionValueIds()
                );

        validateNoDuplicateOptions(optionValues);

        ProductVariant variant =
                new ProductVariant();

        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(request.getPrice());
        variant.setStockQuantity(
                request.getStockQuantity()
        );
        variant.setImageUrl(
                request.getImageUrl()
        );

        variant.setStatus(
                determineVariantStatus(
                        request.getStockQuantity()
                )
        );

        ProductVariant savedVariant =
                productVariantRepository.save(variant);

        saveVariantOptionValues(
                savedVariant,
                optionValues
        );

        return savedVariant;
    }

    public List<ProductVariant> getVariants(
            UUID productId
    ) {

        getProduct(productId);

        return productVariantRepository
                .findByProductIdOrderByCreatedAtAsc(
                        productId
                );
    }

    public List<ProductVariant> getVariantsByStatus(
            UUID productId,
            ProductVariantStatus status
    ) {

        getProduct(productId);

        if (status == null) {
            throw new IllegalArgumentException(
                    "Variant status is required"
            );
        }

        return productVariantRepository
                .findByProductIdAndStatusOrderByCreatedAtAsc(
                        productId,
                        status
                );
    }

    public ProductVariant getVariantById(
            UUID variantId
    ) {

        return productVariantRepository
                .findById(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product variant not found"
                        )
                );
    }

    public List<VariantOptionValue> getVariantOptionValues(
            UUID variantId
    ) {

        getVariantById(variantId);

        return variantOptionValueRepository
                .findByVariantId(variantId);
    }

    @Transactional
    public ProductVariant updateVariant(
            UUID variantId,
            UpdateProductVariantRequest request
    ) {

        ProductVariant variant =
                getVariantById(variantId);

        validateUpdateVariantRequest(request);

        Product product =
                variant.getProduct();

        if (request.getSku() != null) {

            String sku =
                    request.getSku().trim();

            if (productVariantRepository
                    .existsByProductIdAndSkuAndIdNot(
                            product.getId(),
                            sku,
                            variantId
                    )) {

                throw new IllegalArgumentException(
                        "SKU already exists for this product"
                );
            }

            variant.setSku(sku);
        }

        if (request.getPrice() != null) {
            variant.setPrice(
                    request.getPrice()
            );
        }

        if (request.getStockQuantity() != null) {

            variant.setStockQuantity(
                    request.getStockQuantity()
            );

            /*
             * Stock = 0 → OUT_OF_STOCK tương ứng
             * với Product Variant.
             *
             * Nếu variant đang INACTIVE thì không
             * tự động ACTIVE lại.
             */
            if (variant.getStatus()
                    != ProductVariantStatus.INACTIVE) {

                variant.setStatus(
                        determineVariantStatus(
                                request.getStockQuantity()
                        )
                );
            }
        }

        if (request.getImageUrl() != null) {
            variant.setImageUrl(
                    request.getImageUrl()
            );
        }

        if (request.getStatus() != null) {

            variant.setStatus(
                    request.getStatus()
            );
        }

        if (request.getOptionValueIds() != null) {

            List<ProductOptionValue> optionValues =
                    getAndValidateOptionValues(
                            product,
                            request.getOptionValueIds()
                    );

            validateNoDuplicateOptions(optionValues);

            /*
             * Xóa toàn bộ liên kết cũ.
             */
            variantOptionValueRepository
                    .deleteByVariantId(variantId);

            /*
             * Tạo lại liên kết mới.
             */
            saveVariantOptionValues(
                    variant,
                    optionValues
            );
        }

        return productVariantRepository.save(variant);
    }

    @Transactional
    public void deleteVariant(
            UUID variantId
    ) {

        ProductVariant variant =
                getVariantById(variantId);

        /*
         * Xóa các liên kết variant-option trước.
         */
        variantOptionValueRepository
                .deleteByVariantId(variantId);

        productVariantRepository.delete(variant);
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private void validateCreateVariantRequest(
            CreateProductVariantRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Variant data is required"
            );
        }

        if (request.getProductId() == null) {
            throw new IllegalArgumentException(
                    "Product ID is required"
            );
        }

        if (request.getSku() == null
                || request.getSku().isBlank()) {

            throw new IllegalArgumentException(
                    "SKU is required"
            );
        }

        if (request.getPrice() == null) {
            throw new IllegalArgumentException(
                    "Variant price is required"
            );
        }

        if (request.getPrice().signum() < 0) {
            throw new IllegalArgumentException(
                    "Variant price cannot be negative"
            );
        }

        if (request.getStockQuantity() == null) {
            throw new IllegalArgumentException(
                    "Variant stock quantity is required"
            );
        }

        if (request.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Variant stock quantity cannot be negative"
            );
        }
    }

    private void validateUpdateVariantRequest(
            UpdateProductVariantRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Variant data is required"
            );
        }

        if (request.getSku() != null
                && request.getSku().isBlank()) {

            throw new IllegalArgumentException(
                    "SKU cannot be blank"
            );
        }

        if (request.getPrice() != null
                && request.getPrice().signum() < 0) {

            throw new IllegalArgumentException(
                    "Variant price cannot be negative"
            );
        }

        if (request.getStockQuantity() != null
                && request.getStockQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Variant stock quantity cannot be negative"
            );
        }
    }

    private void validateOptionName(
            String name
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Option name is required"
            );
        }
    }

    private void validateOptionValue(
            String value
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Option value is required"
            );
        }
    }

    // =====================================================
    // OPTION VALUE VALIDATION
    // =====================================================

    private List<ProductOptionValue>
    getAndValidateOptionValues(
            Product product,
            List<UUID> optionValueIds
    ) {

        if (optionValueIds == null
                || optionValueIds.isEmpty()) {

            return new ArrayList<>();
        }

        /*
         * Không cho phép cùng một optionValue xuất hiện
         * nhiều lần trong request.
         */
        Set<UUID> uniqueIds =
                new HashSet<>(optionValueIds);

        if (uniqueIds.size()
                != optionValueIds.size()) {

            throw new IllegalArgumentException(
                    "Duplicate option values are not allowed"
            );
        }

        List<ProductOptionValue> optionValues =
                new ArrayList<>();

        for (UUID optionValueId : optionValueIds) {

            ProductOptionValue optionValue =
                    getOptionValueById(
                            optionValueId
                    );

            /*
             * Đây là validation quan trọng:
             *
             * Option Value phải thuộc Option
             * của chính Product đang tạo Variant.
             */
            if (!optionValue
                    .getOption()
                    .getProduct()
                    .getId()
                    .equals(product.getId())) {

                throw new IllegalArgumentException(
                        "Option value does not belong to this product"
                );
            }

            optionValues.add(optionValue);
        }

        return optionValues;
    }

    private void validateNoDuplicateOptions(
            List<ProductOptionValue> optionValues
    ) {

        Set<UUID> optionIds =
                new HashSet<>();

        for (ProductOptionValue optionValue :
                optionValues) {

            UUID optionId =
                    optionValue
                            .getOption()
                            .getId();

            /*
             * Một Variant chỉ được chọn một value
             * cho mỗi Option.
             *
             * Ví dụ:
             *
             * Màu sắc = Đỏ
             * Màu sắc = Xanh
             *
             * là không hợp lệ.
             */
            if (!optionIds.add(optionId)) {

                throw new IllegalArgumentException(
                        "A variant cannot contain multiple values from the same option"
                );
            }
        }
    }

    // =====================================================
    // SAVE RELATION
    // =====================================================

    private void saveVariantOptionValues(
            ProductVariant variant,
            List<ProductOptionValue> optionValues
    ) {

        for (ProductOptionValue optionValue :
                optionValues) {

            VariantOptionValue relation =
                    new VariantOptionValue();

            relation.setVariant(variant);
            relation.setOptionValue(optionValue);

            /*
             * Composite ID cần được thiết lập
             * sau khi Variant đã có ID.
             */
            relation.setId(
                    new com.example.backend.entity.VariantOptionValueId(
                            variant.getId(),
                            optionValue.getId()
                    )
            );

            variantOptionValueRepository.save(
                    relation
            );
        }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private Product getProduct(
            UUID productId
    ) {

        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
    }

    private ProductVariantStatus determineVariantStatus(
            Integer stockQuantity
    ) {
        return stockQuantity == 0
                ? ProductVariantStatus.OUT_OF_STOCK
                : ProductVariantStatus.ACTIVE;
    }
}