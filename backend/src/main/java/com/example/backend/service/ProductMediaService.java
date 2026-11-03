package com.example.backend.service;

import com.example.backend.dto.request.UpdateProductMediaRequest;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductMedia;
import com.example.backend.entity.ProductMediaType;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductMediaRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductMediaService {

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final LocalStorageService localStorageService;
    private final ProductVariantRepository productVariantRepository;
    private final MediaFileInspector mediaFileInspector;
    private final ProductApprovalPolicy approvalPolicy;

    public ProductMediaService(
            ProductRepository productRepository,
            ProductMediaRepository productMediaRepository,
            LocalStorageService localStorageService,
            ProductVariantRepository productVariantRepository,
            MediaFileInspector mediaFileInspector,
            ProductApprovalPolicy approvalPolicy
    ) {
        this.productRepository = productRepository;
        this.productMediaRepository = productMediaRepository;
        this.localStorageService = localStorageService;
        this.productVariantRepository = productVariantRepository;
        this.mediaFileInspector = mediaFileInspector;
        this.approvalPolicy = approvalPolicy;
    }

    @Transactional
    public ProductMedia createMedia(
            UUID productId,
            MultipartFile file,
            Integer displayOrder
    ) {
        Product product = getProduct(productId);

        int order = displayOrder == null
                ? 0
                : displayOrder;

        validateDisplayOrder(order);

        // Nhận diện loại file theo NỘI DUNG, không tin Content-Type/tên file của client.
        MediaFileInspector.Detected detected = mediaFileInspector.inspect(file);

        ProductMediaType mediaType = detected.type();

        String mediaUrl =
                localStorageService.storeProductMedia(
                        productId,
                        file,
                        detected.extension()
                );

        // Nếu transaction rollback thì không để lại file mồ côi.
        TransactionCallbacks.afterRollback(() -> deleteFileQuietly(mediaUrl));

        ProductMedia media = new ProductMedia();

        media.setProduct(product);
        media.setMediaType(mediaType);
        media.setMediaUrl(mediaUrl);
        media.setDisplayOrder(order);

        ProductMedia saved = productMediaRepository.save(media);

        // Ảnh đầu tiên tự trở thành ảnh đại diện nếu sản phẩm chưa có.
        if (mediaType == ProductMediaType.IMAGE
                && (product.getThumbnailUrl() == null
                || product.getThumbnailUrl().isBlank())) {
            product.setThumbnailUrl(mediaUrl);
            productRepository.save(product);
        }

        // Thêm hình ảnh mới là thay đổi nội dung cần duyệt lại.
        approvalPolicy.markContentChanged(product);

        return saved;
    }

    public List<ProductMedia> getMediaByProduct(
            UUID productId
    ) {
        getProduct(productId);

        return productMediaRepository
                .findByProductIdOrderBySortOrderAsc(productId);
    }

    public ProductMedia getMediaById(UUID mediaId) {
        return productMediaRepository.findById(mediaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product media not found"
                        )
                );
    }

    @Transactional
    public ProductMedia updateMedia(
            UUID mediaId,
            UpdateProductMediaRequest request
    ) {
        ProductMedia media =
                getMediaById(mediaId);

        validateUpdateRequest(request);

        if (request.getDisplayOrder() != null) {
            media.setDisplayOrder(
                    request.getDisplayOrder()
            );
        }

        return productMediaRepository.save(media);
    }

    /**
     * Sắp xếp lại thứ tự hiển thị. mediaIds phải gồm ĐÚNG tất cả media của sản phẩm
     * (không thiếu, không thừa, không trùng); thứ tự trong danh sách là thứ tự mới.
     */
    @Transactional
    public List<ProductMedia> reorderMedia(UUID productId, List<UUID> mediaIds) {

        getProduct(productId);

        if (mediaIds == null || mediaIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách media không được để trống.");
        }

        List<ProductMedia> current =
                productMediaRepository.findByProductIdOrderBySortOrderAsc(productId);

        Set<UUID> requested = new HashSet<>(mediaIds);

        if (requested.size() != mediaIds.size()) {
            throw new IllegalArgumentException("Danh sách media có id bị trùng.");
        }

        Set<UUID> existing = new HashSet<>();
        current.forEach(m -> existing.add(m.getId()));

        if (!existing.equals(requested)) {
            throw new IllegalArgumentException(
                    "Danh sách phải gồm đúng tất cả media của sản phẩm."
            );
        }

        java.util.Map<UUID, ProductMedia> byId = new java.util.HashMap<>();
        current.forEach(m -> byId.put(m.getId(), m));

        List<ProductMedia> ordered = new java.util.ArrayList<>();

        for (int i = 0; i < mediaIds.size(); i++) {
            ProductMedia media = byId.get(mediaIds.get(i));
            media.setDisplayOrder(i);
            ordered.add(media);
        }

        return productMediaRepository.saveAll(ordered);
    }

    /** Đặt một ảnh làm ảnh đại diện của sản phẩm (video không được). */
    @Transactional
    public ProductMedia setThumbnail(UUID mediaId) {

        ProductMedia media = getMediaById(mediaId);

        if (media.getMediaType() != ProductMediaType.IMAGE) {
            throw new IllegalArgumentException("Chỉ có thể đặt ảnh làm ảnh đại diện.");
        }

        Product product = media.getProduct();
        product.setThumbnailUrl(media.getMediaUrl());
        productRepository.save(product);

        return media;
    }

    @Transactional(readOnly = true)
    public String getThumbnailUrl(UUID productId) {
        return getProduct(productId).getThumbnailUrl();
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        ProductMedia media =
                getMediaById(mediaId);

        productVariantRepository.findByPrimaryMediaId(mediaId).forEach(v -> v.setPrimaryMedia(null));
        productMediaRepository.delete(media);

        // Chỉ xóa file vật lý SAU KHI DB commit; rollback thì file được giữ nguyên.
        String urlToDelete = media.getMediaUrl();
        TransactionCallbacks.afterCommit(() -> deleteFileQuietly(urlToDelete));

        // Nếu xóa đúng ảnh đang là ảnh đại diện thì chuyển sang ảnh khác (hoặc để trống).
        Product product = media.getProduct();
        String deletedUrl = media.getMediaUrl();

        if (product != null
                && deletedUrl != null
                && deletedUrl.equals(product.getThumbnailUrl())) {

            String next = productMediaRepository
                    .findByProductIdOrderBySortOrderAsc(product.getId())
                    .stream()
                    .filter(m -> !m.getId().equals(mediaId))
                    .filter(m -> m.getMediaType() == ProductMediaType.IMAGE)
                    .map(ProductMedia::getMediaUrl)
                    .findFirst()
                    .orElse(null);

            product.setThumbnailUrl(next);
            productRepository.save(product);
        }

        // Xóa hình ảnh là thay đổi nội dung cần duyệt lại.
        if (product != null) {
            approvalPolicy.markContentChanged(product);
        }
    }

    private void deleteFileQuietly(String url) {
        try {
            localStorageService.deleteProductMedia(url);
        } catch (RuntimeException ignored) {
            // File mồ côi không làm hỏng nghiệp vụ.
        }
    }

    private Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
    }

    private void validateUpdateRequest(
            UpdateProductMediaRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.getDisplayOrder() != null) {
            validateDisplayOrder(
                    request.getDisplayOrder()
            );
        }
    }

    private void validateDisplayOrder(
            Integer displayOrder
    ) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException(
                    "Display order cannot be negative"
            );
        }
    }
}