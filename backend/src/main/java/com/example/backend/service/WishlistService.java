package com.example.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ProductCardResponse;
import com.example.backend.dto.response.WishlistItemResponse;
import com.example.backend.dto.response.WishlistStatusResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.entity.Wishlist;
import com.example.backend.entity.WishlistItem;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WishlistItemRepository;
import com.example.backend.repository.WishlistRepository;

/**
 * Wishlist business rules. Model: wishlist (one per customer) -> wishlist item (one per product).
 *
 * Invariants:
 * - The caller is always resolved from the authenticated principal (email), never from the
 *   path or body, and every query is scoped to that customer.
 * - A product can be added only if the public catalog would show it: it exists, is ACTIVE,
 *   and its shop is ACTIVE. Anything else is reported as "not found", like the catalog does.
 * - Adding is idempotent. Mutations take the same customer row lock the cart uses, so
 *   concurrent adds from one customer run one at a time; the unique constraint on
 *   (wishlist_id, product_id) remains as the backstop.
 * - The list shows only products that are currently visible. Entries whose product or shop
 *   became hidden are kept (they reappear if the product is re-activated) and can still be
 *   removed or checked by product id.
 */
@Service
public class WishlistService {

    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ProductCatalogService productCatalogService;

    public WishlistService(
            UserRepository userRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            ShopRepository shopRepository,
            ProductCatalogService productCatalogService
    ) {
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.productCatalogService = productCatalogService;
    }

    /** Result of an add. created is false when the product was already in the wishlist. */
    public record AddResult(WishlistStatusResponse status, boolean created) {
    }

    // =====================================================
    // ADD
    // =====================================================

    @Transactional
    public AddResult addProduct(String email, UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        User customer = lockActiveCustomer(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(WishlistService::productNotFound);
        assertVisible(product);

        if (wishlistItemRepository.existsOwned(customer.getId(), productId)) {
            return new AddResult(new WishlistStatusResponse(productId, true), false);
        }

        Wishlist wishlist = wishlistRepository.findByUserId(customer.getId())
                .orElseGet(() -> wishlistRepository.save(new Wishlist(customer)));

        wishlistItemRepository.save(new WishlistItem(wishlist, product));
        wishlist.touch();

        return new AddResult(new WishlistStatusResponse(productId, true), true);
    }

    // =====================================================
    // LIST
    // =====================================================

    /**
     * One page of the customer's visible wishlist, newest first. Query count is fixed
     * regardless of page size: one page query (items, products, categories), one count,
     * and the catalog's batch card assembly (variants, stock, fallback media).
     */
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> getWishlist(String email, int page, int size) {
        User customer = findActiveCustomer(email);

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size)
        );

        Page<WishlistItem> items = wishlistItemRepository.findVisibleByUserId(
                customer.getId(), ProductStatus.ACTIVE, ShopStatus.ACTIVE, pageable
        );

        List<Product> products = items.getContent().stream()
                .map(WishlistItem::getProduct)
                .toList();
        Map<UUID, ProductCardResponse> cards = new HashMap<>();
        for (ProductCardResponse card : productCatalogService.toCards(products)) {
            cards.put(card.getId(), card);
        }

        return PageResponse.from(items, item -> new WishlistItemResponse(
                item.getId(),
                item.getCreatedAt(),
                cards.get(item.getProduct().getId())
        ));
    }

    // =====================================================
    // STATUS
    // =====================================================

    /** One indexed existence query; a product that does not exist is simply "not in wishlist". */
    @Transactional(readOnly = true)
    public WishlistStatusResponse getStatus(String email, UUID productId) {
        User customer = findActiveCustomer(email);
        boolean saved = productId != null
                && wishlistItemRepository.existsOwned(customer.getId(), productId);
        return new WishlistStatusResponse(productId, saved);
    }

    // =====================================================
    // REMOVE
    // =====================================================

    /**
     * Removes the caller's entry for a product. Only the wishlist entry is deleted, never the
     * product. Another customer's entry and a missing entry both return the same 404, so
     * ownership cannot be probed. Works for products that have since become hidden.
     */
    @Transactional
    public WishlistStatusResponse removeProduct(String email, UUID productId) {
        User customer = lockActiveCustomer(email);

        WishlistItem item = wishlistItemRepository.findOwned(customer.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        Wishlist wishlist = item.getWishlist();
        wishlistItemRepository.delete(item);
        wishlist.touch();

        return new WishlistStatusResponse(productId, false);
    }

    // =====================================================
    // PRODUCT DELETION (called by ProductService)
    // =====================================================

    /** Removes a product from every wishlist before the product itself is deleted. */
    @Transactional
    public void removeProductFromAllWishlists(UUID productId) {
        wishlistItemRepository.deleteByProductId(productId);
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private void assertVisible(Product product) {
        boolean shopActive = product.getShopId() != null
                && shopRepository.findById(product.getShopId())
                .map(shop -> shop.getStatus() == ShopStatus.ACTIVE)
                .orElse(false);

        if (product.getStatus() != ProductStatus.ACTIVE || !shopActive) {
            throw productNotFound();
        }
    }

    private static ResourceNotFoundException productNotFound() {
        return new ResourceNotFoundException("Không tìm thấy sản phẩm.");
    }

    private User findActiveCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertActive(user);
        return user;
    }

    /** Resolves the caller and takes the row lock that serializes this customer's mutations. */
    private User lockActiveCustomer(String email) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertActive(user);
        return user;
    }

    private void assertActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Customer account is not active.");
        }
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return ProductSearchService.DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, ProductSearchService.MAX_PAGE_SIZE);
    }
}
