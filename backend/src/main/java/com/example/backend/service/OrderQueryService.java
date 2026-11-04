package com.example.backend.service;

import com.example.backend.dto.response.*;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private final UserRepository userRepository; private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository; private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusHistoryRepository historyRepository;
    public OrderQueryService(UserRepository userRepository, OrderRepository orderRepository, SubOrderRepository subOrderRepository,
            OrderDetailRepository orderDetailRepository, OrderStatusHistoryRepository historyRepository) {
        this.userRepository=userRepository; this.orderRepository=orderRepository; this.subOrderRepository=subOrderRepository;
        this.orderDetailRepository=orderDetailRepository; this.historyRepository=historyRepository;
    }
    @Transactional(readOnly=true)
    public PageResponse<OrderListItemResponse> getMyOrders(String email, OrderStatus status, String code,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Specification<Order> filters=(root,query,criteria)->{
            List<Predicate> predicates=new ArrayList<>();
            predicates.add(criteria.equal(root.get("user").get("id"),user.getId()));
            if(code!=null&&!code.isBlank()) predicates.add(criteria.like(criteria.lower(root.get("orderCode")),"%"+code.trim().toLowerCase(Locale.ROOT)+"%"));
            if(from!=null) predicates.add(criteria.greaterThanOrEqualTo(root.<OffsetDateTime>get("createdAt"),from));
            if(to!=null) predicates.add(criteria.lessThanOrEqualTo(root.<OffsetDateTime>get("createdAt"),to));
            if(status!=null){
                var subquery=query.subquery(Integer.class);
                var subOrder=subquery.from(SubOrder.class);
                subquery.select(criteria.literal(1)).where(criteria.equal(subOrder.get("order"),root),criteria.equal(subOrder.<OrderStatus>get("status"),status));
                predicates.add(criteria.exists(subquery));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
        Page<Order> page=orderRepository.findAll(filters,pageable);
        return PageResponse.from(page,o->OrderListItemResponse.from(o,subOrderRepository.findByOrder_IdOrderByCreatedAtAsc(o.getId()).size()));
    }
    @Transactional(readOnly=true)
    public OrderDetailResponse getMyOrder(String email, UUID orderId) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Order order=orderRepository.findByIdAndUser_Id(orderId,user.getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        List<OrderDetailResponse.SubOrderView> children=subOrderRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId()).stream().map(s->{
            List<OrderDetailResponse.OrderLineView> lines=orderDetailRepository.findBySubOrder_Id(s.getId()).stream()
                    .map(d->new OrderDetailResponse.OrderLineView(d.getId(),d.getVariant()==null?null:d.getVariant().getProduct().getId(),d.getVariant()==null?null:d.getVariant().getId(),d.getProductName(),d.getPrice(),d.getQuantity(),d.getDiscountAmount(),d.getTotalAmount())).toList();
            List<OrderDetailResponse.StatusHistoryView> history=historyRepository.findBySubOrder_IdOrderByCreatedAtAsc(s.getId()).stream()
                    .map(h->new OrderDetailResponse.StatusHistoryView(h.getOldStatus(),h.getNewStatus(),h.getNote(),h.getCreatedAt())).toList();
            OrderAddress a=s.getOrderAddress();
            OrderDetailResponse.AddressSnapshot address=a==null?null:new OrderDetailResponse.AddressSnapshot(a.getRecipientName(),a.getRecipientPhone(),a.getRecipientCity(),a.getWard(),a.getAddressDetail());
            return new OrderDetailResponse.SubOrderView(s.getId(),s.getShop()==null?null:s.getShop().getId(),s.getShop()==null?null:s.getShop().getShopName(),s.getStatus(),s.getSubtotal(),s.getDiscountAmount(),s.getTotalAmount(),address,history,lines);
        }).toList();
        return new OrderDetailResponse(order.getId(),order.getOrderCode(),order.getPayment()==null?null:order.getPayment().getPaymentMethod(),order.getPaymentStatus(),order.getSubtotal(),order.getDiscountAmount(),order.getTotalAmount(),order.getCreatedAt(),children);
    }
}
