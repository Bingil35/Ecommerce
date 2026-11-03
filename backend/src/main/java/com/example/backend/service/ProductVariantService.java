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

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ProductVariantService.class);

    private static String currentActor() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMediaRepository productMediaRepository;
    private final CartService cartService;
    private final ProductApprovalPolicy approvalPolicy;
    private final ProductHistoryGuard historyGuard;

    public ProductVariantService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductOptionRepository productOptionRepository,
            ProductOptionValueRepository productOptionValueRepository,
            VariantOptionValueRepository variantOptionValueRepository,
            InventoryRepository inventoryRepository,
            ProductMediaRepository productMediaRepository,
            CartService cartService,
            ProductApprovalPolicy approvalPolicy,
            ProductHistoryGuard historyGuard
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
        this.approvalPolicy = approvalPolicy;
        this.historyGuard = historyGuard;
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

        ProductOption saved = productOptionRepository.save(option);

        // Thay đổi option là thay đổi nội dung cần duyệt lại.
        approvalPolicy.markContentChanged(product);

        return saved;
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

        boolean nameChanged = !name.equals(option.getName());

        option.setName(name);

        ProductOption saved = productOptionRepository.save(option);

        if (nameChanged) {
            approvalPolicy.markContentChanged(option.getProduct());
        }

        return saved;
    }

    @Transactional
    public void deleteOption(
            UUID optionId
    ) {

        ProductOption option =
                getOptionById(optionId);

        List<ProductOptionValue> optionValues =
                productOptionValueRepository
                        .findByProductOptionIdOrderBySortOrderAsc(optionId);

        int usedBy = 0;

        for (ProductOptionValue value : optionValues) {
            usedBy += variantOptionValueRepository
                    .findByOptionValueId(value.getId())
                    .size();
        }

        if (usedBy > 0) {
            throw new IllegalArgumentException(
                    "Thuộc tính này đang được dùng bởi " + usedBy
                            + " biến thể. Hãy sửa hoặc xóa các biến thể đó trước."
            );
        }

        /*
         * Schema KHÔNG có ON DELETE CASCADE: phải xóa các ProductOptionValue
         * (đã chắc chắn không còn biến thể nào dùng) trước khi xóa option.
         */
        productOptionValueRepository.deleteAll(optionValues);
        productOptionValueRepository.flush();

        Product ownerProduct = option.getProduct();

        productOptionRepository.delete(option);

        approvalPolicy.markContentChanged(ownerProduct);
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

        ProductOptionValue saved = productOptionValueRepository.save(
                optionValue
        );

        approvalPolicy.markContentChanged(option.getProduct());

        return saved;
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

        boolean valueChanged = !value.equals(optionValue.getValue());

        optionValue.setValue(value);

        ProductOptionValue saved = productOptionValueRepository.save(
                optionValue
        );

        if (valueChanged) {
            approvalPolicy.markContentChanged(
                    optionValue.getProductOption().getProduct()
            );
        }

        return saved;
    }

    @Transactional
    public void deleteOptionValue(
            UUID optionValueId
    ) {

        ProductOptionValue optionValue =
                getOptionValueById(optionValueId);

        int usedBy = variantOptionValueRepository
                .findByOptionValueId(optionValueId)
                .size();

        if (usedBy > 0) {
            throw new IllegalArgumentException(
                    "Giá trị này đang được dùng bởi " + usedBy
                            + " biến thể. Hãy sửa hoặc xóa các biến thể đó trước."
            );
        }

        /*
         * Không còn VariantOptionValue nào tham chiếu giá trị này (đã kiểm tra ở trên),
         * nên có thể xóa trực tiếp (schema không có ON DELETE CASCADE).
         */
        Product ownerProduct = optionValue.getProductOption().getProduct();

        productOptionValueRepository.delete(optionValue);

        approvalPolicy.markContentChanged(ownerProduct);
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

        if (inventory.getQuantity() != null && inventory.getQuantity() > 0) {
            log.info("INVENTORY INITIAL variantId={} sku={} before=0 after={} by={}",
                    savedVariant.getId(), savedVariant.getSku(),
                    inventory.getQuantity(), currentActor());
        }

        saveVariantOptionValues(
                savedVariant,
                optionValues
        );

        // Thêm biến thể là thay đổi nội dung cần duyệt lại.
        approvalPolicy.markContentChanged(product);

        return savedVariant;
    }

    /** Atomic bulk creation: reject duplicate SKUs and option combinations before inserting. */
    @Transactional
    public List<ProductVariant> generateVariants(UUID productId,
                                                 com.example.backend.dto.request.GenerateVariantsRequest request) {
        if (request == null || request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new IllegalArgumentException("Variants are required");
        }
        if (request.getVariants().size() > 200) {
            throw new IllegalArgumentException("Maximum 200 variants per request");
        }
        Product product = getProduct(productId);
        Set<String> skus = new HashSet<>();
        Set<Set<UUID>> combinations = new HashSet<>();
        List<ProductVariant> existing = productVariantRepository.findByProductIdOrderByCreatedAtAsc(productId);
        for (ProductVariant v : existing) {
            if (v.getSku() != null) skus.add(v.getSku().trim().toLowerCase(java.util.Locale.ROOT));
            Set<UUID> ids = new HashSet<>();
            for (VariantOptionValue link : variantOptionValueRepository.findByVariantId(v.getId())) {
                ids.add(link.getOptionValue().getId());
            }
            combinations.add(ids);
        }
        for (CreateProductVariantRequest item : request.getVariants()) {
            if (item == null) throw new IllegalArgumentException("Variant cannot be null");
            if (item.getProductId() != null && !productId.equals(item.getProductId())) {
                throw new IllegalArgumentException("Variant belongs to another product");
            }
            item.setProductId(productId);
            validateCreateVariantRequest(item);
            String sku = item.getSku().trim().toLowerCase(java.util.Locale.ROOT);
            if (!skus.add(sku)) throw new IllegalArgumentException("Duplicate SKU: " + item.getSku());
            List<ProductOptionValue> values = getAndValidateOptionValues(product, item.getOptionValueIds());
            validateNoDuplicateOptions(values);
            Set<UUID> ids = new HashSet<>();
            for (ProductOptionValue value : values) ids.add(value.getId());
            if (!combinations.add(ids)) throw new IllegalArgumentException("Duplicate option combination");
        }
        List<ProductVariant> created = new ArrayList<>();
        for (CreateProductVariantRequest item : request.getVariants()) {
            created.add(createVariant(item));
        }
        return created;
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

        // Khóa dòng variant: tuần tự hóa mọi thay đổi (kể cả tạo dòng tồn kho nếu thiếu),
        // vì Inventories không có unique trên variant_id.
        productVariantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found"));

        ProductVariant variant =
                getVariantById(variantId);

        validateUpdateVariantRequest(request);

        Product product =
                variant.getProduct();

        // SKU, giá, giá so sánh, ảnh chính, option là nội dung cần duyệt lại.
        // Tồn kho, trạng thái biến thể, giá vốn là thông tin vận hành.
        boolean contentChanged = false;

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

            if (!sku.equals(variant.getSku())) {
                contentChanged = true;
            }

            variant.setSku(sku);
        }

        if (request.getPrice() != null) {
            if (variant.getPrice() == null
                    || variant.getPrice().compareTo(request.getPrice()) != 0) {
                contentChanged = true;
            }
            variant.setPrice(request.getPrice());
        }

        if (request.getComparePrice() != null) {
            if (variant.getComparePrice() == null
                    || variant.getComparePrice().compareTo(request.getComparePrice()) != 0) {
                contentChanged = true;
            }
            variant.setComparePrice(request.getComparePrice());
        }

        if (request.getCostPrice() != null) variant.setCostPrice(request.getCostPrice());
        if (request.getPrimaryMediaId() != null) {
            ProductMedia primaryMedia = productMediaRepository.findById(request.getPrimaryMediaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Primary media not found"));
            if (!primaryMedia.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Primary media must belong to the same product");
            }
            if (variant.getPrimaryMedia() == null
                    || !variant.getPrimaryMedia().getId().equals(primaryMedia.getId())) {
                contentChanged = true;
            }

            variant.setPrimaryMedia(primaryMedia);
        }

        if (request.getStockQuantity() != null) {

            Inventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                    .orElseGet(() -> { Inventory created = new Inventory(); created.setVariant(variant); created.setReservedQuantity(0); created.setSoldQuantity(0); return created; });
            int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
            if (request.getStockQuantity() < reserved) {
                throw new IllegalArgumentException(
                        "Tồn kho không được nhỏ hơn số lượng đang được giữ chỗ (" + reserved + ")."
                );
            }

            int stockBefore = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            inventory.setQuantity(request.getStockQuantity());
            inventoryRepository.save(inventory);
            variant.setStockQuantity(inventory.getQuantity());

            if (inventory.getQuantity() != stockBefore) {
                log.info("INVENTORY ADJUSTMENT(variant update) variantId={} sku={} before={} after={} by={}",
                        variant.getId(), variant.getSku(),
                        stockBefore, inventory.getQuantity(), currentActor());
            }
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

            java.util.Set<UUID> oldOptionValueIds = new java.util.HashSet<>();
            variantOptionValueRepository.findByVariantId(variantId)
                    .forEach(link -> oldOptionValueIds.add(link.getOptionValue().getId()));

            java.util.Set<UUID> newOptionValueIds = new java.util.HashSet<>();
            optionValues.forEach(v -> newOptionValueIds.add(v.getId()));

            if (!oldOptionValueIds.equals(newOptionValueIds)) {
                contentChanged = true;
            }

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

        if (contentChanged) {
            approvalPolicy.markContentChanged(product);
        }

        return productVariantRepository.save(variant);
    }

    /**
     * Xóa biến thể (API của Shop Owner). Biến thể đã có đơn hàng/đánh giá thì KHÔNG xóa,
     * và việc kiểm tra diễn ra trước mọi thao tác xóa. Xóa biến thể là thay đổi nội dung
     * cần duyệt lại.
     */
    @Transactional
    public void deleteVariant(
            UUID variantId
    ) {
        removeVariant(variantId, true);
    }

    /**
     * Xóa biến thể như một phần của việc xóa cả sản phẩm
     * (ProductManagementService đã kiểm tra lịch sử cho toàn bộ sản phẩm trước đó).
     */
    @Transactional
    public void deleteVariantWithProduct(
            UUID variantId
    ) {
        removeVariant(variantId, false);
    }

    private void removeVariant(
            UUID variantId,
            boolean requireReview
    ) {
        ProductVariant variant =
                getVariantById(variantId);

        // Kiểm tra lịch sử giao dịch TRƯỚC khi xóa bất cứ thứ gì.
        historyGuard.assertVariantDeletable(variantId, variant.getSku());

        Product ownerProduct = variant.getProduct();

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

        if (requireReview) {
            approvalPolicy.markContentChanged(ownerProduct);
        }
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