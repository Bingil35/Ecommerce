package com.example.backend.service;

import com.example.backend.dto.request.CreateProductRequest;
import com.example.backend.dto.request.UpdateProductRequest;
import com.example.backend.dto.response.ProductSummary;
import com.example.backend.entity.Category;
import com.example.backend.entity.Inventory;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WishlistService wishlistService;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductApprovalPolicy approvalPolicy;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            WishlistService wishlistService,
            ProductVariantRepository variantRepository,
            InventoryRepository inventoryRepository,
            ProductApprovalPolicy approvalPolicy
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.wishlistService = wishlistService;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
        this.approvalPolicy = approvalPolicy;
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
        // Luôn PENDING: Shop Owner không thể tự tạo sản phẩm ACTIVE/REJECTED.
        product.setStatus(ProductStatus.PENDING);

        return productRepository.save(product);
    }

    // =========================
    // SUMMARY (GIÁ / TỒN KHO) - dùng cho shop owner
    // =========================

    /**
     * Tính tóm tắt cho nhiều sản phẩm bằng đúng 2 truy vấn
     * (variants + inventories), tránh N+1.
     * Sản phẩm chưa có variant nhận ProductSummary.EMPTY.
     */
    @Transactional(readOnly = true)
    public Map<UUID, ProductSummary> getSummaries(Collection<UUID> productIds) {

        Map<UUID, ProductSummary> result = new HashMap<>();

        if (productIds == null || productIds.isEmpty()) {
            return result;
        }

        List<ProductVariant> variants =
                variantRepository.findByProductIdIn(productIds);

        Map<UUID, Integer> quantityByVariant = new HashMap<>();
        if (!variants.isEmpty()) {
            List<UUID> variantIds = variants.stream()
                    .map(ProductVariant::getId)
                    .toList();

            for (Inventory inventory :
                    inventoryRepository.findByVariantIdIn(variantIds)) {
                // Tồn khả dụng = quantity - reserved (không âm).
                int quantity = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
                int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
                quantityByVariant.merge(
                        inventory.getVariant().getId(),
                        Math.max(0, quantity - reserved),
                        Integer::sum
                );
            }
        }

        Map<UUID, int[]> counts = new HashMap<>();
        Map<UUID, BigDecimal> minPrices = new HashMap<>();
        Map<UUID, BigDecimal> maxPrices = new HashMap<>();
        Map<UUID, ProductVariant> firstVariants = new HashMap<>();

        for (ProductVariant v : variants) {

            UUID productId = v.getProduct().getId();

            // counts[0] = variantCount, counts[1] = totalStock
            int[] c = counts.computeIfAbsent(productId, k -> new int[2]);
            c[0]++;
            firstVariants.merge(productId, v, ProductService::earlierVariant);

            if (v.getStatus() != ProductStatus.ACTIVE) {
                continue;
            }

            c[1] += quantityByVariant.getOrDefault(v.getId(), 0);

            BigDecimal price = v.getPrice();
            if (price != null) {
                minPrices.merge(productId, price, BigDecimal::min);
                maxPrices.merge(productId, price, BigDecimal::max);
            }
        }

        for (UUID productId : productIds) {
            int[] c = counts.get(productId);
            result.put(
                    productId,
                    c == null
                            ? ProductSummary.EMPTY
                            : new ProductSummary(
                            c[0],
                            minPrices.get(productId),
                            maxPrices.get(productId),
                            c[1],
                            firstVariants.get(productId).getSku()
                    )
            );
        }

        return result;
    }

    private static ProductVariant earlierVariant(ProductVariant a, ProductVariant b) {
        if (a.getCreatedAt() == null) {
            return b;
        }
        if (b.getCreatedAt() == null) {
            return a;
        }
        return b.getCreatedAt().isBefore(a.getCreatedAt()) ? b : a;
    }

    @Transactional(readOnly = true)
    public ProductSummary getSummary(UUID productId) {
        return getSummaries(List.of(productId))
                .getOrDefault(productId, ProductSummary.EMPTY);
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

        // Có thay đổi ở nội dung cần kiểm duyệt lại hay không.
        boolean contentChanged = false;

        if (request.getCategoryId() != null) {

            Category category = categoryRepository.findById(
                    request.getCategoryId()
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Category not found"
                    )
            );

            UUID currentCategoryId = product.getCategory() == null
                    ? null
                    : product.getCategory().getId();

            if (!category.getId().equals(currentCategoryId)) {
                contentChanged = true;
            }

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
                contentChanged = true;
            }
        }

        if (request.getDescription() != null) {
            if (!java.util.Objects.equals(
                    product.getDescription(), request.getDescription())) {
                contentChanged = true;
            }
            product.setDescription(request.getDescription());
        }

        if (request.getBrandId() != null) {
            if (!request.getBrandId().equals(product.getBrandId())) {
                contentChanged = true;
            }
            product.setBrandId(request.getBrandId());
        }

        if (request.getThumbnailUrl() != null) {
            if (!java.util.Objects.equals(
                    product.getThumbnailUrl(), request.getThumbnailUrl())) {
                contentChanged = true;
            }
            product.setThumbnailUrl(request.getThumbnailUrl());
        }

        // featured là thông tin vận hành, không cần duyệt lại.
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        /*
         * Trạng thái KHÔNG được đổi qua API này (request không còn field status).
         * Sửa nội dung quan trọng: ACTIVE/INACTIVE -> PENDING; PENDING/REJECTED giữ nguyên.
         */
        if (contentChanged) {
            approvalPolicy.markContentChanged(product);
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
         * Việc kiểm tra đơn hàng/đánh giá (ProductHistoryGuard) và dọn dữ liệu liên quan
         * do ProductManagementService.deleteProduct thực hiện TRƯỚC khi gọi method này.
         * Không gọi trực tiếp method này từ controller.
         */

        /*
         * Xóa sản phẩm khỏi mọi danh sách yêu thích, nếu không sẽ vi phạm khóa ngoại.
         * Wishlist không phải dữ liệu đơn hàng nên việc xóa này là an toàn.
         */
        wishlistService.removeProductFromAllWishlists(id);

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