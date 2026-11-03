package com.example.backend.repository;
import com.example.backend.entity.SubOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.example.backend.entity.OrderStatus;
public interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {
    List<SubOrder> findByOrder_IdOrderByCreatedAtAsc(UUID orderId);
    Optional<SubOrder> findByIdAndOrder_User_Id(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubOrder s join fetch s.order o where s.id = :subOrderId and o.user.id = :userId")
    Optional<SubOrder> findByIdAndCustomerForUpdate(@Param("subOrderId") UUID subOrderId, @Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubOrder s join fetch s.shop sh where s.id = :subOrderId and sh.ownerUserId = :ownerId")
    Optional<SubOrder> findByIdAndShopOwnerForUpdate(@Param("subOrderId") UUID subOrderId, @Param("ownerId") UUID ownerId);
}

