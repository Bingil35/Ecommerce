package com.example.backend.service;

import com.example.backend.dto.request.CreateProductReviewRequest;
import com.example.backend.dto.response.ProductReviewResponse;
import com.example.backend.dto.response.ProductReviewLikeResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final VariantOptionValueRepository variantOptions;
    private final ProductReviewLikeRepository likes;
    private final UserRoleRepository userRoles;

    public ProductReviewService(ProductRepository products, ProductReviewRepository reviews, ReviewReplyRepository replies,
            UserRepository users, ShopRepository shops, SubOrderRepository subOrders, OrderDetailRepository orderDetails,
            ProductVariantRepository variants, VariantOptionValueRepository variantOptions, ProductReviewLikeRepository likes, UserRoleRepository userRoles) {
        this.products = products; this.reviews = reviews; this.replies = replies; this.users = users; this.shops = shops;
        this.subOrders = subOrders; this.orderDetails = orderDetails; this.variants = variants; this.variantOptions = variantOptions;
        this.likes = likes; this.userRoles = userRoles;
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> productReviews(UUID productId, String email) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Không tìm thấy sản phẩm.");
        List<ProductReview> productReviews = reviews.findByProduct_IdOrderByCreatedAtDesc(productId);
        Map<UUID, Long> likeCounts = likes.countByProductIdGrouped(productId).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        User viewer = customerUser(email).orElse(null);
        Set<UUID> likedReviewIds = viewer == null ? Set.of()
                : Set.copyOf(likes.findLikedReviewIds(productId, viewer.getId()));
        return productReviews.stream()
                .map(review -> toResponse(review, likeCounts.getOrDefault(review.getId(), 0L), likedReviewIds.contains(review.getId())))
                .toList();
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

    @Transactional
    public ProductReviewLikeResponse like(String email, UUID reviewId) {
        User user = requireCustomer(email);
        ProductReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        if (!likes.existsByReview_IdAndUser_Id(reviewId, user.getId())) {
            ProductReviewLike like = new ProductReviewLike();
            like.setReview(review);
            like.setUser(user);
            likes.save(like);
        }
        return new ProductReviewLikeResponse(true, likes.countByReview_Id(reviewId));
    }

    @Transactional
    public ProductReviewLikeResponse unlike(String email, UUID reviewId) {
        User user = requireCustomer(email);
        if (!reviews.existsById(reviewId)) throw new ResourceNotFoundException("Review not found.");
        likes.deleteByReview_IdAndUser_Id(reviewId, user.getId());
        return new ProductReviewLikeResponse(false, likes.countByReview_Id(reviewId));
    }

    private User requireCustomer(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (!userRoles.findRoleNamesByUserId(user.getId()).contains("CUSTOMER"))
            throw new AccessDeniedException("Only customers can like product reviews.");
        return user;
    }

    private java.util.Optional<User> customerUser(String email) {
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) return java.util.Optional.empty();
        return users.findByEmail(email).filter(user -> userRoles.findRoleNamesByUserId(user.getId()).contains("CUSTOMER"));
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        return toResponse(review, likes.countByReview_Id(review.getId()), false);
    }

    private ProductReviewResponse toResponse(ProductReview review, long likeCount, boolean likedByMe) {
        List<ProductReviewResponse.Reply> replyViews = replies.findByReview_IdOrderByCreatedAtAsc(review.getId()).stream()
                .map(reply -> new ProductReviewResponse.Reply(reply.getId(), reply.getContent(), reply.getCreatedAt())).toList();
        ProductVariant variant = review.getVariant();
        List<String> optionViews = variant == null ? List.of() : variantOptions.findWithOptionsByVariantIdIn(List.of(variant.getId())).stream()
                .map(link -> link.getOptionValue().getProductOption().getName() + ": " + link.getOptionValue().getValue()).toList();
        String imageUrl = variant != null && variant.getPrimaryMedia() != null
                ? variant.getPrimaryMedia().getUrl() : review.getProduct().getThumbnailUrl();
        UUID subOrderId = review.getSubOrder() == null ? null : review.getSubOrder().getId();
        String orderCode = review.getSubOrder() == null || review.getSubOrder().getOrder() == null
                ? null : review.getSubOrder().getOrder().getOrderCode();
        return new ProductReviewResponse(review.getId(), review.getProduct().getId(), review.getProduct().getName(),
                review.getUser().getFullName(), review.getRating(), review.getContent(), review.getCreatedAt(), likeCount, likedByMe,
                subOrderId, orderCode, imageUrl, optionViews, replyViews);
    }
}
