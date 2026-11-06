package com.example.backend.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.service.LocalStorageService;
import com.example.backend.service.MediaFileInspector;
import com.example.backend.service.NotificationService;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final UserRepository users; private final UserRoleRepository userRoles; private final ShopRepository shops;
    private final CouponRepository coupons; private final SystemBannerRepository banners; private final OrderRepository orders;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final NotificationService notificationService; private final LocalStorageService storage; private final MediaFileInspector inspector;
    public AdminManagementController(UserRepository users, UserRoleRepository userRoles, ShopRepository shops,
            CouponRepository coupons, SystemBannerRepository banners, OrderRepository orders, CategoryRepository categories, ProductRepository products,
            NotificationService notificationService, LocalStorageService storage, MediaFileInspector inspector) {
        this.users=users; this.userRoles=userRoles; this.shops=shops; this.coupons=coupons; this.banners=banners; this.orders=orders; this.categories=categories; this.products=products;
        this.notificationService=notificationService;this.storage=storage;this.inspector=inspector;
    }
    @GetMapping("/dashboard") @Transactional(readOnly=true)
    public ApiResponse<Map<String,Object>> dashboard() {
        Map<String,Object> d=new LinkedHashMap<>(); d.put("users",users.count()); d.put("shops",shops.count());
        d.put("pendingShops",shops.findByStatusOrderByShopNameAsc(ShopStatus.PENDING).size()); d.put("products",products.count());
        d.put("categories",categories.count()); d.put("orders",orders.count());
        d.put("revenue",orders.sumTotalAmount());
        d.put("activeCoupons",coupons.findAll().stream().filter(c->c.getScope()==CouponScope.PLATFORM && c.getStatus()==CouponStatus.ACTIVE).count());
        d.put("activeBanners",banners.findAll().stream().filter(b->b.getStatus()==BannerStatus.ACTIVE).count());
        d.put("recentUsers",users.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream().limit(6).map(u->userView(u)).toList());
        return ApiResponse.success("Dashboard",d);
    }
    @GetMapping("/users") @Transactional(readOnly=true)
    public ApiResponse<List<Map<String,Object>>> listUsers(@RequestParam(required=false) String q, @RequestParam(required=false) String status) {
        String query=q==null?"":q.toLowerCase(Locale.ROOT); String st=status==null?"":status;
        return ApiResponse.success("Users",users.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream()
            .filter(u->(query.isBlank() || safe(u.getFullName()).toLowerCase().contains(query)||safe(u.getEmail()).toLowerCase().contains(query)||safe(u.getPhone()).contains(query))&&(st.isBlank()||u.getStatus().name().equals(st)))
            .map(this::userView).toList());
    }
    private Map<String,Object> userView(User u) { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",u.getId());m.put("fullName",u.getFullName());m.put("email",u.getEmail());m.put("phone",u.getPhone());m.put("avatarUrl",u.getAvatarUrl());m.put("status",u.getStatus());m.put("roles",userRoles.findRoleNamesByUserId(u.getId()));m.put("createdAt",u.getCreatedAt());m.put("lastLoginAt",u.getLastLoginAt());return m; }
    private String safe(String s){return s==null?"":s;}
    @PatchMapping("/users/{id}/status") @Transactional
    public ApiResponse<Map<String,Object>> userStatus(@PathVariable UUID id,@RequestBody StatusRequest r,Authentication auth) {
        User u=users.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy tài khoản."));
        if ("ADMIN".equalsIgnoreCase(r.status()) && userRoles.findRoleNamesByUserId(id).contains("ADMIN")) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (u.getEmail().equalsIgnoreCase(auth.getName()) && "INACTIVE".equals(r.status())) throw new IllegalArgumentException("Không thể tự khóa tài khoản quản trị.");
        u.setStatus(UserStatus.valueOf(r.status())); users.save(u); return ApiResponse.success("Đã cập nhật tài khoản.",userView(u));
    }
    @GetMapping("/shops") @Transactional(readOnly=true)
    public ApiResponse<List<Map<String,Object>>> listShops(@RequestParam(required=false) String q,@RequestParam(required=false) String status) {
        String query=q==null?"":q.toLowerCase(Locale.ROOT);
        return ApiResponse.success("Shops",shops.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream()
          .filter(s->(query.isBlank()||safe(s.getShopName()).toLowerCase().contains(query)||safe(s.getSlug()).toLowerCase().contains(query)||safe(s.getEmail()).toLowerCase().contains(query))&&(status==null||status.isBlank()||s.getStatus().name().equals(status)))
          .map(s->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.getId());m.put("shopName",s.getShopName());m.put("slug",s.getSlug());m.put("email",s.getEmail());m.put("phone",s.getPhone());m.put("status",s.getStatus());m.put("createdAt",s.getCreatedAt());if(s.getOwnerUserId()!=null)users.findById(s.getOwnerUserId()).ifPresent(u->{m.put("ownerName",u.getFullName());m.put("ownerEmail",u.getEmail());});return m;}).toList());
    }
    @PatchMapping("/shops/{id}/status") @Transactional
    public ApiResponse<Map<String,Object>> shopStatus(@PathVariable UUID id,@RequestBody StatusRequest r) {
        Shop s=shops.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy cửa hàng."));s.setStatus(ShopStatus.valueOf(r.status()));shops.save(s);
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.getId());m.put("shopName",s.getShopName());m.put("status",s.getStatus());return ApiResponse.success("Đã cập nhật cửa hàng.",m);
    }
    @GetMapping("/coupons") @Transactional(readOnly=true)
    public ApiResponse<List<Map<String,Object>>> listCoupons(){return ApiResponse.success("Coupons",coupons.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream().filter(c->c.getScope()==CouponScope.PLATFORM).map(this::couponView).toList());}
    private Map<String,Object> couponView(Coupon c){Map<String,Object> m=new LinkedHashMap<>();m.put("id",c.getId());m.put("code",c.getCode());m.put("discountType",c.getDiscountType());m.put("discountValue",c.getDiscountValue());m.put("minimumOrder",c.getMinimumOrder());m.put("maximumDiscount",c.getMaximumDiscount());m.put("usageLimit",c.getUsageLimit());m.put("usedCount",c.getUsedCount());m.put("startAt",c.getStartAt());m.put("endAt",c.getEndAt());m.put("status",c.getStatus());return m;}
    @PostMapping("/coupons") public ResponseEntity<ApiResponse<Map<String,Object>>> createCoupon(@RequestBody CouponRequest r){Coupon c=saveCoupon(new Coupon(),r);if(c.getStatus()==CouponStatus.ACTIVE)notificationService.notifyRole("CUSTOMER","Ưu đãi mới trên Kala","Mã giảm giá toàn sàn "+c.getCode()+" vừa được phát hành.",NotificationType.PROMOTION,c.getId());return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đã tạo mã giảm giá.",couponView(c)));}
    @PutMapping("/coupons/{id}") public ApiResponse<Map<String,Object>> updateCoupon(@PathVariable UUID id,@RequestBody CouponRequest r){Coupon c=coupons.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy mã giảm giá."));return ApiResponse.success("Đã cập nhật mã giảm giá.",couponView(saveCoupon(c,r)));}
    @DeleteMapping("/coupons/{id}") public ApiResponse<Void> deleteCoupon(@PathVariable UUID id){Coupon c=coupons.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy mã giảm giá."));if(c.getUsedCount()!=null&&c.getUsedCount()>0){c.setStatus(CouponStatus.INACTIVE);coupons.save(c);}else coupons.delete(c);return ApiResponse.success("Đã ngừng mã giảm giá.",null);}
    private Coupon saveCoupon(Coupon c,CouponRequest r){if(r.code()==null||r.code().isBlank()||r.discountType()==null||r.discountValue()==null)throw new IllegalArgumentException("Vui lòng nhập đủ thông tin mã giảm giá.");c.setShop(null);c.setScope(CouponScope.PLATFORM);c.setCode(r.code().trim().toUpperCase(Locale.ROOT));c.setDiscountType(r.discountType());c.setDiscountValue(r.discountValue());c.setMinimumOrder(r.minimumOrder());c.setMaximumDiscount(r.maximumDiscount());c.setUsageLimit(r.usageLimit());c.setStartAt(r.startAt());c.setEndAt(r.endAt());c.setStatus(r.status()==null?CouponStatus.ACTIVE:r.status());return coupons.save(c);}
    @GetMapping("/banners") public ApiResponse<List<SystemBanner>> listBanners(){return ApiResponse.success("Banners",banners.findAllByOrderByPriorityAscCreatedAtDesc());}
    @PostMapping(value="/banners/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<Map<String,String>> uploadBanner(@RequestParam("file") MultipartFile file){MediaFileInspector.Detected d=inspector.inspect(file);if(d.type()!=ProductMediaType.IMAGE)throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");return ApiResponse.success("Đã tải ảnh banner.",Map.of("url",storage.storeSystemBannerImage(file,d.extension())));}
    @PostMapping("/banners") public ResponseEntity<ApiResponse<SystemBanner>> createBanner(@RequestBody BannerRequest r){SystemBanner b=saveBanner(new SystemBanner(),r);if(b.getStatus()==BannerStatus.ACTIVE)notificationService.notifyRole("CUSTOMER","Tin mới từ Kala",b.getTitle(),NotificationType.PROMOTION,b.getId());return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đã tạo banner.",b));}
    @PutMapping("/banners/{id}") public ApiResponse<SystemBanner> updateBanner(@PathVariable UUID id,@RequestBody BannerRequest r){SystemBanner b=banners.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy banner."));return ApiResponse.success("Đã cập nhật banner.",saveBanner(b,r));}
    @DeleteMapping("/banners/{id}") public ApiResponse<Void> deleteBanner(@PathVariable UUID id){banners.delete(banners.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy banner.")));return ApiResponse.success("Đã xóa banner.",null);}
    private SystemBanner saveBanner(SystemBanner b,BannerRequest r){if(r.title()==null||r.title().isBlank()||r.imageUrl()==null||r.imageUrl().isBlank())throw new IllegalArgumentException("Vui lòng nhập tiêu đề và đường dẫn ảnh.");b.setTitle(r.title().trim());b.setImageUrl(r.imageUrl().trim());b.setPriority(r.priority()==null?0:r.priority());b.setStatus(r.status()==null?BannerStatus.ACTIVE:r.status());return banners.save(b);}
    public record StatusRequest(String status){} public record CouponRequest(String code,DiscountType discountType,Integer discountValue,BigDecimal minimumOrder,BigDecimal maximumDiscount,Integer usageLimit,OffsetDateTime startAt,OffsetDateTime endAt,CouponStatus status){} public record BannerRequest(String title,String imageUrl,Short priority,BannerStatus status){}
}
