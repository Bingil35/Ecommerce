package com.example.backend.service;

import com.example.backend.dto.request.PlaceOrderRequest;
import com.example.backend.dto.response.PlacedOrderResponse;
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
public class OrderService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserAddressRepository userAddressRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ShopRepository shopRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final OrderCouponAllocationRepository couponAllocationRepository;
    private final NotificationService notificationService;

    public OrderService(UserRepository userRepository, CartRepository cartRepository,
            CartDetailRepository cartDetailRepository, UserAddressRepository userAddressRepository,
            InventoryRepository inventoryRepository, PaymentRepository paymentRepository,
            OrderRepository orderRepository, SubOrderRepository subOrderRepository,
            OrderAddressRepository orderAddressRepository, OrderDetailRepository orderDetailRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository, ShopRepository shopRepository,
            CouponRepository couponRepository, CouponRedemptionRepository couponRedemptionRepository,
            OrderCouponAllocationRepository couponAllocationRepository, NotificationService notificationService) {
        this.userRepository = userRepository; this.cartRepository = cartRepository;
        this.cartDetailRepository = cartDetailRepository; this.userAddressRepository = userAddressRepository;
        this.inventoryRepository = inventoryRepository; this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository; this.subOrderRepository = subOrderRepository;
        this.orderAddressRepository = orderAddressRepository; this.orderDetailRepository = orderDetailRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository; this.shopRepository = shopRepository;
        this.couponRepository = couponRepository; this.couponRedemptionRepository = couponRedemptionRepository;
        this.couponAllocationRepository = couponAllocationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public PlacedOrderResponse placeOrder(String email, PlaceOrderRequest request) {
        if (request == null || request.getAddressId() == null || request.getPaymentMethod() == null
                || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn địa chỉ, phương thức thanh toán và ít nhất một sản phẩm.");
        }
        List<UUID> requestedIds = request.getItemIds();
        if (requestedIds.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Danh sách sản phẩm có mã không hợp lệ.");
        if (new LinkedHashSet<>(requestedIds).size() != requestedIds.size()) throw new IllegalArgumentException("Danh sách sản phẩm bị trùng.");

        User customer = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản khách hàng."));
        if (customer.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không hoạt động.");
        UserAddress address = userAddressRepository.findByIdAndUser_Id(request.getAddressId(), customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản của bạn."));
        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống."));

        List<CartDetail> allCartLines = cartDetailRepository.findDetailedByCartId(cart.getId());
        Map<UUID, CartDetail> cartLineById = allCartLines.stream().collect(Collectors.toMap(CartDetail::getId, line -> line));
        List<CartDetail> selectedLines = new ArrayList<>();
        for (UUID itemId : requestedIds) {
            CartDetail line = cartLineById.get(itemId);
            if (line == null) throw new IllegalArgumentException("Có sản phẩm không còn trong giỏ hàng của bạn. Vui lòng tải lại giỏ hàng.");
            selectedLines.add(line);
        }

        Map<UUID, List<CartDetail>> linesByShop = selectedLines.stream().collect(Collectors.groupingBy(
                line -> line.getSubCart().getShop().getId(), LinkedHashMap::new, Collectors.toList()));
        for (CartDetail line : selectedLines) {
            ProductVariant variant = line.getVariant(); Product product = variant.getProduct(); Shop cartShop = line.getSubCart().getShop();
            if (variant.getStatus() != ProductStatus.ACTIVE || product.getStatus() != ProductStatus.ACTIVE)
                throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' hiện không còn được bán.");
            if (product.getShopId() == null || !product.getShopId().equals(cartShop.getId())) throw new IllegalArgumentException("Sản phẩm trong giỏ không khớp với cửa hàng.");
            Shop currentShop = shopRepository.findById(cartShop.getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng."));
            if (currentShop.getStatus() != ShopStatus.ACTIVE) throw new IllegalArgumentException("Cửa hàng '" + currentShop.getShopName() + "' hiện không hoạt động.");
            if (line.getQuantity() == null || line.getQuantity() <= 0) throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            if (variant.getPrice() == null || variant.getPrice().signum() < 0) throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' chưa có giá bán hợp lệ.");
        }

        Map<UUID, BigDecimal> lineGross = new LinkedHashMap<>();
        Map<UUID, BigDecimal> subtotalByShop = new LinkedHashMap<>();
        BigDecimal orderSubtotal = ZERO;
        for (Map.Entry<UUID, List<CartDetail>> entry : linesByShop.entrySet()) {
            BigDecimal shopSubtotal = ZERO;
            for (CartDetail line : entry.getValue()) {
                BigDecimal gross = money(line.getVariant().getPrice()).multiply(BigDecimal.valueOf(line.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                lineGross.put(line.getId(), gross); shopSubtotal = shopSubtotal.add(gross);
            }
            subtotalByShop.put(entry.getKey(), money(shopSubtotal)); orderSubtotal = orderSubtotal.add(shopSubtotal);
        }
        orderSubtotal = money(orderSubtotal);

        // Validate coupon codes in stable lexical order; each coupon row is locked while its usage limit is checked.
        Map<UUID, Coupon> shopCoupons = new LinkedHashMap<>();
        Map<UUID, BigDecimal> shopDiscounts = new LinkedHashMap<>();
        Map<UUID, BigDecimal> platformDiscounts = new LinkedHashMap<>();
        Map<UUID, BigDecimal> lineShopDiscounts = new HashMap<>();
        Map<UUID, BigDecimal> linePlatformDiscounts = new HashMap<>();
        Map<String, Coupon> lockedCouponsByCode = new HashMap<>();
        Map<UUID, String> requestedShopCodes = request.getShopCouponCodes() == null ? Map.of() : request.getShopCouponCodes();
        for (Map.Entry<UUID, String> e : requestedShopCodes.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank() && !linesByShop.containsKey(e.getKey()))
                throw new IllegalArgumentException("Không thể áp dụng coupon cho cửa hàng không có sản phẩm được chọn.");
        }
        boolean requestedAnyCoupon = hasText(request.getPlatformCouponCode())
                || requestedShopCodes.values().stream().anyMatch(OrderService::hasText);
        if (customer.isCouponRestricted() && requestedAnyCoupon) {
            throw new AccessDeniedException("Tài khoản đang bị tạm hạn chế sử dụng coupon trong khi Admin xác minh. Bạn vẫn có thể đặt hàng không áp dụng coupon.");
        }

        List<String> codesToLock = new ArrayList<>();
        if (hasText(request.getPlatformCouponCode())) codesToLock.add(request.getPlatformCouponCode().trim());
        requestedShopCodes.values().stream().filter(OrderService::hasText).map(String::trim).forEach(codesToLock::add);
        codesToLock = codesToLock.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        for (String code : codesToLock) {
            List<Coupon> matches = couponRepository.findByCodeIgnoreCaseForUpdate(code);
            if (matches.size() != 1) throw new IllegalArgumentException(matches.isEmpty() ? "Mã coupon '" + code + "' không tồn tại." : "Mã coupon bị trùng trong hệ thống; vui lòng liên hệ quản trị viên.");
            Coupon coupon = matches.get(0); validateCoupon(coupon, code);
            lockedCouponsByCode.put(code.toLowerCase(Locale.ROOT), coupon);
        }

        for (Map.Entry<UUID, String> e : requestedShopCodes.entrySet()) {
            if (!hasText(e.getValue())) continue;
            Coupon coupon = lockedCouponsByCode.get(e.getValue().trim().toLowerCase(Locale.ROOT));
            if (coupon.getScope() != CouponScope.SHOP || coupon.getShop() == null || !coupon.getShop().getId().equals(e.getKey()))
                throw new IllegalArgumentException("Coupon '" + e.getValue() + "' không thuộc cửa hàng đã chọn.");
            BigDecimal shopSubtotal = subtotalByShop.get(e.getKey()); validateMinimum(coupon, shopSubtotal);
            BigDecimal discount = calculateDiscount(coupon, shopSubtotal);
            if (discount.signum() <= 0) throw new IllegalArgumentException("Coupon không tạo ra giá trị giảm hợp lệ.");
            shopCoupons.put(e.getKey(), coupon); shopDiscounts.put(e.getKey(), discount);
            List<BigDecimal> allocated = allocate(
                    discount,
                    linesByShop.get(e.getKey()).stream()
                            .map(l -> lineGross.get(l.getId()))
                            .toList()
            );
            List<CartDetail> shopLines = linesByShop.get(e.getKey());
            for (int i = 0; i < shopLines.size(); i++) lineShopDiscounts.put(shopLines.get(i).getId(), allocated.get(i));
        }

        Coupon platformCoupon = null;
        BigDecimal totalShopDiscount = shopDiscounts.values().stream().reduce(ZERO, BigDecimal::add);
        BigDecimal platformBase = money(orderSubtotal.subtract(totalShopDiscount));
        if (hasText(request.getPlatformCouponCode())) {
            platformCoupon = lockedCouponsByCode.get(request.getPlatformCouponCode().trim().toLowerCase(Locale.ROOT));
            if (platformCoupon.getScope() != CouponScope.PLATFORM || platformCoupon.getShop() != null)
                throw new IllegalArgumentException("Mã coupon đã nhập không phải coupon toàn sàn.");
            validateMinimum(platformCoupon, orderSubtotal);
            BigDecimal platformDiscount = calculateDiscount(platformCoupon, platformBase);
            if (platformDiscount.signum() <= 0) throw new IllegalArgumentException("Coupon toàn sàn không tạo ra giá trị giảm hợp lệ.");
            Map<UUID, BigDecimal> weights = new LinkedHashMap<>();
            for (UUID shopId : linesByShop.keySet()) weights.put(shopId, money(subtotalByShop.get(shopId).subtract(shopDiscounts.getOrDefault(shopId, ZERO))));
            List<UUID> shopIds = new ArrayList<>(weights.keySet());
            List<BigDecimal> allocation = allocate(platformDiscount, shopIds.stream().map(weights::get).toList());
            for (int i = 0; i < shopIds.size(); i++) platformDiscounts.put(shopIds.get(i), allocation.get(i));
            for (UUID shopId : shopIds) {
                List<CartDetail> shopLines = linesByShop.get(shopId);
                List<BigDecimal> remainingWeights = shopLines.stream().map(l -> money(lineGross.get(l.getId()).subtract(lineShopDiscounts.getOrDefault(l.getId(), ZERO)))).toList();
                List<BigDecimal> lineAllocation = allocate(platformDiscounts.get(shopId), remainingWeights);
                for (int i = 0; i < shopLines.size(); i++) linePlatformDiscounts.put(shopLines.get(i).getId(), lineAllocation.get(i));
            }
        }

        List<UUID> variantIds = selectedLines.stream().map(l -> l.getVariant().getId()).distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        Map<UUID, Inventory> lockedInventory = new HashMap<>();
        for (UUID variantId : variantIds) lockedInventory.put(variantId, inventoryRepository.findFirstByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Một sản phẩm chưa được thiết lập tồn kho.")));
        Map<UUID, Integer> quantityByVariant = new HashMap<>();
        for (CartDetail line : selectedLines) quantityByVariant.merge(line.getVariant().getId(), line.getQuantity(), Integer::sum);
        for (Map.Entry<UUID, Integer> entry : quantityByVariant.entrySet()) {
            Inventory inventory = lockedInventory.get(entry.getKey());
            int available = Math.max(0, safeInt(inventory.getQuantity()) - safeInt(inventory.getReservedQuantity()));
            if (entry.getValue() > available) {
                ProductVariant variant = selectedLines.stream().map(CartDetail::getVariant).filter(v -> v.getId().equals(entry.getKey())).findFirst().orElseThrow();
                throw new IllegalArgumentException("Sản phẩm '" + variant.getProduct().getName() + "' chỉ còn " + available + " sản phẩm khả dụng.");
            }
        }

        Payment payment = new Payment(); payment.setPaymentMethod(request.getPaymentMethod()); payment = paymentRepository.save(payment);
        Order order = new Order(); order.setUser(customer); order.setPayment(payment);
        order.setOrderCode("KAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        order.setPaymentStatus(PaymentStatus.PENDING); order.setSubtotal(orderSubtotal); order.setDiscountAmount(ZERO); order.setTotalAmount(orderSubtotal);
        order = orderRepository.save(order);

        OrderAddress addressSnapshot = new OrderAddress(); addressSnapshot.setRecipientName(address.getRecipientName());
        addressSnapshot.setRecipientPhone(address.getRecipientPhone()); addressSnapshot.setRecipientCity(address.getProvinceCity());
        addressSnapshot.setWard(address.getWard()); addressSnapshot.setAddressDetail(address.getAddressDetail());
        addressSnapshot.setIsDefault(Boolean.TRUE.equals(address.getIsDefault())); addressSnapshot = orderAddressRepository.save(addressSnapshot);

        Map<UUID, SubOrder> savedSubOrders = new LinkedHashMap<>();
        List<PlacedOrderResponse.SubOrderItem> responseSubOrders = new ArrayList<>();
        BigDecimal orderDiscount = ZERO;
        for (Map.Entry<UUID, List<CartDetail>> shopEntry : linesByShop.entrySet()) {
            UUID shopId = shopEntry.getKey(); Shop shop = shopEntry.getValue().get(0).getSubCart().getShop();
            BigDecimal shopSubtotal = subtotalByShop.get(shopId);
            BigDecimal shopCouponDiscount = shopDiscounts.getOrDefault(shopId, ZERO);
            BigDecimal platformAllocation = platformDiscounts.getOrDefault(shopId, ZERO);
            BigDecimal subDiscount = money(shopCouponDiscount.add(platformAllocation));
            SubOrder subOrder = new SubOrder(); subOrder.setOrder(order); subOrder.setShop(shop); subOrder.setOrderAddress(addressSnapshot);
            subOrder.setStatus(OrderStatus.PENDING); subOrder.setSubtotal(shopSubtotal); subOrder.setDiscountAmount(subDiscount);
            subOrder.setTotalAmount(money(shopSubtotal.subtract(subDiscount))); subOrder = subOrderRepository.save(subOrder);
            savedSubOrders.put(shopId, subOrder);
            List<PlacedOrderResponse.OrderLine> responseLines = new ArrayList<>();
            for (CartDetail cartLine : shopEntry.getValue()) {
                BigDecimal gross = lineGross.get(cartLine.getId());
                BigDecimal detailDiscount = money(lineShopDiscounts.getOrDefault(cartLine.getId(), ZERO).add(linePlatformDiscounts.getOrDefault(cartLine.getId(), ZERO)));
                OrderDetail detail = new OrderDetail(); detail.setSubOrder(subOrder); detail.setVariant(cartLine.getVariant());
                detail.setProductName(cartLine.getVariant().getProduct().getName()); detail.setPrice(money(cartLine.getVariant().getPrice()));
                detail.setQuantity(cartLine.getQuantity()); detail.setDiscountAmount(detailDiscount); detail.setTotalAmount(money(gross.subtract(detailDiscount)));
                detail = orderDetailRepository.save(detail);
                responseLines.add(new PlacedOrderResponse.OrderLine(detail.getId(), detail.getVariant().getId(), detail.getProductName(),
                        detail.getPrice(), detail.getQuantity(), detail.getDiscountAmount(), detail.getTotalAmount()));
            }
            orderStatusHistoryRepository.save(createInitialHistory(subOrder, customer));
            if (shopCoupons.containsKey(shopId) && shopCouponDiscount.signum() > 0)
                saveCouponAllocation(shopCoupons.get(shopId), order, subOrder, shopCouponDiscount);
            if (platformCoupon != null && platformAllocation.signum() > 0)
                saveCouponAllocation(platformCoupon, order, subOrder, platformAllocation);
            orderDiscount = orderDiscount.add(subDiscount);
            responseSubOrders.add(new PlacedOrderResponse.SubOrderItem(subOrder.getId(), shop.getId(), shop.getShopName(), subOrder.getStatus(),
                    subOrder.getSubtotal(), subOrder.getDiscountAmount(), subOrder.getTotalAmount(), responseLines));
        }

        if (platformCoupon != null) redeem(platformCoupon, customer, order, null);
        for (Map.Entry<UUID, Coupon> e : shopCoupons.entrySet()) redeem(e.getValue(), customer, order, savedSubOrders.get(e.getKey()));
        for (Map.Entry<UUID, Integer> entry : quantityByVariant.entrySet()) {
            Inventory inventory = lockedInventory.get(entry.getKey()); inventory.setQuantity(safeInt(inventory.getQuantity()) - entry.getValue());
            inventory.setSoldQuantity(safeInt(inventory.getSoldQuantity()) + entry.getValue());
        }
        orderDiscount = money(orderDiscount); order.setSubtotal(orderSubtotal); order.setDiscountAmount(orderDiscount); order.setTotalAmount(money(orderSubtotal.subtract(orderDiscount)));
        order = orderRepository.save(order);
        cartDetailRepository.deleteAll(selectedLines); cart.touch();
        notificationService.notifyUser(customer.getId(), "Đặt hàng thành công", "Đơn hàng " + order.getOrderCode() + " đã được ghi nhận.", NotificationType.ORDER, order.getId());
        notificationService.notifyRole("ADMIN", "Có đơn hàng mới", "Đơn hàng " + order.getOrderCode() + " vừa được tạo.", NotificationType.ORDER, order.getId());
        for (UUID shopId : linesByShop.keySet()) notificationService.notifyShopOwner(shopId, "Đơn hàng mới", "Bạn vừa nhận đơn " + order.getOrderCode() + ".", NotificationType.ORDER, order.getId());
        return new PlacedOrderResponse(order.getId(), order.getOrderCode(), payment.getPaymentMethod(), order.getPaymentStatus(),
                order.getSubtotal(), order.getDiscountAmount(), order.getTotalAmount(), order.getCreatedAt(), responseSubOrders);
    }

    private void validateCoupon(Coupon coupon, String code) {
        OffsetDateTime now = OffsetDateTime.now();
        if (coupon.getStatus() != CouponStatus.ACTIVE) throw new IllegalArgumentException("Coupon '" + code + "' hiện không hoạt động.");
        if (coupon.getStartAt() != null && now.isBefore(coupon.getStartAt())) throw new IllegalArgumentException("Coupon '" + code + "' chưa đến thời gian áp dụng.");
        if (coupon.getEndAt() != null && now.isAfter(coupon.getEndAt())) throw new IllegalArgumentException("Coupon '" + code + "' đã hết hạn.");
        if (coupon.getUsageLimit() != null && safeInt(coupon.getUsedCount()) >= coupon.getUsageLimit()) throw new IllegalArgumentException("Coupon '" + code + "' đã hết lượt sử dụng.");
        if (coupon.getDiscountValue() == null || coupon.getDiscountValue() <= 0 || coupon.getDiscountType() == null)
            throw new IllegalArgumentException("Coupon '" + code + "' chưa được cấu hình mức giảm hợp lệ.");
    }

    private void validateMinimum(Coupon coupon, BigDecimal base) {
        if (coupon.getMinimumOrder() != null && base.compareTo(money(coupon.getMinimumOrder())) < 0)
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của coupon '" + coupon.getCode() + "'.");
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal base) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = base.multiply(BigDecimal.valueOf(coupon.getDiscountValue())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscount() != null) discount = discount.min(money(coupon.getMaximumDiscount()));
        } else discount = BigDecimal.valueOf(coupon.getDiscountValue()).setScale(2, RoundingMode.HALF_UP);
        return money(discount.min(base).max(ZERO));
    }

    private void redeem(Coupon coupon, User user, Order order, SubOrder subOrder) {
        coupon.setUsedCount(safeInt(coupon.getUsedCount()) + 1); couponRepository.save(coupon);
        CouponRedemption redemption = new CouponRedemption(); redemption.setCoupon(coupon); redemption.setUser(user);
        redemption.setOrder(order); redemption.setSubOrder(subOrder); couponRedemptionRepository.save(redemption);
    }

    private void saveCouponAllocation(Coupon coupon, Order order, SubOrder subOrder, BigDecimal amount) {
        OrderCouponAllocation allocation = new OrderCouponAllocation(); allocation.setCoupon(coupon); allocation.setOrder(order);
        allocation.setSubOrder(subOrder); allocation.setDiscountAmount(money(amount)); couponAllocationRepository.save(allocation);
    }

    /** Proportional allocation with the final line receiving the cent remainder, so sums reconcile exactly. */
    private List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        if (weights.isEmpty()) return List.of();
        BigDecimal totalWeight = weights.stream().map(this::money).reduce(ZERO, BigDecimal::add);
        List<BigDecimal> result = new ArrayList<>(Collections.nCopies(weights.size(), ZERO));
        if (totalWeight.signum() <= 0 || money(amount).signum() <= 0) return result;
        int lastPositive = -1;
        for (int i = 0; i < weights.size(); i++) if (money(weights.get(i)).signum() > 0) lastPositive = i;
        BigDecimal remaining = money(amount);
        for (int i = 0; i < weights.size(); i++) {
            BigDecimal weight = money(weights.get(i));
            if (weight.signum() <= 0) continue;
            BigDecimal part = i == lastPositive ? remaining
                    : money(amount.multiply(weight).divide(totalWeight, 2, RoundingMode.DOWN));
            if (part.compareTo(remaining) > 0) part = remaining;
            result.set(i, money(part));
            remaining = money(remaining.subtract(part));
        }
        return result;
    }

    private OrderStatusHistory createInitialHistory(SubOrder subOrder, User customer) {
        OrderStatusHistory history = new OrderStatusHistory(); history.setSubOrder(subOrder); history.setOldStatus(null);
        history.setNewStatus(OrderStatus.PENDING); history.setChangedBy(customer); history.setNote("Đơn hàng được tạo."); return history;
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private int safeInt(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP); }
}
