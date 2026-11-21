package com.example.backend.repository;

import com.example.backend.entity.OrderDetail;
import com.example.backend.entity.OrderStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopOwnerProductStatsRepository extends JpaRepository<OrderDetail, UUID> {
    @Query("select d.variant.product.id, sum(coalesce(d.quantity, 0)), sum(coalesce(d.totalAmount, 0)), count(distinct d.subOrder.id) " +
            "from OrderDetail d where d.subOrder.shop.id = :shopId and d.subOrder.status = :status " +
            "and d.variant.product.shopId = :shopId group by d.variant.product.id")
    List<Object[]> lifetimeDelivered(@Param("shopId") UUID shopId, @Param("status") OrderStatus status);

    @Query("select d.variant.product.id, sum(coalesce(d.quantity, 0)), sum(coalesce(d.totalAmount, 0)), count(distinct d.subOrder.id) " +
            "from OrderDetail d where d.subOrder.shop.id = :shopId and d.subOrder.status = :status " +
            "and d.variant.product.shopId = :shopId and d.subOrder.updatedAt >= :from group by d.variant.product.id")
    List<Object[]> deliveredSince(@Param("shopId") UUID shopId, @Param("status") OrderStatus status,
            @Param("from") java.time.OffsetDateTime from);
}
