package com.example.backend.repository;
import com.example.backend.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {
    List<OrderDetail> findBySubOrder_Id(UUID subOrderId);
}
