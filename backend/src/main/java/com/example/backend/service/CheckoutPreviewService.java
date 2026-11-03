package com.example.backend.service;

import com.example.backend.dto.request.CheckoutPreviewRequest;
import com.example.backend.dto.response.CheckoutPreviewResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutPreviewService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final UserRepository users; private final CartRepository carts; private final CartDetailRepository lines;
    private final UserAddressRepository addresses; private final CouponRepository coupons; private final InventoryRepository inventories;
    public CheckoutPreviewService(UserRepository users, CartRepository carts, CartDetailRepository lines,
            UserAddressRepository addresses, CouponRepository coupons, InventoryRepository inventories) {
        this.users=users; this.carts=carts; this.lines=lines; this.addresses=addresses; this.coupons=coupons; this.inventories=inventories;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(String email, CheckoutPreviewRequest request) {
        User user=users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if(user.getStatus()!=UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không hoạt động.");
        UserAddress address=addresses.findByIdAndUser_Id(request.addressId(),user.getId()).orElseThrow(()->new IllegalArgumentException("Địa chỉ không tồn tại hoặc không thuộc tài khoản."));
        if(request.itemIds()==null || request.itemIds().isEmpty() || new HashSet<>(request.itemIds()).size()!=request.itemIds().size()) throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
        Cart cart=carts.findByUserId(user.getId()).orElseThrow(()->new IllegalArgumentException("Giỏ hàng đang trống."));
        Map<UUID,CartDetail> byId=lines.findDetailedByCartId(cart.getId()).stream().collect(Collectors.toMap(CartDetail::getId,x->x));
        List<CartDetail> selected=request.itemIds().stream().map(id->{CartDetail x=byId.get(id); if(x==null) throw new IllegalArgumentException("Sản phẩm không còn trong giỏ hàng."); return x;}).toList();
        Map<UUID,List<CartDetail>> byShop=selected.stream().collect(Collectors.groupingBy(x->x.getSubCart().getShop().getId(),LinkedHashMap::new,Collectors.toList()));
        Map<UUID,BigDecimal> subtotal=new LinkedHashMap<>(); BigDecimal gross=ZERO;
        for(var entry:byShop.entrySet()) { BigDecimal sum=ZERO; for(CartDetail line:entry.getValue()) { ProductVariant v=line.getVariant();
            if(v.getStatus()!=ProductStatus.ACTIVE || v.getProduct().getStatus()!=ProductStatus.ACTIVE || line.getSubCart().getShop().getStatus()!=ShopStatus.ACTIVE) throw new IllegalArgumentException("Có sản phẩm hoặc cửa hàng không còn hoạt động.");
            if(line.getQuantity()==null || line.getQuantity()<=0 || v.getPrice()==null || v.getPrice().signum()<0) throw new IllegalArgumentException("Số lượng hoặc giá sản phẩm không hợp lệ.");
            sum=sum.add(money(v.getPrice()).multiply(BigDecimal.valueOf(line.getQuantity()))); }
            subtotal.put(entry.getKey(),money(sum)); gross=gross.add(sum); }
        gross=money(gross);
        Map<UUID,Integer> requestedByVariant=new HashMap<>();
        for (CartDetail line : selected) requestedByVariant.merge(line.getVariant().getId(), line.getQuantity(), Integer::sum);
        for (var entry : requestedByVariant.entrySet()) {
            Inventory inventory=inventories.findFirstByVariantId(entry.getKey()).orElseThrow(()->new IllegalArgumentException("Sản phẩm chưa được thiết lập tồn kho."));
            int available=Math.max(0,(inventory.getQuantity()==null?0:inventory.getQuantity())-(inventory.getReservedQuantity()==null?0:inventory.getReservedQuantity()));
            if(entry.getValue()>available) throw new IllegalArgumentException("Một sản phẩm không đủ tồn kho. Hiện còn " + available + " sản phẩm khả dụng.");
        }
        Map<UUID,BigDecimal> shopDiscounts=new HashMap<>(); List<String> applied=new ArrayList<>();
        Map<UUID,String> shopCodes=request.shopCouponCodes()==null?Map.of():request.shopCouponCodes();
        boolean any=has(request.platformCouponCode())||shopCodes.values().stream().anyMatch(CheckoutPreviewService::has);
        if(user.isCouponRestricted() && any) throw new AccessDeniedException("Tài khoản đang bị tạm hạn chế sử dụng coupon trong khi Admin xác minh.");
        for(var e:shopCodes.entrySet()) if(has(e.getValue())) {
            if(!byShop.containsKey(e.getKey())) throw new IllegalArgumentException("Không thể áp dụng coupon cho cửa hàng không có sản phẩm được chọn.");
            Coupon c=findCoupon(e.getValue()); if(c.getScope()!=CouponScope.SHOP || c.getShop()==null || !c.getShop().getId().equals(e.getKey())) throw new IllegalArgumentException("Coupon không thuộc cửa hàng đã chọn.");
            validate(c); checkMinimum(c,subtotal.get(e.getKey()));
            BigDecimal value=discount(c,subtotal.get(e.getKey()));
            if(value.signum()<=0) throw new IllegalArgumentException("Coupon không tạo ra giá trị giảm hợp lệ.");
            shopDiscounts.put(e.getKey(),value); applied.add(c.getCode());
        }
        BigDecimal shopDiscount=shopDiscounts.values().stream().reduce(ZERO,BigDecimal::add);
        BigDecimal platformDiscount=ZERO;
        Map<UUID,BigDecimal> platformByShop=new LinkedHashMap<>();
        if(has(request.platformCouponCode())) {
            Coupon c=findCoupon(request.platformCouponCode());
            if(c.getScope()!=CouponScope.PLATFORM || c.getShop()!=null) throw new IllegalArgumentException("Đây không phải coupon toàn sàn.");
            validate(c); checkMinimum(c,gross); platformDiscount=discount(c,money(gross.subtract(shopDiscount)));
            if(platformDiscount.signum()<=0) throw new IllegalArgumentException("Coupon toàn sàn không tạo ra giá trị giảm hợp lệ.");
            applied.add(c.getCode());
            List<UUID> shopIds=new ArrayList<>(byShop.keySet());
            List<BigDecimal> weights=shopIds.stream().map(id->money(subtotal.get(id).subtract(shopDiscounts.getOrDefault(id,ZERO)))).toList();
            List<BigDecimal> parts=allocate(platformDiscount,weights);
            for(int i=0;i<shopIds.size();i++) platformByShop.put(shopIds.get(i),parts.get(i));
        }
        BigDecimal totalDiscount=money(shopDiscount.add(platformDiscount));
        Map<UUID,BigDecimal> lineShopDiscounts=new HashMap<>();
        Map<UUID,BigDecimal> linePlatformDiscounts=new HashMap<>();
        for (var entry : byShop.entrySet()) {
            List<CartDetail> shopLines=entry.getValue();
            List<BigDecimal> weights=shopLines.stream().map(line -> money(line.getVariant().getPrice()).multiply(BigDecimal.valueOf(line.getQuantity()))).toList();
            List<BigDecimal> shopParts=allocate(shopDiscounts.getOrDefault(entry.getKey(),ZERO),weights);
            List<BigDecimal> platformWeights=new ArrayList<>();
            for(int i=0;i<weights.size();i++) platformWeights.add(money(weights.get(i).subtract(shopParts.get(i))));
            List<BigDecimal> platformParts=allocate(platformByShop.getOrDefault(entry.getKey(),ZERO),platformWeights);
            for (int i=0;i<shopLines.size();i++) {
                lineShopDiscounts.put(shopLines.get(i).getId(),shopParts.get(i));
                linePlatformDiscounts.put(shopLines.get(i).getId(),platformParts.get(i));
            }
        }
        List<CheckoutPreviewResponse.ShopPreview> shopViews=new ArrayList<>();
        for(var e:byShop.entrySet()) {
            BigDecimal s=subtotal.get(e.getKey());
            BigDecimal shopCouponDiscount=shopDiscounts.getOrDefault(e.getKey(),ZERO);
            BigDecimal platformCouponDiscount=platformByShop.getOrDefault(e.getKey(),ZERO);
            BigDecimal d=money(shopCouponDiscount.add(platformCouponDiscount));
            Shop shop=e.getValue().get(0).getSubCart().getShop();
            List<CheckoutPreviewResponse.ItemPreview> itemViews=e.getValue().stream().map(line -> {
                BigDecimal lineSubtotal=money(line.getVariant().getPrice()).multiply(BigDecimal.valueOf(line.getQuantity()));
                BigDecimal lineShopDiscount=lineShopDiscounts.getOrDefault(line.getId(),ZERO);
                BigDecimal linePlatformDiscount=linePlatformDiscounts.getOrDefault(line.getId(),ZERO);
                return new CheckoutPreviewResponse.ItemPreview(line.getId(),money(line.getVariant().getPrice()),line.getVariant().getComparePrice(),line.getQuantity(),lineSubtotal,
                        lineShopDiscount,linePlatformDiscount,money(lineSubtotal.subtract(lineShopDiscount).subtract(linePlatformDiscount)));
            }).toList();
            shopViews.add(new CheckoutPreviewResponse.ShopPreview(shop.getId(),shop.getShopName(),e.getValue().size(),s,shopCouponDiscount,platformCouponDiscount,d,money(s.subtract(d)),itemViews));
        }
        // Keep the preview's per-shop and per-line allocation consistent with order placement.
        return new CheckoutPreviewResponse(address.getId(),address.getRecipientName(),address.getRecipientPhone(),address.getProvinceCity(),address.getWard(),address.getAddressDetail(),shopViews,gross,money(shopDiscount),money(platformDiscount),totalDiscount,money(gross.subtract(totalDiscount)),applied);
    }
    private Coupon findCoupon(String code) { List<Coupon> found=coupons.findByCodeIgnoreCase(code.trim()); if(found.size()!=1) throw new IllegalArgumentException(found.isEmpty()?"Coupon không tồn tại.":"Mã coupon bị trùng trong hệ thống."); return found.get(0); }
    private void validate(Coupon c) { OffsetDateTime now=OffsetDateTime.now(); if(c.getStatus()!=CouponStatus.ACTIVE || (c.getStartAt()!=null&&c.getStartAt().isAfter(now)) || (c.getEndAt()!=null&&c.getEndAt().isBefore(now))) throw new IllegalArgumentException("Coupon không còn hiệu lực."); if(c.getUsageLimit()!=null&&(c.getUsedCount()==null?0:c.getUsedCount())>=c.getUsageLimit()) throw new IllegalArgumentException("Coupon đã hết lượt sử dụng."); if(c.getDiscountValue()==null||c.getDiscountValue()<=0||c.getDiscountType()==null) throw new IllegalArgumentException("Coupon chưa được cấu hình hợp lệ."); }
    private void checkMinimum(Coupon c,BigDecimal base) { if(c.getMinimumOrder()!=null&&base.compareTo(money(c.getMinimumOrder()))<0) throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của coupon."); }
    private BigDecimal discount(Coupon c,BigDecimal base) { BigDecimal d=c.getDiscountType()==DiscountType.PERCENTAGE?base.multiply(BigDecimal.valueOf(c.getDiscountValue())).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP):BigDecimal.valueOf(c.getDiscountValue()).setScale(2,RoundingMode.HALF_UP); if(c.getMaximumDiscount()!=null)d=d.min(money(c.getMaximumDiscount())); return money(d.min(base).max(ZERO)); }

    private List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        if (weights.isEmpty()) return List.of();
        BigDecimal total=weights.stream().map(this::money).reduce(ZERO,BigDecimal::add);
        if (total.signum()<=0) return weights.stream().map(x->ZERO).toList();
        List<BigDecimal> result=new ArrayList<>(Collections.nCopies(weights.size(),ZERO));
        int lastPositive=-1;
        for(int i=0;i<weights.size();i++) if(money(weights.get(i)).signum()>0) lastPositive=i;
        BigDecimal remaining=money(amount);
        for(int i=0;i<weights.size();i++) {
            BigDecimal weight=money(weights.get(i));
            if(weight.signum()<=0) continue;
            BigDecimal part=i==lastPositive?remaining:money(amount.multiply(weight).divide(total,2,RoundingMode.DOWN));
            if(part.compareTo(remaining)>0) part=remaining;
            result.set(i,money(part)); remaining=money(remaining.subtract(part));
        }
        return result;
    }
    private BigDecimal money(BigDecimal x){return (x==null?ZERO:x).setScale(2,RoundingMode.HALF_UP);} private static boolean has(String x){return x!=null&&!x.isBlank();}
}
