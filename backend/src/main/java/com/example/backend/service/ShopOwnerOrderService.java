package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderStatusHistoryRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.SubOrderRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.OrderDetailRepository;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ShopOwnerOrderResponse;
import com.example.backend.dto.response.OrderDetailResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

@Service
public class ShopOwnerOrderService {
    private static final Set<OrderStatus> FORWARD = Set.of(OrderStatus.CONFIRMED);
    private final UserRepository users; private final SubOrderRepository subOrders; private final OrderStatusHistoryRepository histories; private final OrderRepository orders; private final OrderDetailRepository orderDetails;
    private final NotificationService notificationService;
    public ShopOwnerOrderService(UserRepository users, SubOrderRepository subOrders, OrderStatusHistoryRepository histories, OrderRepository orders, OrderDetailRepository orderDetails, NotificationService notificationService) {
        this.users=users; this.subOrders=subOrders; this.histories=histories; this.orders=orders; this.orderDetails=orderDetails;
        this.notificationService=notificationService;
    }
    @Transactional(readOnly = true)
    public PageResponse<ShopOwnerOrderResponse> list(String email, OrderStatus status, String keyword, int page, int size) {
        User owner=users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        Specification<SubOrder> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.join("shop").get("ownerUserId"), owner.getId()));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (!normalizedKeyword.isBlank()) {
                var order = root.join("order");
                var customer = order.join("user");
                String pattern = "%" + normalizedKeyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(order.get("orderCode")), pattern, '\\'),
                        cb.like(cb.lower(customer.get("fullName")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var results=subOrders.findAll(filter,
                PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size),50), Sort.by(Sort.Direction.DESC,"createdAt")));
        return PageResponse.from(results, sub -> {
            OrderAddress a=sub.getOrderAddress();
            var address=a==null?null:new OrderDetailResponse.AddressSnapshot(a.getRecipientName(),a.getRecipientPhone(),a.getRecipientCity(),a.getWard(),a.getAddressDetail());
            var history=histories.findBySubOrder_IdOrderByCreatedAtAsc(sub.getId()).stream().map(h->new OrderDetailResponse.StatusHistoryView(h.getOldStatus(),h.getNewStatus(),h.getNote(),h.getCreatedAt())).toList();
            var lines=orderDetails.findBySubOrder_Id(sub.getId()).stream().map(d->new OrderDetailResponse.OrderLineView(d.getId(),d.getVariant()==null?null:d.getVariant().getProduct().getId(),d.getVariant()==null?null:d.getVariant().getId(),d.getProductName(),d.getPrice(),d.getQuantity(),d.getDiscountAmount(),d.getTotalAmount())).toList();
            return new ShopOwnerOrderResponse(sub.getId(),sub.getOrder().getOrderCode(),sub.getOrder().getUser().getFullName(),sub.getStatus(),sub.getOrder().getPayment()==null?null:sub.getOrder().getPayment().getPaymentMethod(),sub.getOrder().getPaymentStatus(),sub.getSubtotal(),sub.getDiscountAmount(),sub.getTotalAmount(),sub.getCreatedAt(),address,history,lines);
        });
    }
    @Transactional(readOnly = true)
    public ShopOwnerOrderResponse get(String email, UUID subOrderId) {
        User owner=users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Account not found."));
        SubOrder sub=subOrders.findByIdAndShopOwner(subOrderId,owner.getId()).orElseThrow(()->new ResourceNotFoundException("Shop order not found."));
        OrderAddress a=sub.getOrderAddress();
        var address=a==null?null:new OrderDetailResponse.AddressSnapshot(a.getRecipientName(),a.getRecipientPhone(),a.getRecipientCity(),a.getWard(),a.getAddressDetail());
        var history=histories.findBySubOrder_IdOrderByCreatedAtAsc(sub.getId()).stream().map(h->new OrderDetailResponse.StatusHistoryView(h.getOldStatus(),h.getNewStatus(),h.getNote(),h.getCreatedAt())).toList();
        var lines=orderDetails.findBySubOrder_Id(sub.getId()).stream().map(d->new OrderDetailResponse.OrderLineView(d.getId(),d.getVariant()==null?null:d.getVariant().getProduct().getId(),d.getVariant()==null?null:d.getVariant().getId(),d.getProductName(),d.getPrice(),d.getQuantity(),d.getDiscountAmount(),d.getTotalAmount())).toList();
        return new ShopOwnerOrderResponse(sub.getId(),sub.getOrder().getOrderCode(),sub.getOrder().getUser().getFullName(),sub.getStatus(),sub.getOrder().getPayment()==null?null:sub.getOrder().getPayment().getPaymentMethod(),sub.getOrder().getPaymentStatus(),sub.getSubtotal(),sub.getDiscountAmount(),sub.getTotalAmount(),sub.getCreatedAt(),address,history,lines);
    }
    @Transactional
    public void updateStatus(String email, UUID subOrderId, OrderStatus next, String note) {
        User owner=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if(owner.getStatus()!=UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không hoạt động.");
        SubOrder sub=subOrders.findByIdAndShopOwnerForUpdate(subOrderId,owner.getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng của cửa hàng."));
        OrderStatus current=sub.getStatus();
        if(!FORWARD.contains(next)) throw new IllegalArgumentException("Shop owners may only confirm orders.");
        if(current!=OrderStatus.PENDING) throw new IllegalArgumentException("Only PENDING orders can be confirmed.");
        OrderStatus actualNext=OrderStatus.SHIPPING;
        sub.setStatus(actualNext);
        OrderStatusHistory history=new OrderStatusHistory(); history.setSubOrder(sub); history.setOldStatus(current);
        history.setNewStatus(actualNext); history.setChangedBy(owner); history.setNote(note==null||note.isBlank()?"Shop confirmed the order; shipment is in progress.":note.trim().substring(0,Math.min(500,note.trim().length())));
        histories.save(history);
        notificationService.notifyUser(sub.getOrder().getUser().getId(), "Đơn hàng đang được giao", "Cửa hàng đã xác nhận đơn " + sub.getOrder().getOrderCode() + ".", NotificationType.ORDER, sub.getOrder().getId());
    }
}
