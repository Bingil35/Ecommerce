package com.example.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.entity.OrderCancellationAudit;

public interface OrderCancellationAuditRepository extends JpaRepository<OrderCancellationAudit, UUID> {
    List<OrderCancellationAudit> findTop100ByOrderByCreatedAtDesc();
    long countByUser_IdAndCreatedAtGreaterThanEqual(UUID userId, OffsetDateTime from);
    boolean existsByEventKey(String eventKey);
    java.util.Optional<OrderCancellationAudit> findFirstByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<OrderCancellationAudit> findByUser_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(UUID userId, OffsetDateTime from);
    List<OrderCancellationAudit> findByOrder_IdAndCoupon_Id(UUID orderId, UUID couponId);
}
