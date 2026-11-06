package com.example.backend.controller;

import com.example.backend.dto.request.ShopCouponRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopCouponResponse;
import com.example.backend.entity.Coupon;
import com.example.backend.entity.CouponScope;
import com.example.backend.entity.CouponStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CouponRepository;
import com.example.backend.service.ShopService;
import com.example.backend.service.NotificationService;
import com.example.backend.entity.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-owner/coupons")
public class ShopOwnerCouponController {
    private final CouponRepository coupons;
    private final ShopService shops;
    private final NotificationService notifications;
    public ShopOwnerCouponController(CouponRepository coupons, ShopService shops, NotificationService notifications) { this.coupons = coupons; this.shops = shops; this.notifications=notifications; }

    @GetMapping
    public ApiResponse<List<ShopCouponResponse>> list(Authentication auth) {
        var shop = shops.getMyShop(auth.getName());
        return ApiResponse.success("Lấy danh sách khuyến mãi thành công.", coupons.findByShop_IdOrderByCreatedAtDesc(shop.getId()).stream().map(ShopCouponResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<ShopCouponResponse> create(Authentication auth, @RequestBody ShopCouponRequest request) {
        Coupon coupon = new Coupon(); coupon.setShop(shops.getMyShop(auth.getName())); apply(coupon, request);
        coupon.setScope(CouponScope.SHOP); coupon.setUsedCount(0);
        coupon=coupons.save(coupon);
        if(coupon.getStatus()==CouponStatus.ACTIVE)notifications.notifyShopFollowers(coupon.getShop().getId(),"Ưu đãi mới từ cửa hàng","Cửa hàng "+coupon.getShop().getShopName()+" vừa phát hành mã "+coupon.getCode()+".",NotificationType.PROMOTION,coupon.getId());
        return ApiResponse.success("Tạo khuyến mãi thành công.", ShopCouponResponse.from(coupon));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShopCouponResponse> update(Authentication auth, @PathVariable UUID id, @RequestBody ShopCouponRequest request) {
        var shop = shops.getMyShop(auth.getName());
        Coupon coupon = coupons.findById(id).filter(c -> c.getShop() != null && c.getShop().getId().equals(shop.getId())).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi."));
        boolean wasInactive=coupon.getStatus()!=CouponStatus.ACTIVE;
        apply(coupon, request);
        coupon=coupons.save(coupon);
        if(wasInactive&&coupon.getStatus()==CouponStatus.ACTIVE)notifications.notifyShopFollowers(shop.getId(),"Ưu đãi mới từ cửa hàng","Cửa hàng "+shop.getShopName()+" vừa kích hoạt mã "+coupon.getCode()+".",NotificationType.PROMOTION,coupon.getId());
        return ApiResponse.success("Cập nhật khuyến mãi thành công.", ShopCouponResponse.from(coupon));
    }

    private void apply(Coupon coupon, ShopCouponRequest r) {
        if (r == null || r.code() == null || r.code().isBlank() || r.code().trim().length() > 30 || r.discountType() == null || r.discountValue() == null || r.discountValue() <= 0)
            throw new IllegalArgumentException("Mã, loại giảm giá và giá trị giảm hợp lệ là bắt buộc.");
        if (r.discountType().name().equals("PERCENTAGE") && r.discountValue() > 100) throw new IllegalArgumentException("Mức giảm phần trăm không được vượt quá 100.");
        if (r.startAt() != null && r.endAt() != null && r.endAt().isBefore(r.startAt())) throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        coupon.setCode(r.code().trim().toUpperCase()); coupon.setScope(CouponScope.SHOP); coupon.setDiscountType(r.discountType());
        coupon.setDiscountValue(r.discountValue()); coupon.setMinimumOrder(r.minimumOrder()); coupon.setMaximumDiscount(r.maximumDiscount());
        coupon.setUsageLimit(r.usageLimit()); coupon.setStartAt(r.startAt()); coupon.setEndAt(r.endAt());
        coupon.setStatus(r.status() == null ? CouponStatus.ACTIVE : r.status());
    }
}
