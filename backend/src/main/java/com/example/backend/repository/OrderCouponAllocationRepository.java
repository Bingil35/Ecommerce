package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.OrderCouponAllocation;

public interface OrderCouponAllocationRepository extends JpaRepository<OrderCouponAllocation, UUID> {
    List<OrderCouponAllocation> findByOrder_Id(UUID orderId);
    List<OrderCouponAllocation> findBySubOrder_Id(UUID subOrderId);
    void deleteBySubOrder_Id(UUID subOrderId);
    void deleteByOrder_Id(UUID orderId);
}
