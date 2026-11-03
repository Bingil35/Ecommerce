package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.CouponRedemption;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
    List<CouponRedemption> findByOrder_Id(UUID orderId);
    List<CouponRedemption> findBySubOrder_Id(UUID subOrderId);
    void deleteBySubOrder_Id(UUID subOrderId);
    void deleteByOrder_Id(UUID orderId);
}
