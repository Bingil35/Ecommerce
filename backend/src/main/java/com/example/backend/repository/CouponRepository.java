package com.example.backend.repository;

import java.util.List;
import java.util.UUID;
import com.example.backend.entity.CouponStatus;
import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.Coupon;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    java.util.Optional<Coupon> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c left join fetch c.shop where lower(c.code) = lower(:code) order by c.id")
    List<Coupon> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
    @Query("select c from Coupon c left join fetch c.shop where c.status = :status and (c.startAt is null or c.startAt <= :now) and (c.endAt is null or c.endAt >= :now) order by c.endAt asc nulls last")
    List<Coupon> findCurrentlyActive(@Param("status") CouponStatus status, @Param("now") OffsetDateTime now);
    @Query("select c from Coupon c left join fetch c.shop where lower(c.code) = lower(:code) order by c.id")
    List<Coupon> findByCodeIgnoreCase(@Param("code") String code);
}

