package com.example.backend.repository;
import com.example.backend.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.Inventory;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findFirstByVariantId(UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variant.id = :variantId")
    Optional<Inventory> findFirstByVariantIdForUpdate(@Param("variantId") UUID variantId);

    void deleteByVariantId(UUID variantId);
    List<Inventory> findByVariantIdIn(Collection<UUID> variantIds);

    /** Khóa dòng tồn kho để cập nhật an toàn khi nhiều request đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdForUpdate(@Param("variantId") UUID variantId);

    /**
     * Biến thể ACTIVE của shop có tồn khả dụng (quantity - reserved) không vượt quá
     * ngưỡng cảnh báo (ngưỡng riêng của biến thể, hoặc ngưỡng mặc định).
     * Sắp xếp: ít hàng nhất trước.
     */
    @Query(
            value = """
                    select i from Inventory i
                    join fetch i.variant v
                    join fetch v.product p
                    where p.shopId = :shopId
                      and v.status = com.example.backend.entity.ProductStatus.ACTIVE
                      and (coalesce(i.quantity, 0) - coalesce(i.reservedQuantity, 0))
                          <= (case when coalesce(i.lowStockThreshold, 0) > 0
                                   then i.lowStockThreshold
                                   else :defaultThreshold end)
                    order by (coalesce(i.quantity, 0) - coalesce(i.reservedQuantity, 0)) asc,
                             p.name asc, v.sku asc
                    """,
            countQuery = """
                    select count(i) from Inventory i
                    join i.variant v
                    join v.product p
                    where p.shopId = :shopId
                      and v.status = com.example.backend.entity.ProductStatus.ACTIVE
                      and (coalesce(i.quantity, 0) - coalesce(i.reservedQuantity, 0))
                          <= (case when coalesce(i.lowStockThreshold, 0) > 0
                                   then i.lowStockThreshold
                                   else :defaultThreshold end)
                    """
    )
    Page<Inventory> findLowStock(
            @Param("shopId") UUID shopId,
            @Param("defaultThreshold") int defaultThreshold,
            Pageable pageable
    );
}
