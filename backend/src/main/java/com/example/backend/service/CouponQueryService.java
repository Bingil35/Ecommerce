package com.example.backend.service;

import com.example.backend.dto.response.CouponResponse;
import com.example.backend.entity.Coupon;
import com.example.backend.entity.CouponScope;
import com.example.backend.entity.CouponStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CouponRepository;
import com.example.backend.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponQueryService {
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    public CouponQueryService(CouponRepository couponRepository, UserRepository userRepository) {
        this.couponRepository = couponRepository; this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public List<CouponResponse> getAvailable(String email, CouponScope scope) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (user.isCouponRestricted()) return List.of();
        OffsetDateTime now = OffsetDateTime.now();
        return couponRepository.findCurrentlyActive(CouponStatus.ACTIVE, now).stream()
                .filter(c -> scope == null || c.getScope() == scope)
                .filter(c -> c.getUsageLimit() == null || (c.getUsedCount() == null || c.getUsedCount() < c.getUsageLimit()))
                .map(CouponResponse::from).toList();
    }
}
