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
import com.example.backend.entity.ProductMedia;
import com.example.backend.entity.ProductVariantStatus;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.Inventory;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.entity.VariantOptionValue;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductOptionRepository;
import com.example.backend.repository.ProductOptionValueRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductMediaRepository;
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
    private final InventoryRepository inventoryRepository;
    private final ProductMediaRepository productMediaRepository;
    private final CartService cartService;

    public ProductVariantService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductOptionRepository productOptionRepository,
            ProductOptionValueRepository productOptionValueRepository,
            VariantOptionValueRepository variantOptionValueRepository,
            InventoryRepository inventoryRepository,
            ProductMediaRepository productMediaRepository,
            CartService cartService
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productOptionRepository = productOptionRepository;
        this.productOptionValueRepository = productOptionValueRepository;
        this.variantOptionValueRepository =
                variantOptionValueRepository;
        this.inventoryRepository = inventoryRepository;
        this.productMediaRepository = productMediaRepository;
        this.cartService = cartService;
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
                .existsByProductOptionIdAndValue(
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
                .findByProductOptionIdOrderBySortOrderAsc(optionId);
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
                optionValue.getProductOption().getId();

        if (productOptionValueRepository
                .existsByProductOptionIdAndValueAndIdNot(
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
        variant.setComparePrice(request.getComparePrice());
        variant.setCostPrice(request.getCostPrice());
        if (request.getPrimaryMediaId() != null) {
            ProductMedia primaryMedia = productMediaRepository.findById(request.getPrimaryMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Primary media not found"));
            if (!primaryMedia.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Primary media must belong to the same product");
            }
            variant.setPrimaryMedia(primaryMedia);
        }
        variant.setStatus(ProductStatus.ACTIVE);

        ProductVariant savedVariant =
                productVariantRepository.save(variant);
        Inventory inventory = new Inventory();
        inventory.setVariant(savedVariant);
        inventory.setQuantity(request.getStockQuantity());
        inventory.setReservedQuantity(0);
        inventory.setSoldQuantity(0);
        inventoryRepository.save(inventory);
        savedVariant.setStockQuantity(inventory.getQuantity());

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

        List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByCreatedAtAsc(productId);
        variants.forEach(v -> inventoryRepository.findFirstByVariantId(v.getId())
                .ifPresent(i -> v.setStockQuantity(i.getQuantity())));
        return variants;
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

        List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndStatusOrderByCreatedAtAsc(productId, ProductStatus.valueOf(status.name()));
        variants.forEach(v -> inventoryRepository.findFirstByVariantId(v.getId())
                .ifPresent(i -> v.setStockQuantity(i.getQuantity())));
        return variants;
    }

    public ProductVariant getVariantById(
            UUID variantId
    ) {

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        inventoryRepository.findFirstByVariantId(variantId)
                .ifPresent(i -> variant.setStockQuantity(i.getQuantity()));
        return variant;
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

        if (request.getPrice() != null) variant.setPrice(request.getPrice());
        if (request.getComparePrice() != null) variant.setComparePrice(request.getComparePrice());
        if (request.getCostPrice() != null) variant.setCostPrice(request.getCostPrice());
        if (request.getPrimaryMediaId() != null) {
            ProductMedia primaryMedia = productMediaRepository.findById(request.getPrimaryMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Primary media not found"));
            if (!primaryMedia.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Primary media must belong to the same product");
            }
            variant.setPrimaryMedia(primaryMedia);
        }

        if (request.getStockQuantity() != null) {

            Inventory inventory = inventoryRepository.findFirstByVariantId(variantId)
                    .orElseGet(() -> { Inventory created = new Inventory(); created.setVariant(variant); created.setReservedQuantity(0); created.setSoldQuantity(0); return created; });
            inventory.setQuantity(request.getStockQuantity());
            inventoryRepository.save(inventory);
            variant.setStockQuantity(inventory.getQuantity());
        }

        if (request.getStatus() != null) {
            variant.setStatus(ProductStatus.valueOf(request.getStatus().name()));
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
        variantOptionValueRepository.deleteByVariantId(variantId);
        /*
         * Xóa các dòng giỏ hàng tham chiếu variant này, nếu không sẽ vi phạm khóa ngoại.
         * Giỏ hàng không phải dữ liệu đơn hàng, nên việc xóa này là an toàn.
         */
        cartService.removeVariantFromAllCarts(variantId);
        inventoryRepository.deleteByVariantId(variantId);
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

        if ((request.getPrice() != null && request.getPrice().signum() < 0)
                || (request.getComparePrice() != null && request.getComparePrice().signum() < 0)
                || (request.getCostPrice() != null && request.getCostPrice().signum() < 0)) {

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
                    .getProductOption()
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
                            .getProductOption()
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

        for (ProductOptionValue optionValue : optionValues) {
            VariantOptionValue relation = new VariantOptionValue();
            relation.setVariant(variant);
            relation.setOptionValue(optionValue);
            variantOptionValueRepository.save(relation);
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
        return ProductVariantStatus.ACTIVE;
    }
}