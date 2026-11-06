package com.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.example.backend.entity.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.response.CancelSubOrderResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CouponRedemptionRepository;
import com.example.backend.repository.CouponRepository;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.repository.OrderCancellationAuditRepository;
import com.example.backend.repository.OrderCouponAllocationRepository;
import com.example.backend.repository.OrderDetailRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.OrderStatusHistoryRepository;
import com.example.backend.repository.SubOrderRepository;
import com.example.backend.repository.UserRepository;

@Service
public class OrderCancellationService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final OrderCouponAllocationRepository allocationRepository;
    private final OrderCancellationAuditRepository auditRepository;
    private final NotificationService notificationService;

    public OrderCancellationService(UserRepository userRepository, OrderRepository orderRepository,
            SubOrderRepository subOrderRepository, OrderDetailRepository orderDetailRepository,
            InventoryRepository inventoryRepository, OrderStatusHistoryRepository historyRepository,
            CouponRepository couponRepository, CouponRedemptionRepository redemptionRepository,
            OrderCouponAllocationRepository allocationRepository, OrderCancellationAuditRepository auditRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository; this.orderRepository = orderRepository; this.subOrderRepository = subOrderRepository;
        this.orderDetailRepository = orderDetailRepository; this.inventoryRepository = inventoryRepository;
        this.historyRepository = historyRepository; this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository; this.allocationRepository = allocationRepository; this.auditRepository = auditRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CancelSubOrderResponse cancelMySubOrder(String email, UUID subOrderId, String reason) {
        User customer = userRepository.findByEmailForUpdate(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (customer.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không hoạt động.");
        SubOrder initial = subOrderRepository.findByIdAndOrder_User_Id(subOrderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng con."));
        Order order = orderRepository.findByIdAndUserIdForUpdate(initial.getOrder().getId(), customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        SubOrder target = subOrderRepository.findByIdAndCustomerForUpdate(subOrderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng con."));
        if (order.getPaymentStatus() == PaymentStatus.PAID) throw new IllegalArgumentException("Đơn hàng đã thanh toán; cần xử lý hoàn tiền trước khi hủy.");
        if (order.getPaymentStatus() != PaymentStatus.PENDING) throw new IllegalArgumentException("Trạng thái thanh toán hiện tại không cho phép tự hủy đơn.");
        if (target.getStatus() != OrderStatus.PENDING) throw new IllegalArgumentException("Chỉ có thể tự hủy đơn con đang ở trạng thái PENDING.");

        List<SubOrder> siblings = subOrderRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId());
        List<OrderCouponAllocation> allocations = allocationRepository.findByOrder_Id(order.getId());
        OrderCouponAllocation platformAllocation = allocations.stream()
                .filter(a -> a.getCoupon().getScope() == CouponScope.PLATFORM).findFirst().orElse(null);
        Coupon platformCoupon = platformAllocation == null ? null : platformAllocation.getCoupon();
        BigDecimal remainingSubtotal = ZERO;
        for (SubOrder sibling : siblings) if (!sibling.getId().equals(target.getId()) && sibling.getStatus() != OrderStatus.CANCELLED)
            remainingSubtotal = remainingSubtotal.add(money(sibling.getSubtotal()));
        boolean platformCouponStillEligible = platformCoupon == null || platformCoupon.getMinimumOrder() == null
                || money(remainingSubtotal).compareTo(money(platformCoupon.getMinimumOrder())) >= 0;

        if (platformCoupon != null && !platformCouponStillEligible) {
            // A platform coupon is no longer eligible: cancel the whole order atomically. Never partially cancel an order that has progressed.
            for (SubOrder sibling : siblings) {
                if (sibling.getStatus() != OrderStatus.PENDING)
                    throw new IllegalArgumentException("Hủy đơn con làm mất điều kiện coupon toàn sàn; các đơn con đều phải còn PENDING để hủy toàn bộ đơn.");
            }
            restoreStock(siblings);
            for (SubOrder sibling : siblings) {
                sibling.setStatus(OrderStatus.CANCELLED);
                historyRepository.save(history(sibling, customer, reason, "Đơn hàng bị hủy do coupon toàn sàn không còn đủ điều kiện sau yêu cầu hủy đơn con."));
            }
            releaseAllRedemptions(order.getId());
            allocationRepository.deleteByOrder_Id(order.getId());
            if (platformCoupon != null) zeroOutRetainedAudit(order.getId(), platformCoupon.getId());
            order.setSubtotal(ZERO); order.setDiscountAmount(ZERO); order.setTotalAmount(ZERO); orderRepository.save(order);
            notificationService.notifyUser(customer.getId(), "Đơn hàng đã được hủy", "Đơn hàng " + order.getOrderCode() + " đã được hủy theo yêu cầu của bạn.", NotificationType.ORDER, order.getId());
            for (SubOrder sibling : siblings) notificationService.notifyShopOwner(sibling.getShop().getId(), "Đơn hàng bị hủy", "Đơn hàng " + order.getOrderCode() + " đã bị khách hủy.", NotificationType.ORDER, order.getId());
            return new CancelSubOrderResponse(order.getId(), target.getId(), target.getStatus(), order.getTotalAmount(),
                    "Điều kiện coupon toàn sàn không còn được đáp ứng; toàn bộ đơn hàng đã bị hủy và tồn kho được hoàn lại.");
        }

        restoreStock(List.of(target));
        target.setStatus(OrderStatus.CANCELLED);
        historyRepository.save(history(target, customer, reason, "Khách hàng tự hủy đơn hàng."));

        // A shop coupon belongs to the cancelled suborder, so release its usage exactly once.
        List<CouponRedemption> targetRedemptions = redemptionRepository.findBySubOrder_Id(target.getId());
        for (CouponRedemption redemption : targetRedemptions) decrementCouponUsage(redemption.getCoupon().getId());
        redemptionRepository.deleteBySubOrder_Id(target.getId());
        allocationRepository.deleteBySubOrder_Id(target.getId());

        // Audit only when the platform coupon discount remains on other active suborders after cancellation.
        if (platformCoupon != null) {
            BigDecimal retained = ZERO;
            for (OrderCouponAllocation allocation : allocations) {
                if (allocation.getCoupon().getId().equals(platformCoupon.getId())
                        && !allocation.getSubOrder().getId().equals(target.getId()))
                    retained = retained.add(money(allocation.getDiscountAmount()));
            }
            retained = money(retained);
            if (retained.signum() > 0) saveCancellationAudit(customer, order, target, platformCoupon, retained, reason);
            else zeroOutRetainedAudit(order.getId(), platformCoupon.getId());
        }

        recalculateOrder(order, siblings);
        notificationService.notifyUser(customer.getId(), "Đơn hàng đã được hủy", "Một phần đơn hàng " + order.getOrderCode() + " đã được hủy theo yêu cầu của bạn.", NotificationType.ORDER, order.getId());
        notificationService.notifyShopOwner(target.getShop().getId(), "Đơn hàng bị hủy", "Khách đã hủy một phần đơn " + order.getOrderCode() + ".", NotificationType.ORDER, order.getId());
        return new CancelSubOrderResponse(order.getId(), target.getId(), target.getStatus(), order.getTotalAmount(),
                "Đã hủy đơn con, hoàn lại tồn kho và cập nhật lại tổng tiền đơn hàng.");
    }

    private void restoreStock(List<SubOrder> subOrders) {
        List<OrderDetail> details = new ArrayList<>();
        for (SubOrder subOrder : subOrders) details.addAll(orderDetailRepository.findBySubOrder_Id(subOrder.getId()));
        List<UUID> variantIds = details.stream().map(OrderDetail::getVariant).filter(Objects::nonNull)
                .map(ProductVariant::getId).distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        Map<UUID, Inventory> inventories = new HashMap<>();
        for (UUID variantId : variantIds) inventories.put(variantId, inventoryRepository.findFirstByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho để hoàn lại sản phẩm " + variantId)));
        for (OrderDetail detail : details) {
            if (detail.getVariant() == null) continue;
            Inventory inventory = inventories.get(detail.getVariant().getId()); int quantity = safe(detail.getQuantity());
            inventory.setQuantity(safe(inventory.getQuantity()) + quantity);
            inventory.setSoldQuantity(Math.max(0, safe(inventory.getSoldQuantity()) - quantity));
        }
    }

    private void releaseAllRedemptions(UUID orderId) {
        List<CouponRedemption> redemptions = redemptionRepository.findByOrder_Id(orderId);
        List<UUID> couponIds = redemptions.stream().map(r -> r.getCoupon().getId()).distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        for (UUID couponId : couponIds) {
            Coupon coupon = couponRepository.findByIdForUpdate(couponId).orElseThrow(() -> new IllegalStateException("Không tìm thấy coupon đã sử dụng."));
            long usageCount = redemptions.stream().filter(r -> r.getCoupon().getId().equals(couponId)).count();
            coupon.setUsedCount(Math.max(0, safe(coupon.getUsedCount()) - (int) usageCount)); couponRepository.save(coupon);
        }
        redemptionRepository.deleteByOrder_Id(orderId);
    }

    private void decrementCouponUsage(UUID couponId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId).orElseThrow(() -> new IllegalStateException("Không tìm thấy coupon đã sử dụng."));
        coupon.setUsedCount(Math.max(0, safe(coupon.getUsedCount()) - 1)); couponRepository.save(coupon);
    }

    private void saveCancellationAudit(User customer, Order order, SubOrder target, Coupon coupon, BigDecimal retained, String reason) {
        String eventKey = order.getId().toString().replace("-", "") + ":" + target.getId().toString().replace("-", "") + ":" + coupon.getId().toString().replace("-", "");
        if (auditRepository.existsByEventKey(eventKey)) return;

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sevenDaysAgo = now.minusDays(7);
        OffsetDateTime thirtyDaysAgo = now.minusDays(30);
        List<OrderCancellationAudit> previous30d =
                auditRepository.findByUser_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(customer.getId(), thirtyDaysAgo);
        List<OrderCancellationAudit> previous7d = previous30d.stream()
                .filter(a -> !a.getCreatedAt().isBefore(sevenDaysAgo)).toList();

        // A single checkout/order may contain several shop suborders. Count it once per window
        // so cancelling multiple suborders from one purchase cannot artificially trigger a ban.
        java.util.Set<UUID> weeklyOrderIds = previous7d.stream().map(a -> a.getOrder().getId())
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<UUID> monthlyOrderIds = previous30d.stream().map(a -> a.getOrder().getId())
                .collect(java.util.stream.Collectors.toSet());
        weeklyOrderIds.add(order.getId()); monthlyOrderIds.add(order.getId());
        int weeklyCount = weeklyOrderIds.size();
        int monthlyCount = monthlyOrderIds.size();
        // Count cancellation events for frequency, but aggregate retained value once per Order+Coupon.
        // If the same order is cancelled in several steps, its latest remaining discount replaces
        // the previous audit value instead of being added again (prevents inflated risk scores).
        Map<String, BigDecimal> retainedByOrderCoupon7d = latestRetainedByOrderCoupon(previous7d);
        Map<String, BigDecimal> retainedByOrderCoupon30d = latestRetainedByOrderCoupon(previous30d);
        String currentOrderCouponKey = order.getId() + ":" + coupon.getId();
        retainedByOrderCoupon7d.put(currentOrderCouponKey, money(retained));
        retainedByOrderCoupon30d.put(currentOrderCouponKey, money(retained));
        BigDecimal retained7d = money(retainedByOrderCoupon7d.values().stream().reduce(ZERO, BigDecimal::add));
        BigDecimal retained30d = money(retainedByOrderCoupon30d.values().stream().reduce(ZERO, BigDecimal::add));

        long minutesToCancel = 0L;
        if (order.getCreatedAt() != null) {
            minutesToCancel = Math.max(0L, java.time.Duration.between(order.getCreatedAt(), now).toMinutes());
        }

        // Cancellation speed is only a weak signal; it can never trigger a restriction on its own.
        int score = 0;
        if (minutesToCancel < 2) score += 5;
        else if (minutesToCancel < 10) score += 3;
        else if (minutesToCancel < 30) score += 2;
        else if (minutesToCancel < 120) score += 1;

        if (weeklyCount >= 6) score += 15;
        else if (weeklyCount >= 4) score += 10;
        else if (weeklyCount >= 2) score += 3;

        if (monthlyCount >= 10) score += 18;
        else if (monthlyCount >= 7) score += 12;
        else if (monthlyCount >= 5) score += 7;
        else if (monthlyCount >= 3) score += 3;

        if (retained30d.compareTo(new BigDecimal("1000000")) >= 0) score += 12;
        else if (retained30d.compareTo(new BigDecimal("500000")) >= 0) score += 8;
        else if (retained30d.compareTo(new BigDecimal("200000")) >= 0) score += 5;
        else if (retained30d.compareTo(new BigDecimal("50000")) >= 0) score += 2;

        CancellationRiskLevel level = score >= 35 ? CancellationRiskLevel.BLACK
                : score >= 20 ? CancellationRiskLevel.RED
                : score >= 10 ? CancellationRiskLevel.YELLOW : CancellationRiskLevel.LEVEL_1;

        OrderCancellationAudit audit = new OrderCancellationAudit();
        audit.setUser(customer); audit.setOrder(order); audit.setSubOrder(target); audit.setCoupon(coupon);
        audit.setCouponDiscountRetained(money(retained));
        audit.setCancellationReason(reason == null ? null : reason.trim().substring(0, Math.min(reason.trim().length(), 500)));
        audit.setWindowCount(weeklyCount); audit.setWeeklyEventCount(weeklyCount); audit.setMonthlyEventCount(monthlyCount);
        audit.setMinutesToCancel(minutesToCancel); audit.setRetainedDiscount7d(retained7d);
        audit.setRetainedDiscount30d(retained30d); audit.setRiskScore(score); audit.setRiskLevel(level);
        audit.setEventKey(eventKey);
        auditRepository.save(audit);

        if (score >= 20) {
            notificationService.notifyRole("ADMIN", "Cảnh báo rủi ro voucher",
                    customer.getFullName() + " (" + customer.getEmail() + ") có điểm rủi ro " + score
                            + ", hủy " + monthlyCount + " đơn trong 30 ngày và giữ lại " + retained30d.toPlainString() + " ₫ ưu đãi.",
                    NotificationType.SYSTEM, audit.getId());
        }

        // Only the high-risk threshold restricts coupon use. Login and regular-price checkout remain available.
        if (score >= 35 && !customer.isCouponRestricted()) {
            customer.setCouponRestricted(true);
            customer.setCouponRestrictedAt(now);
            customer.setCouponRestrictionReviewedAt(null);
            customer.setCouponRestrictionReviewedBy(null);
            customer.setCouponRestrictionReason("Tạm khóa coupon để Admin xác minh. Điểm rủi ro: " + score
                    + "; sự kiện 7 ngày: " + weeklyCount + "; sự kiện 30 ngày: " + monthlyCount
                    + "; giá trị ưu đãi giữ lại 30 ngày: " + retained30d.toPlainString()
                    + "; thời gian từ lúc đặt đến hủy: " + minutesToCancel + " phút.");
        }
    }


    private void zeroOutRetainedAudit(UUID orderId, UUID couponId) {
        List<OrderCancellationAudit> previous = auditRepository.findByOrder_IdAndCoupon_Id(orderId, couponId);
        for (OrderCancellationAudit audit : previous) audit.setCouponDiscountRetained(ZERO);
        auditRepository.saveAll(previous);
    }

    private Map<String, BigDecimal> latestRetainedByOrderCoupon(List<OrderCancellationAudit> audits) {
        Map<String, BigDecimal> latest = new java.util.LinkedHashMap<>();
        for (OrderCancellationAudit audit : audits) {
            if (audit.getOrder() == null || audit.getCoupon() == null) continue;
            String key = audit.getOrder().getId() + ":" + audit.getCoupon().getId();
            latest.put(key, money(audit.getCouponDiscountRetained()));
        }
        return latest;
    }

    private OrderStatusHistory history(SubOrder subOrder, User customer, String reason, String defaultNote) {
        OrderStatusHistory history = new OrderStatusHistory(); history.setSubOrder(subOrder);
        history.setOldStatus(OrderStatus.PENDING); history.setNewStatus(OrderStatus.CANCELLED); history.setChangedBy(customer);
        String clean = reason == null ? "" : reason.trim();
        history.setNote(clean.isBlank() ? defaultNote : defaultNote + " Lý do: " + clean.substring(0, Math.min(clean.length(), 500)));
        return history;
    }

    private void recalculateOrder(Order order, List<SubOrder> siblings) {
        BigDecimal subtotal = ZERO, discount = ZERO, total = ZERO;
        for (SubOrder sibling : siblings) {
            if (sibling.getStatus() == OrderStatus.CANCELLED) continue;
            subtotal = subtotal.add(money(sibling.getSubtotal())); discount = discount.add(money(sibling.getDiscountAmount())); total = total.add(money(sibling.getTotalAmount()));
        }
        order.setSubtotal(money(subtotal)); order.setDiscountAmount(money(discount)); order.setTotalAmount(money(total)); orderRepository.save(order);
    }
    private int safe(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private static final BigDecimal ZERO = new BigDecimal("0.00");
}
