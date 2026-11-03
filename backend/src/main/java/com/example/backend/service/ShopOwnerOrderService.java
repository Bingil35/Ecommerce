package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderStatusHistoryRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.SubOrderRepository;
import com.example.backend.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopOwnerOrderService {
    private static final Set<OrderStatus> FORWARD = Set.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
    private final UserRepository users; private final SubOrderRepository subOrders; private final OrderStatusHistoryRepository histories; private final OrderRepository orders;
    public ShopOwnerOrderService(UserRepository users, SubOrderRepository subOrders, OrderStatusHistoryRepository histories, OrderRepository orders) {
        this.users=users; this.subOrders=subOrders; this.histories=histories; this.orders=orders;
    }
    @Transactional
    public void updateStatus(String email, UUID subOrderId, OrderStatus next, String note) {
        User owner=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if(owner.getStatus()!=UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không hoạt động.");
        SubOrder sub=subOrders.findByIdAndShopOwnerForUpdate(subOrderId,owner.getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng của cửa hàng."));
        OrderStatus current=sub.getStatus();
        if(!FORWARD.contains(next)) throw new IllegalArgumentException("Cửa hàng chỉ được chuyển đơn sang CONFIRMED, SHIPPING hoặc DELIVERED.");
        boolean valid=(current==OrderStatus.PENDING&&next==OrderStatus.CONFIRMED)
                ||(current==OrderStatus.CONFIRMED&&next==OrderStatus.SHIPPING)
                ||(current==OrderStatus.SHIPPING&&next==OrderStatus.DELIVERED);
        if(!valid) throw new IllegalArgumentException("Không thể chuyển trạng thái từ "+current+" sang "+next+".");
        sub.setStatus(next);
        OrderStatusHistory history=new OrderStatusHistory(); history.setSubOrder(sub); history.setOldStatus(current);
        history.setNewStatus(next); history.setChangedBy(owner); history.setNote(note==null||note.isBlank()?"Cửa hàng cập nhật trạng thái đơn hàng.":note.trim().substring(0,Math.min(500,note.trim().length())));
        histories.save(history);
        // COD is considered collected when the shop marks delivery complete. Since PaymentStatus
        // belongs to the parent Order, mark it PAID only after every non-cancelled shop suborder
        // has reached DELIVERED.
        if (next == OrderStatus.DELIVERED && sub.getOrder().getPayment() != null
                && sub.getOrder().getPayment().getPaymentMethod() == PaymentMethod.COD
                && sub.getOrder().getPaymentStatus() == PaymentStatus.PENDING) {
            var siblings = subOrders.findByOrder_IdOrderByCreatedAtAsc(sub.getOrder().getId());
            boolean allDelivered = siblings.stream().filter(x -> x.getStatus() != OrderStatus.CANCELLED)
                    .allMatch(x -> x.getStatus() == OrderStatus.DELIVERED);
            boolean hasDeliverable = siblings.stream().anyMatch(x -> x.getStatus() != OrderStatus.CANCELLED);
            if (hasDeliverable && allDelivered) {
                sub.getOrder().setPaymentStatus(PaymentStatus.PAID);
                orders.save(sub.getOrder());
            }
        }
    }
}
