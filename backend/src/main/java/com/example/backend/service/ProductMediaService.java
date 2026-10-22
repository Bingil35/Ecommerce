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

import java.util.List;
import java.util.UUID;

@Service
public class ProductMediaService {

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final LocalStorageService localStorageService;
    private final ProductVariantRepository productVariantRepository;

    public ProductMediaService(
            ProductRepository productRepository,
            ProductMediaRepository productMediaRepository,
            LocalStorageService localStorageService,
            ProductVariantRepository productVariantRepository
    ) {
        this.productRepository = productRepository;
        this.productMediaRepository = productMediaRepository;
        this.localStorageService = localStorageService;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional
    public ProductMedia createMedia(
            UUID productId,
            MultipartFile file,
            Integer displayOrder
    ) {
        Product product = getProduct(productId);

        validateFile(file);

        int order = displayOrder == null
                ? 0
                : displayOrder;

        validateDisplayOrder(order);

        ProductMediaType mediaType =
                determineMediaType(file);

        String mediaUrl =
                localStorageService.storeProductMedia(
                        productId,
                        file
                );

        ProductMedia media = new ProductMedia();

        media.setProduct(product);
        media.setMediaType(mediaType);
        media.setMediaUrl(mediaUrl);
        media.setDisplayOrder(order);

        return productMediaRepository.save(media);
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

    @Transactional
    public void deleteMedia(UUID mediaId) {
        ProductMedia media =
                getMediaById(mediaId);

        productVariantRepository.findByPrimaryMediaId(mediaId).forEach(v -> v.setPrimaryMedia(null));
        localStorageService.deleteProductMedia(media.getMediaUrl());
        productMediaRepository.delete(media);
    }

    private Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
    }

    private ProductMediaType determineMediaType(
            MultipartFile file
    ) {
        String contentType =
                file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException(
                    "Cannot determine file type"
            );
        }

        if (contentType.startsWith("image/")) {
            return ProductMediaType.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return ProductMediaType.VIDEO;
        }

        throw new IllegalArgumentException(
                "Only image and video files are supported"
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }
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