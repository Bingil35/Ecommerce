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
    private final VariantOptionValueRepository variantOptionValueRepository;
    public OrderQueryService(UserRepository userRepository, OrderRepository orderRepository, SubOrderRepository subOrderRepository,
            OrderDetailRepository orderDetailRepository, OrderStatusHistoryRepository historyRepository,
            VariantOptionValueRepository variantOptionValueRepository) {
        this.userRepository=userRepository; this.orderRepository=orderRepository; this.subOrderRepository=subOrderRepository;
        this.orderDetailRepository=orderDetailRepository; this.historyRepository=historyRepository;
        this.variantOptionValueRepository=variantOptionValueRepository;
    }
    @Transactional(readOnly=true)
    public PageResponse<OrderListItemResponse> getMyOrders(String email, OrderStatus status, String code,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Specification<SubOrder> filters=(root,query,criteria)->{
            List<Predicate> predicates=new ArrayList<>();
            predicates.add(criteria.equal(root.get("order").get("user").get("id"),user.getId()));
            if(code!=null&&!code.isBlank()) predicates.add(criteria.like(criteria.lower(root.get("order").get("orderCode")),"%"+code.trim().toLowerCase(Locale.ROOT)+"%"));
            if(from!=null) predicates.add(criteria.greaterThanOrEqualTo(root.<OffsetDateTime>get("createdAt"),from));
            if(to!=null) predicates.add(criteria.lessThanOrEqualTo(root.<OffsetDateTime>get("createdAt"),to));
            if(status!=null) predicates.add(criteria.equal(root.<OrderStatus>get("status"),status));
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
        Page<SubOrder> page=subOrderRepository.findAll(filters,pageable);
        return PageResponse.from(page,OrderListItemResponse::from);
    }
    @Transactional(readOnly=true)
    public OrderDetailResponse getMyOrder(String email, UUID orderId) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Order order=orderRepository.findByIdAndUser_Id(orderId,user.getId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        List<OrderDetailResponse.SubOrderView> children=subOrderRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId()).stream().map(s->{
            List<com.example.backend.entity.OrderDetail> details=orderDetailRepository.findBySubOrder_Id(s.getId());
            List<UUID> variantIds=details.stream().map(d->d.getVariant()==null?null:d.getVariant().getId()).filter(Objects::nonNull).distinct().toList();
            Map<UUID,List<String>> options=new HashMap<>();
            if(!variantIds.isEmpty()) variantOptionValueRepository.findWithOptionsByVariantIdIn(variantIds).forEach(v->
                    options.computeIfAbsent(v.getVariant().getId(),id->new ArrayList<>()).add(v.getOptionValue().getProductOption().getName()+": "+v.getOptionValue().getValue()));
            List<OrderDetailResponse.OrderLineView> lines=details.stream()
                    .map(d->{var variant=d.getVariant(); var product=variant==null?null:variant.getProduct();
                        return new OrderDetailResponse.OrderLineView(d.getId(),product==null?null:product.getId(),variant==null?null:variant.getId(),d.getProductName(),
                                variant==null||variant.getImageUrl()==null?(product==null?null:product.getThumbnailUrl()):variant.getImageUrl(),
                                variant==null?List.of():options.getOrDefault(variant.getId(),List.of()),d.getPrice(),d.getQuantity(),d.getDiscountAmount(),d.getTotalAmount());}).toList();
            List<OrderDetailResponse.StatusHistoryView> history=historyRepository.findBySubOrder_IdOrderByCreatedAtAsc(s.getId()).stream()
                    .map(h->new OrderDetailResponse.StatusHistoryView(h.getOldStatus(),h.getNewStatus(),h.getNote(),h.getCreatedAt())).toList();
            OrderAddress a=s.getOrderAddress();
            OrderDetailResponse.AddressSnapshot address=a==null?null:new OrderDetailResponse.AddressSnapshot(a.getRecipientName(),a.getRecipientPhone(),a.getRecipientCity(),a.getWard(),a.getAddressDetail());
            return new OrderDetailResponse.SubOrderView(s.getId(),s.getShop()==null?null:s.getShop().getId(),s.getShop()==null?null:s.getShop().getShopName(),s.getStatus(),s.getSubtotal(),s.getDiscountAmount(),s.getTotalAmount(),address,history,lines);
        }).toList();
        return new OrderDetailResponse(order.getId(),order.getOrderCode(),order.getPayment()==null?null:order.getPayment().getPaymentMethod(),order.getPaymentStatus(),order.getSubtotal(),order.getDiscountAmount(),order.getTotalAmount(),order.getCreatedAt(),children);
    }

    @Transactional(readOnly=true)
    public OrderDetailResponse getMySubOrder(String email, UUID subOrderId) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
        SubOrder sub=subOrderRepository.findByIdAndOrder_User_Id(subOrderId,user.getId())
                .orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        OrderDetailResponse parent=getMyOrder(email,sub.getOrder().getId());
        return parent;
    }
}
