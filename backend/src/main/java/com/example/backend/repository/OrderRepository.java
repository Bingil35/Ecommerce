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
    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    Optional<Order> findByIdAndUser_Id(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("orderId") UUID orderId, @Param("userId") UUID userId);
    Optional<Order> findByOrderCode(String orderCode);

}
