package com.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.CartItemResponse;
import com.example.backend.dto.response.CartResponse;
import com.example.backend.dto.response.ProductDetailResponse;
import com.example.backend.dto.response.SubCartResponse;
import com.example.backend.entity.Cart;
import com.example.backend.entity.CartDetail;
import com.example.backend.entity.Inventory;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.SubCart;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.entity.VariantOptionValue;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CartDetailRepository;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.SubCartRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VariantOptionValueRepository;

/**
 * Shopping cart business rules.
 *
 * Model: cart (one per customer) -> subcart (one per shop) -> cart detail (one per variant).
 *
 * Invariants:
 * - Prices are always read from ProductVariant.price. The client never supplies a price.
 * - Stock is checked against free stock = max(0, quantity - reservedQuantity), the same
 *   formula the catalog uses. Adding to the cart never changes Inventory.
 * - Every read or mutation is scoped to the authenticated customer's own cart.
 * - Every mutation takes a pessimistic lock on the customer row first, so concurrent
 *   requests from one customer are serialized. The unique constraints remain as a backstop.
 */
@Service
public class CartService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final SubCartRepository subCartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;
    private final ShopRepository shopRepository;

    public CartService(
            UserRepository userRepository,
            CartRepository cartRepository,
            SubCartRepository subCartRepository,
            CartDetailRepository cartDetailRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            InventoryRepository inventoryRepository,
            VariantOptionValueRepository variantOptionValueRepository,
            ShopRepository shopRepository
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.subCartRepository = subCartRepository;
        this.cartDetailRepository = cartDetailRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.variantOptionValueRepository = variantOptionValueRepository;
        this.shopRepository = shopRepository;
    }

    /** Result of an add. newItem is true when a new cart line was created, false when quantity was merged. */
    public record AddItemResult(CartResponse cart, boolean newItem) {
    }

    // =====================================================
    // READ
    // =====================================================

    /** Returns the customer's cart. Does not create a cart row for a customer who has none. */
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        User customer = findActiveCustomer(email);

        return cartRepository.findByUserId(customer.getId())
                .map(this::buildCartResponse)
                .orElseGet(() -> CartResponse.empty(null));
    }

    // =====================================================
    // ADD
    // =====================================================

    /**
     * Adds a variant to the customer's cart. The line goes into the subcart of the
     * variant's shop, which is created on first use. If the variant is already in that
     * subcart, its quantity is increased instead of creating a duplicate line.
     */
    @Transactional
    public AddItemResult addItem(String email, AddToCartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (request.getVariantId() == null) {
            throw new IllegalArgumentException(
                    "Variant ID is required. Products without variants cannot be purchased."
            );
        }
        int quantity = requirePositiveQuantity(request.getQuantity());

        User customer = lockActiveCustomer(email);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));

        if (variant.getProduct() == null
                || !product.getId().equals(variant.getProduct().getId())) {
            throw new IllegalArgumentException("Variant does not belong to the requested product");
        }

        Shop shop = assertPurchasable(product, variant);

        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseGet(() -> cartRepository.save(new Cart(customer)));

        SubCart subCart = subCartRepository.findByCartIdAndShopId(cart.getId(), shop.getId())
                .orElseGet(() -> subCartRepository.save(new SubCart(cart, shop)));

        Optional<CartDetail> existing =
                cartDetailRepository.findBySubCartIdAndVariantId(subCart.getId(), variant.getId());

        long currentQuantity = existing.map(line -> (long) line.getQuantity()).orElse(0L);
        long requestedTotal = currentQuantity + quantity;

        int freeStock = findFreeStock(variant.getId());
        if (requestedTotal > freeStock) {
            throw insufficientStock(freeStock);
        }

        boolean newItem;
        if (existing.isPresent()) {
            existing.get().setQuantity((int) requestedTotal);
            newItem = false;
        } else {
            cartDetailRepository.save(new CartDetail(subCart, variant, quantity));
            newItem = true;
        }

        cart.touch();
        return new AddItemResult(buildCartResponse(cart), newItem);
    }

    // =====================================================
    // UPDATE QUANTITY
    // =====================================================

    /** Replaces the quantity of one of the customer's own lines. Zero is rejected; use remove instead. */
    @Transactional
    public CartResponse updateItemQuantity(String email, UUID detailId, UpdateCartItemRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        int quantity = requirePositiveQuantity(request.getQuantity());

        User customer = lockActiveCustomer(email);
        CartDetail detail = findOwnedDetail(detailId, customer);

        ProductVariant variant = detail.getVariant();
        assertPurchasable(variant.getProduct(), variant);

        int freeStock = findFreeStock(variant.getId());
        if (quantity > freeStock) {
            throw insufficientStock(freeStock);
        }

        detail.setQuantity(quantity);

        Cart cart = detail.getSubCart().getCart();
        cart.touch();
        return buildCartResponse(cart);
    }

    // =====================================================
    // REMOVE
    // =====================================================

    /**
     * Removes one of the customer's own lines. If that was the last line of a shop,
     * the shop's subcart is removed too.
     */
    @Transactional
    public CartResponse removeItem(String email, UUID detailId) {
        User customer = lockActiveCustomer(email);
        CartDetail detail = findOwnedDetail(detailId, customer);

        SubCart subCart = detail.getSubCart();
        Cart cart = subCart.getCart();

        cartDetailRepository.delete(detail);
        if (!cartDetailRepository.existsBySubCartId(subCart.getId())) {
            subCartRepository.delete(subCart);
        }
        cart.touch();

        return buildCartResponse(cart);
    }

    // =====================================================
    // VARIANT DELETION (called by ProductVariantService)
    // =====================================================

    /**
     * Removes a variant from every cart before the variant itself is deleted. Subcarts
     * left with no lines are removed too. Other lines and subcarts are untouched.
     */
    @Transactional
    public void removeVariantFromAllCarts(UUID variantId) {
        List<SubCart> affected = cartDetailRepository.findSubCartsByVariantId(variantId);

        cartDetailRepository.deleteByVariantId(variantId);

        for (SubCart subCart : affected) {
            if (!cartDetailRepository.existsBySubCartId(subCart.getId())) {
                subCartRepository.delete(subCart);
            }
        }
    }

    // =====================================================
    // RESPONSE ASSEMBLY
    // =====================================================

    /**
     * Builds the cart response with a fixed number of queries regardless of cart size:
     * one for lines (with subcart, shop, variant, product and primary media), one for
     * stock and one for option values. Every field the storefront needs to render a line
     * (variant name and options, image, slugs, prices) is filled here, so the client
     * never has to look up product details to show the cart.
     */
    private CartResponse buildCartResponse(Cart cart) {
        List<CartDetail> lines = cartDetailRepository.findDetailedByCartId(cart.getId());
        if (lines.isEmpty()) {
            return CartResponse.empty(cart.getId());
        }

        List<UUID> variantIds = lines.stream()
                .map(line -> line.getVariant().getId())
                .distinct()
                .toList();

        Map<UUID, Integer> freeStockByVariant = loadFreeStock(variantIds);
        Map<UUID, List<ProductDetailResponse.SelectedOption>> optionsByVariant = loadSelectedOptions(variantIds);

        Map<UUID, ShopGroup> groups = new LinkedHashMap<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO.setScale(MONEY_SCALE);

        for (CartDetail line : lines) {
            ProductVariant variant = line.getVariant();
            Product product = variant.getProduct();
            SubCart subCart = line.getSubCart();
            Shop shop = subCart.getShop();

            int quantity = line.getQuantity();
            int freeStock = freeStockByVariant.getOrDefault(variant.getId(), 0);
            BigDecimal unitPrice = variant.getPrice();

            BigDecimal lineSubtotal = unitPrice == null
                    ? null
                    : unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            boolean available = isPurchasable(product, variant, shop) && freeStock >= quantity;

            List<ProductDetailResponse.SelectedOption> selectedOptions =
                    optionsByVariant.getOrDefault(variant.getId(), List.of());

            CartItemResponse item = new CartItemResponse(
                    line.getId(),
                    product.getId(),
                    product.getSlug(),
                    product.getName(),
                    variant.getId(),
                    variantNameOf(selectedOptions),
                    selectedOptions,
                    variant.getSku(),
                    imageUrlOf(variant, product),
                    unitPrice,
                    variant.getComparePrice(),
                    quantity,
                    lineSubtotal,
                    freeStock,
                    available
            );

            ShopGroup group = groups.computeIfAbsent(subCart.getId(), id -> new ShopGroup(id, shop));
            group.items.add(item);

            if (available) {
                group.totalQuantity += quantity;
                group.subtotal = group.subtotal.add(lineSubtotal);
                totalQuantity += quantity;
                subtotal = subtotal.add(lineSubtotal);
            }
        }

        List<SubCartResponse> shops = groups.values().stream()
                .map(group -> new SubCartResponse(
                        group.subCartId,
                        group.shop.getId(),
                        group.shop.getSlug(),
                        group.shop.getShopName(),
                        group.items,
                        group.totalQuantity,
                        group.subtotal
                ))
                .toList();

        return new CartResponse(cart.getId(), shops, totalQuantity, subtotal);
    }

    /** Accumulates the lines of one shop while the response is being built. */
    private static final class ShopGroup {
        final UUID subCartId;
        final Shop shop;
        final List<CartItemResponse> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO.setScale(MONEY_SCALE);

        ShopGroup(UUID subCartId, Shop shop) {
            this.subCartId = subCartId;
            this.shop = shop;
        }
    }

    private Map<UUID, Integer> loadFreeStock(Collection<UUID> variantIds) {
        Map<UUID, Integer> result = new HashMap<>();
        for (Inventory inventory : inventoryRepository.findByVariantIdIn(variantIds)) {
            result.putIfAbsent(inventory.getVariant().getId(), freeStockOf(inventory));
        }
        return result;
    }

    private Map<UUID, List<ProductDetailResponse.SelectedOption>> loadSelectedOptions(Collection<UUID> variantIds) {
        Map<UUID, List<ProductDetailResponse.SelectedOption>> result = new HashMap<>();
        for (VariantOptionValue link : variantOptionValueRepository.findWithOptionsByVariantIdIn(variantIds)) {
            result.computeIfAbsent(link.getId().getVariantId(), key -> new ArrayList<>())
                    .add(new ProductDetailResponse.SelectedOption(link.getOptionValue()));
        }
        return result;
    }

    // =====================================================
    // PURCHASABILITY RULES
    // =====================================================

    /**
     * Hard check used on mutations. Returns the shop, which the caller needs to choose
     * the subcart. Throws with a message the customer can act on.
     */
    private Shop assertPurchasable(Product product, ProductVariant variant) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Product is not available for purchase.");
        }
        if (variant.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Product variant is not available for purchase.");
        }
        if (variant.getPrice() == null) {
            throw new IllegalArgumentException("Product variant has no price and cannot be purchased.");
        }
        Shop shop = product.getShopId() == null
                ? null
                : shopRepository.findById(product.getShopId()).orElse(null);
        if (shop == null || shop.getStatus() != ShopStatus.ACTIVE) {
            throw new IllegalArgumentException("Shop is not available for purchase.");
        }
        return shop;
    }

    /** Soft check used on reads. Mirrors assertPurchasable without throwing. */
    private boolean isPurchasable(Product product, ProductVariant variant, Shop shop) {
        return product.getStatus() == ProductStatus.ACTIVE
                && variant.getStatus() == ProductStatus.ACTIVE
                && variant.getPrice() != null
                && shop != null
                && shop.getStatus() == ShopStatus.ACTIVE;
    }

    // =====================================================
    // CUSTOMER AND OWNERSHIP
    // =====================================================

    private User findActiveCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertActive(user);
        return user;
    }

    /** Resolves the caller and takes the row lock that serializes this customer's cart mutations. */
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

    /**
     * Finds a line only if it belongs to the caller. A line of another customer returns
     * the same 404 as a missing line, so IDs cannot be probed.
     */
    private CartDetail findOwnedDetail(UUID detailId, User customer) {
        return cartDetailRepository.findOwnedById(detailId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    // =====================================================
    // STOCK AND VALIDATION HELPERS
    // =====================================================

    private int findFreeStock(UUID variantId) {
        return inventoryRepository.findFirstByVariantId(variantId)
                .map(this::freeStockOf)
                .orElse(0);
    }

    private int freeStockOf(Inventory inventory) {
        int quantity = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
        int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        return Math.max(0, quantity - reserved);
    }

    private IllegalArgumentException insufficientStock(int freeStock) {
        return new IllegalArgumentException(
                "Insufficient stock. Available quantity: " + freeStock + "."
        );
    }

    private int requirePositiveQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        return quantity;
    }

    /**
     * Builds the variant name from product_options.name and product_option_values.value,
     * in option order, e.g. "Dung tích: 50ml" or "Màu: Đen, Size: L". Null when the variant
     * has no linked option values (nothing in variant_option_values), because the name
     * cannot be known without that link.
     */
    private String variantNameOf(List<ProductDetailResponse.SelectedOption> selectedOptions) {
        String joined = selectedOptions.stream()
                .filter(option -> option.getValue() != null && !option.getValue().isBlank())
                .map(option -> option.getOptionName() == null || option.getOptionName().isBlank()
                        ? option.getValue()
                        : option.getOptionName() + ": " + option.getValue())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private String imageUrlOf(ProductVariant variant, Product product) {
        String variantImage = variant.getImageUrl();
        return variantImage != null ? variantImage : product.getThumbnailUrl();
    }
}
