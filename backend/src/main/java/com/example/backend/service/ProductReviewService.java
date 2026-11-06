package com.example.backend.service;

import com.example.backend.dto.request.CreateProductReviewRequest;
import com.example.backend.dto.response.ProductReviewResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewService {
    private final ProductRepository products;
    private final ProductReviewRepository reviews;
    private final ReviewReplyRepository replies;
    private final UserRepository users;
    private final ShopRepository shops;
    private final SubOrderRepository subOrders;
    private final OrderDetailRepository orderDetails;
    private final ProductVariantRepository variants;

    public ProductReviewService(ProductRepository products, ProductReviewRepository reviews, ReviewReplyRepository replies,
            UserRepository users, ShopRepository shops, SubOrderRepository subOrders, OrderDetailRepository orderDetails,
            ProductVariantRepository variants) {
        this.products = products; this.reviews = reviews; this.replies = replies; this.users = users; this.shops = shops;
        this.subOrders = subOrders; this.orderDetails = orderDetails; this.variants = variants;
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> productReviews(UUID productId) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Không tìm thấy sản phẩm.");
        return reviews.findByProduct_IdOrderByCreatedAtDesc(productId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> ownerReviews(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Shop shop = shops.findByOwnerUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng."));
        return reviews.findByShopId(shop.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductReviewResponse create(String email, UUID productId, CreateProductReviewRequest request) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        SubOrder subOrder = subOrders.findByIdAndOrder_User_Id(request.subOrderId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Đơn hàng không thuộc tài khoản của bạn."));
        if (subOrder.getStatus() != OrderStatus.DELIVERED) throw new IllegalArgumentException("Chỉ có thể đánh giá sau khi đơn hàng đã giao.");
        boolean bought = orderDetails.findBySubOrder_Id(subOrder.getId()).stream().anyMatch(line -> {
            ProductVariant variant = line.getVariant();
            return variant != null && variant.getProduct().getId().equals(productId)
                    && (request.variantId() == null || variant.getId().equals(request.variantId()));
        });
        if (!bought) throw new AccessDeniedException("Sản phẩm này không thuộc đơn hàng đã giao.");
        if (reviews.existsBySubOrder_IdAndProduct_IdAndUser_Id(subOrder.getId(), productId, user.getId()))
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này trong đơn hàng.");
        ProductReview review = new ProductReview(); review.setProduct(product); review.setUser(user); review.setSubOrder(subOrder);
        review.setRating(request.rating()); review.setContent(request.content() == null || request.content().isBlank() ? null : request.content().trim());
        if (request.variantId() != null) review.setVariant(variants.findById(request.variantId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân loại sản phẩm.")));
        reviews.save(review);
        List<ProductReview> all = reviews.findByProduct_IdOrderByCreatedAtDesc(productId);
        product.setRatingCount(all.size());
        product.setRatingAverage(BigDecimal.valueOf(all.stream().mapToInt(r -> r.getRating()).average().orElse(0)).setScale(1, RoundingMode.HALF_UP));
        return toResponse(review);
    }

    @Transactional
    public ProductReviewResponse reply(String email, UUID reviewId, String content) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Shop shop = shops.findByOwnerUserId(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng."));
        ProductReview review = reviews.findByIdAndShopId(reviewId, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá của cửa hàng."));
        ReviewReply reply = replies.findFirstByReview_IdOrderByCreatedAtAsc(reviewId).orElseGet(ReviewReply::new);
        reply.setReview(review); reply.setShopId(shop.getId()); reply.setContent(content.trim()); replies.save(reply);
        return toResponse(review);
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        List<ProductReviewResponse.Reply> replyViews = replies.findByReview_IdOrderByCreatedAtAsc(review.getId()).stream()
                .map(reply -> new ProductReviewResponse.Reply(reply.getId(), reply.getContent(), reply.getCreatedAt())).toList();
        return new ProductReviewResponse(review.getId(), review.getProduct().getId(), review.getProduct().getName(),
                review.getUser().getFullName(), review.getRating(), review.getContent(), review.getCreatedAt(), replyViews);
    }
}
