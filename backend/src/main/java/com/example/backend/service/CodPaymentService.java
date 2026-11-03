package com.example.backend.service;

import com.example.backend.dto.response.PaymentConfirmationResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.SubOrderRepository;
import com.example.backend.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodPaymentService {
    private final UserRepository users; private final OrderRepository orders; private final SubOrderRepository subOrders;
    public CodPaymentService(UserRepository users, OrderRepository orders, SubOrderRepository subOrders) {
        this.users=users; this.orders=orders; this.subOrders=subOrders;
    }
    @Transactional
    public PaymentConfirmationResponse confirmReceived(String email, UUID orderId) {
        User user=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Order order=orders.findByIdAndUserIdForUpdate(orderId,user.getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if(order.getPayment()==null || order.getPayment().getPaymentMethod()!=PaymentMethod.COD)
            throw new IllegalArgumentException("Đơn hàng không sử dụng phương thức COD.");
        if(order.getPaymentStatus()==PaymentStatus.PAID)
            return new PaymentConfirmationResponse(order.getId(),order.getOrderCode(),order.getPaymentStatus(),order.getUpdatedAt(),"Đơn hàng đã được xác nhận thanh toán COD trước đó.");
        if(order.getPaymentStatus()!=PaymentStatus.PENDING) throw new IllegalArgumentException("Trạng thái thanh toán không cho phép xác nhận COD.");
        List<SubOrder> children=subOrders.findByOrder_IdOrderByCreatedAtAsc(orderId);
        List<SubOrder> payableChildren=children.stream().filter(s->s.getStatus()!=OrderStatus.CANCELLED).toList();
        if(payableChildren.isEmpty() || payableChildren.stream().anyMatch(s->s.getStatus()!=OrderStatus.DELIVERED))
            throw new IllegalArgumentException("Chỉ xác nhận COD sau khi tất cả đơn con chưa hủy đã giao thành công.");
        order.setPaymentStatus(PaymentStatus.PAID); orders.save(order);
        return new PaymentConfirmationResponse(order.getId(),order.getOrderCode(),PaymentStatus.PAID,OffsetDateTime.now(),"Đã xác nhận khách hàng nhận đủ hàng và thanh toán COD.");
    }
}
