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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.example.backend.entity.OrderStatus;
public interface SubOrderRepository extends JpaRepository<SubOrder, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<SubOrder> {
    long countByShop_Id(UUID shopId);
    long countByShop_IdAndStatus(UUID shopId, OrderStatus status);
    @Query("select sum(s.totalAmount) from SubOrder s where s.shop.id = :shopId and s.status = :status")
    BigDecimal sumTotalByShopIdAndStatus(@Param("shopId") UUID shopId, @Param("status") OrderStatus status);
    List<SubOrder> findByShop_IdAndStatusAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(UUID shopId, OrderStatus status, OffsetDateTime from);
    @org.springframework.data.jpa.repository.Query("select s from SubOrder s join s.order o join o.user u where s.shop.ownerUserId = :ownerId and (:status is null or s.status = :status) and (:keyword = '' or lower(o.orderCode) like concat('%', :keyword, '%') or lower(u.fullName) like concat('%', :keyword, '%'))" )
    org.springframework.data.domain.Page<SubOrder> searchShopOrders(@Param("ownerId") UUID ownerId,
            @Param("status") OrderStatus status, @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
    List<SubOrder> findByOrder_IdOrderByCreatedAtAsc(UUID orderId);
    Optional<SubOrder> findByIdAndOrder_User_Id(UUID id, UUID userId);
    @Query("select s from SubOrder s join fetch s.order o where s.id = :subOrderId and s.shop.ownerUserId = :ownerId")
    Optional<SubOrder> findByIdAndShopOwner(@Param("subOrderId") UUID subOrderId, @Param("ownerId") UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubOrder s join fetch s.order o where s.id = :subOrderId and o.user.id = :userId")
    Optional<SubOrder> findByIdAndCustomerForUpdate(@Param("subOrderId") UUID subOrderId, @Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubOrder s join fetch s.shop sh where s.id = :subOrderId and sh.ownerUserId = :ownerId")
    Optional<SubOrder> findByIdAndShopOwnerForUpdate(@Param("subOrderId") UUID subOrderId, @Param("ownerId") UUID ownerId);
}

