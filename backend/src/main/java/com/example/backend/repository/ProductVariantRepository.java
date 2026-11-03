package com.example.backend.repository;

import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(
            UUID productId
    );

    List<ProductVariant> findByProductIdAndStatusOrderByCreatedAtAsc(
            UUID productId,
            ProductStatus status
    );

    List<ProductVariant> findByProductIdIn(Collection<UUID> productIds);

    /**
     * Khóa dòng variant để tuần tự hóa mọi thay đổi tồn kho của cùng một biến thể.
     * Bảng Inventories không có ràng buộc unique trên variant_id nên cần khóa này
     * để không bao giờ tạo trùng dòng tồn kho khi có request đồng thời.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProductVariant v where v.id = :id")
    java.util.Optional<ProductVariant> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    List<ProductVariant> findByProductIdInAndStatusOrderByCreatedAtAsc(
            Collection<UUID> productIds,
            ProductStatus status
    );

    List<ProductVariant> findByPrimaryMediaId(UUID primaryMediaId);

    boolean existsByProductIdAndSku(
            UUID productId,
            String sku
    );

    boolean existsByProductIdAndSkuAndIdNot(
            UUID productId,
            String sku,
            UUID id
    );
}