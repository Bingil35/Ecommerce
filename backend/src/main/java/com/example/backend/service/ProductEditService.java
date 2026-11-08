package com.example.backend.service;

import com.example.backend.dto.request.EditProductRequest;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductMedia;
import com.example.backend.entity.ProductMediaType;
import com.example.backend.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ProductEditService {
    private final ProductService products;
    private final ProductMediaService media;
    private final ProductVariantService variants;
    private final ProductVariantRepository variantRepository;
    private final ProductOwnershipService ownership;
    private final MediaFileInspector inspector;

    public ProductEditService(ProductService products, ProductMediaService media,
                              ProductVariantService variants, ProductVariantRepository variantRepository,
                              ProductOwnershipService ownership, MediaFileInspector inspector) {
        this.products = products;
        this.media = media;
        this.variants = variants;
        this.variantRepository = variantRepository;
        this.ownership = ownership;
        this.inspector = inspector;
    }

    /** All DB writes share a transaction; media service cleans files on rollback/commit. */
    @Transactional
    public Product update(UUID productId, String email, EditProductRequest form,
                          List<MultipartFile> images, List<MultipartFile> videos) {
        ownership.assertProduct(productId, email);
        if (form == null || form.product() == null) throw new IllegalArgumentException("Product form is required");
        images = images == null ? List.of() : images;
        videos = videos == null ? List.of() : videos;
        List<ProductMedia> current = media.getMediaByProduct(productId);
        Set<UUID> deleted = new HashSet<>(form.deletedMediaIds() == null ? List.of() : form.deletedMediaIds());
        Set<UUID> existing = new HashSet<>();
        current.forEach(m -> existing.add(m.getId()));
        if (!existing.containsAll(deleted)) throw new IllegalArgumentException("Media does not belong to this product");
        List<ProductMedia> retained = current.stream().filter(m -> !deleted.contains(m.getId())).toList();
        long imageCount = retained.stream().filter(m -> m.getMediaType() == ProductMediaType.IMAGE).count();
        long videoCount = retained.size() - imageCount;
        if (imageCount + images.size() > 10 || videoCount + videos.size() > 3)
            throw new IllegalArgumentException("Maximum 10 images and 3 videos");
        for (MultipartFile file : images) {
            var detected = inspector.inspect(file);
            if (detected.type() != ProductMediaType.IMAGE || !Set.of("jpg", "png", "webp").contains(detected.extension()))
                throw new IllegalArgumentException("Only JPG, PNG and WEBP images are supported");
        }
        for (MultipartFile file : videos) {
            if (inspector.inspect(file).type() != ProductMediaType.VIDEO)
                throw new IllegalArgumentException("Only MP4 and WEBM videos are supported");
        }
        if (form.thumbnailMediaId() != null && form.thumbnailImageIndex() != null)
            throw new IllegalArgumentException("Select only one thumbnail");
        if (form.thumbnailMediaId() != null && retained.stream().noneMatch(m ->
                m.getId().equals(form.thumbnailMediaId()) && m.getMediaType() == ProductMediaType.IMAGE))
            throw new IllegalArgumentException("Thumbnail must be a retained image of this product");
        if (form.thumbnailImageIndex() != null && (form.thumbnailImageIndex() < 0 || form.thumbnailImageIndex() >= images.size()))
            throw new IllegalArgumentException("Invalid thumbnail image index");
        if (form.variants() != null) for (var entry : form.variants().entrySet()) {
            var variant = variantRepository.findById(entry.getKey()).orElseThrow(() -> new IllegalArgumentException("Variant not found"));
            if (!variant.getProduct().getId().equals(productId) || entry.getValue() == null)
                throw new IllegalArgumentException("Variant does not belong to this product");
        }
        // Thumbnail URLs must come from owned media, never from the client.
        form.product().setThumbnailUrl(null);
        Product product = products.updateProduct(productId, form.product());
        if (form.variants() != null) for (var entry : form.variants().entrySet())
            variants.updateVariant(entry.getKey(), entry.getValue());
        for (UUID id : deleted) media.deleteMedia(id);
        int order = retained.stream().mapToInt(m -> m.getDisplayOrder() == null ? 0 : m.getDisplayOrder()).max().orElse(-1) + 1;
        List<ProductMedia> addedImages = new ArrayList<>();
        for (MultipartFile file : images) addedImages.add(media.createMedia(productId, file, order++));
        for (MultipartFile file : videos) media.createMedia(productId, file, order++);
        if (form.thumbnailMediaId() != null) media.setThumbnail(form.thumbnailMediaId());
        if (form.thumbnailImageIndex() != null) media.setThumbnail(addedImages.get(form.thumbnailImageIndex()).getId());
        return product;
    }
}
