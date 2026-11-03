package com.example.backend.repository;
import com.example.backend.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findBySubOrder_IdOrderByCreatedAtAsc(UUID subOrderId);
}
