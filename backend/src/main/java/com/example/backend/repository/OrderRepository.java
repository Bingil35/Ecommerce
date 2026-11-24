package com.example.backend.repository;
import com.example.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    java.math.BigDecimal sumTotalAmount();
    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.createdAt >= :from and o.createdAt < :to")
    java.math.BigDecimal sumTotalAmountBetween(@Param("from") java.time.OffsetDateTime from, @Param("to") java.time.OffsetDateTime to);
    @Query("select p.paymentMethod, count(o) from Order o join o.payment p where o.createdAt >= :from and o.createdAt < :to group by p.paymentMethod")
    List<Object[]> countByPaymentMethodForOrdersCreatedBetween(@Param("from") java.time.OffsetDateTime from, @Param("to") java.time.OffsetDateTime to);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.OffsetDateTime from, java.time.OffsetDateTime to);
    @Query(value = "select to_char(date_trunc('day', created_at at time zone 'Asia/Bangkok'), 'YYYY-MM-DD'), coalesce(sum(total_amount), 0) from orders where created_at >= :from and created_at < :to group by date_trunc('day', created_at at time zone 'Asia/Bangkok') order by date_trunc('day', created_at at time zone 'Asia/Bangkok')", nativeQuery = true)
    List<Object[]> revenueByDay(@Param("from") java.time.OffsetDateTime from, @Param("to") java.time.OffsetDateTime to);
    @Query(value = "select to_char(date_trunc('month', created_at at time zone 'Asia/Bangkok'), 'YYYY-MM'), coalesce(sum(total_amount), 0) from orders where created_at >= :from and created_at < :to group by date_trunc('month', created_at at time zone 'Asia/Bangkok') order by date_trunc('month', created_at at time zone 'Asia/Bangkok')", nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("from") java.time.OffsetDateTime from, @Param("to") java.time.OffsetDateTime to);
    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    Optional<Order> findByIdAndUser_Id(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("orderId") UUID orderId, @Param("userId") UUID userId);
    Optional<Order> findByOrderCode(String orderCode);

}
