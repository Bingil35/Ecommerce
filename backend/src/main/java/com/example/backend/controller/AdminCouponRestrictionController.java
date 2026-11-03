package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.OrderCancellationAuditRepository;
import com.example.backend.entity.OrderCancellationAudit;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Admin review endpoints for coupon-only restrictions.
 * Access is restricted by SecurityConfig's /api/admin/** matcher.
 */
@RestController
@RequestMapping("/api/admin/coupon-restrictions")
public class AdminCouponRestrictionController {
    private final UserRepository userRepository;
    private final OrderCancellationAuditRepository auditRepository;

    public AdminCouponRestrictionController(UserRepository userRepository,
            OrderCancellationAuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponRestrictionItem>>> getPendingReviews() {
        List<CouponRestrictionItem> items = userRepository.findByCouponRestrictedTrueOrderByCouponRestrictedAtAsc()
                .stream().map(user -> {
                    OrderCancellationAudit latest = auditRepository
                            .findFirstByUser_IdOrderByCreatedAtDesc(user.getId()).orElse(null);
                    return new CouponRestrictionItem(
                            user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                            user.getCouponRestrictedAt(), user.getCouponRestrictionReason(),
                            latest == null ? null : latest.getRiskScore(),
                            latest == null ? null : latest.getWeeklyEventCount(),
                            latest == null ? null : latest.getMonthlyEventCount(),
                            latest == null ? null : latest.getMinutesToCancel(),
                            latest == null ? null : latest.getRetainedDiscount7d(),
                            latest == null ? null : latest.getRetainedDiscount30d(),
                            latest == null ? null : latest.getCouponDiscountRetained(),
                            latest == null ? null : latest.getCancellationReason(),
                            latest == null ? null : latest.getOrder().getId());
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Danh sách tài khoản đang bị hạn chế coupon.", items));
    }

    @PutMapping("/{userId}/release")
    public ResponseEntity<ApiResponse<CouponRestrictionItem>> releaseRestriction(
            @PathVariable UUID userId, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (!user.isCouponRestricted()) {
            throw new IllegalArgumentException("Tài khoản hiện không bị hạn chế coupon.");
        }
        user.setCouponRestricted(false);
        user.setCouponRestrictionReviewedAt(OffsetDateTime.now());
        user.setCouponRestrictionReviewedBy(authentication.getName());
        user.setCouponRestrictionReason(null);
        user = userRepository.save(user);
        CouponRestrictionItem item = new CouponRestrictionItem(user.getId(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getCouponRestrictedAt(),
                "Đã được Admin xác minh và gỡ hạn chế bởi " + authentication.getName()
                        + " lúc " + user.getCouponRestrictionReviewedAt(),
                null, null, null, null, null, null, null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Đã gỡ hạn chế sử dụng coupon cho tài khoản.", item));
    }

    public record CouponRestrictionItem(
            UUID userId, String fullName, String email, String phone,
            OffsetDateTime restrictedAt, String reason, Integer latestRiskScore,
            Integer weeklyEventCount, Integer monthlyEventCount, Long minutesToCancel,
            java.math.BigDecimal retainedDiscount7d, java.math.BigDecimal retainedDiscount30d,
            java.math.BigDecimal latestRetainedDiscount, String latestCancellationReason,
            UUID latestOrderId) {}
}
