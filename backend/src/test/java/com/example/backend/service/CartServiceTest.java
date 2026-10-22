package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.CartItemResponse;
import com.example.backend.dto.response.CartResponse;
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
 * Service-level tests for the cart (cart -> subcart per shop -> cart detail per variant).
 * Repositories are mocked with in-memory stores whose queries filter by cart, shop and
 * owner the same way the real queries do, so ownership and grouping are exercised.
 */
class CartServiceTest {

    private static final String ALICE = "alice@example.com";
    private static final String BOB = "bob@example.com";

    private UserRepository userRepository;
    private CartRepository cartRepository;
    private SubCartRepository subCartRepository;
    private CartDetailRepository cartDetailRepository;
    private ProductRepository productRepository;
    private ProductVariantRepository productVariantRepository;
    private InventoryRepository inventoryRepository;
    private VariantOptionValueRepository variantOptionValueRepository;
    private ShopRepository shopRepository;
    private CartService service;

    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<UUID, Cart> cartsByUserId = new HashMap<>();
    private final List<SubCart> subCarts = new ArrayList<>();
    private final List<CartDetail> details = new ArrayList<>();
    private final Map<UUID, Product> productsById = new HashMap<>();
    private final Map<UUID, ProductVariant> variantsById = new HashMap<>();
    private final Map<UUID, Inventory> inventoryByVariant = new HashMap<>();
    private final Map<UUID, Shop> shopsById = new HashMap<>();

    private Shop shop;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cartRepository = mock(CartRepository.class);
        subCartRepository = mock(SubCartRepository.class);
        cartDetailRepository = mock(CartDetailRepository.class);
        productRepository = mock(ProductRepository.class);
        productVariantRepository = mock(ProductVariantRepository.class);
        inventoryRepository = mock(InventoryRepository.class);
        variantOptionValueRepository = mock(VariantOptionValueRepository.class);
        shopRepository = mock(ShopRepository.class);

        service = new CartService(
                userRepository, cartRepository, subCartRepository, cartDetailRepository,
                productRepository, productVariantRepository, inventoryRepository,
                variantOptionValueRepository, shopRepository
        );

        user(ALICE, UserStatus.ACTIVE);
        user(BOB, UserStatus.ACTIVE);

        shop = newShop("Urban Threads", ShopStatus.ACTIVE);
        product = newProduct(shop, ProductStatus.ACTIVE);
        variant = newVariant(product, "TEE-M", "19.99", ProductStatus.ACTIVE);
        newInventory(variant, 10, 2); // free stock = 8

        stubUsers();
        stubCatalog();
        stubCarts();
    }

    // =====================================================
    // ADD TO CART
    // =====================================================

    @Test
    void addsValidProductToEmptyCartUnderItsShop() {
        CartService.AddItemResult result = addResult(ALICE, product.getId(), variant.getId(), 2);

        assertTrue(result.newItem());
        CartResponse cart = result.cart();
        assertNotNull(cart.getCartId());
        assertEquals(1, cart.getShops().size());
        SubCartResponse group = cart.getShops().get(0);
        assertEquals(shop.getId(), group.getShopId());
        assertEquals("Urban Threads", group.getShopName());
        assertEquals(1, group.getItems().size());
        assertEquals(2, group.getItems().get(0).getQuantity());
        assertMoney("19.99", group.getItems().get(0).getUnitPrice());
        assertMoney("39.98", group.getItems().get(0).getLineSubtotal());
        assertEquals(2, cart.getTotalQuantity());
        assertMoney("39.98", cart.getSubtotal());
        assertEquals(1, subCarts.size());
    }

    @Test
    void addingSameVariantAgainIncreasesExistingLineWithoutDuplicate() {
        add(ALICE, product.getId(), variant.getId(), 2);
        CartService.AddItemResult second = addResult(ALICE, product.getId(), variant.getId(), 3);

        assertFalse(second.newItem(), "merging into an existing line must not report a new line");
        assertEquals(1, lines(second.cart()).size());
        assertEquals(5, lines(second.cart()).get(0).getQuantity());
        assertMoney("99.95", lines(second.cart()).get(0).getLineSubtotal());
        assertEquals(1, details.size());
        verify(cartDetailRepository, times(1)).save(any(CartDetail.class));
    }

    @Test
    void differentVariantsOfTheSameShopShareOneSubCart() {
        ProductVariant large = newVariant(product, "TEE-L", "5.00", ProductStatus.ACTIVE);
        newInventory(large, 4, 0);

        add(ALICE, product.getId(), variant.getId(), 1);
        CartResponse cart = add(ALICE, product.getId(), large.getId(), 2);

        assertEquals(1, cart.getShops().size());
        assertEquals(2, cart.getShops().get(0).getItems().size());
        assertEquals(1, subCarts.size());
        assertEquals(3, cart.getTotalQuantity());
        assertMoney("19.99", lineFor(cart, variant.getId()).getLineSubtotal());
        assertMoney("10.00", lineFor(cart, large.getId()).getLineSubtotal());
        assertMoney("29.99", cart.getSubtotal());
    }

    @Test
    void productsFromTwoShopsAreGroupedSeparatelyAndTotalledTogether() {
        Shop bags = newShop("Bag Store", ShopStatus.ACTIVE);
        Product bag = newProduct(bags, ProductStatus.ACTIVE);
        ProductVariant bagVariant = newVariant(bag, "BAG-1", "30.00", ProductStatus.ACTIVE);
        newInventory(bagVariant, 5, 0);

        add(ALICE, product.getId(), variant.getId(), 2);
        CartResponse cart = add(ALICE, bag.getId(), bagVariant.getId(), 1);

        assertEquals(2, cart.getShops().size());
        assertEquals(2, subCarts.size());
        assertEquals(1, groupFor(cart, shop.getId()).getItems().size());
        assertMoney("39.98", groupFor(cart, shop.getId()).getSubtotal());
        assertMoney("30.00", groupFor(cart, bags.getId()).getSubtotal());
        assertEquals(3, cart.getTotalQuantity());
        assertMoney("69.98", cart.getSubtotal());
    }

    @Test
    void removingTheLastLineOfAShopRemovesThatShopGroup() {
        Shop bags = newShop("Bag Store", ShopStatus.ACTIVE);
        Product bag = newProduct(bags, ProductStatus.ACTIVE);
        ProductVariant bagVariant = newVariant(bag, "BAG-1", "30.00", ProductStatus.ACTIVE);
        newInventory(bagVariant, 5, 0);

        add(ALICE, product.getId(), variant.getId(), 2);
        CartResponse cart = add(ALICE, bag.getId(), bagVariant.getId(), 1);
        UUID bagLineId = lineFor(cart, bagVariant.getId()).getId();

        CartResponse after = service.removeItem(ALICE, bagLineId);

        assertEquals(1, after.getShops().size());
        assertEquals(shop.getId(), after.getShops().get(0).getShopId());
        assertEquals(1, subCarts.size());
        assertMoney("39.98", after.getSubtotal());
    }

    @Test
    void removingOneOfTwoLinesKeepsTheShopGroup() {
        ProductVariant large = newVariant(product, "TEE-L", "5.00", ProductStatus.ACTIVE);
        newInventory(large, 4, 0);
        add(ALICE, product.getId(), variant.getId(), 1);
        CartResponse cart = add(ALICE, product.getId(), large.getId(), 1);

        CartResponse after = service.removeItem(ALICE, lineFor(cart, large.getId()).getId());

        assertEquals(1, after.getShops().size());
        assertEquals(1, after.getShops().get(0).getItems().size());
        assertEquals(1, subCarts.size());
    }

    @Test
    void productWithoutVariantIdIsRejectedBecauseItCannotBeStockedOrPriced() {
        AddToCartRequest request = request(product.getId(), null, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request));
        assertTrue(ex.getMessage().contains("Variant ID is required"));
        assertTrue(details.isEmpty());
    }

    @Test
    void nonexistentProductFails() {
        AddToCartRequest request = request(UUID.randomUUID(), variant.getId(), 1);

        assertThrows(ResourceNotFoundException.class, () -> service.addItem(ALICE, request));
        assertTrue(details.isEmpty());
    }

    @Test
    void nonexistentVariantFails() {
        AddToCartRequest request = request(product.getId(), UUID.randomUUID(), 1);

        assertThrows(ResourceNotFoundException.class, () -> service.addItem(ALICE, request));
        assertTrue(details.isEmpty());
    }

    @Test
    void variantBelongingToAnotherProductIsRejected() {
        Product other = newProduct(shop, ProductStatus.ACTIVE);
        ProductVariant foreign = newVariant(other, "OTHER-M", "9.00", ProductStatus.ACTIVE);
        newInventory(foreign, 5, 0);

        AddToCartRequest request = request(product.getId(), foreign.getId(), 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request));
        assertTrue(ex.getMessage().contains("does not belong"));
        assertTrue(details.isEmpty());
    }

    @Test
    void inactiveProductCannotBeAdded() {
        product.setStatus(ProductStatus.INACTIVE);
        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request(product.getId(), variant.getId(), 1)));
        assertTrue(details.isEmpty());
    }

    @Test
    void inactiveVariantCannotBeAdded() {
        variant.setStatus(ProductStatus.INACTIVE);
        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request(product.getId(), variant.getId(), 1)));
        assertTrue(details.isEmpty());
    }

    @Test
    void productOfClosedShopCannotBeAdded() {
        shop.setStatus(ShopStatus.CLOSED);
        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request(product.getId(), variant.getId(), 1)));
        assertTrue(details.isEmpty());
        assertTrue(subCarts.isEmpty(), "no subcart may be created for a rejected add");
    }

    @Test
    void insufficientStockIsRejectedAndNothingIsPersisted() {
        AddToCartRequest request = request(product.getId(), variant.getId(), 9); // free = 8

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItem(ALICE, request));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertTrue(details.isEmpty());
    }

    @Test
    void insufficientStockCountsQuantityAlreadyInTheCart() {
        add(ALICE, product.getId(), variant.getId(), 5);
        AddToCartRequest request = request(product.getId(), variant.getId(), 4); // 5 + 4 = 9 > 8

        assertThrows(IllegalArgumentException.class, () -> service.addItem(ALICE, request));
        assertEquals(5, details.get(0).getQuantity());
    }

    @Test
    void zeroNullAndNegativeQuantitiesAreRejectedOnAdd() {
        for (Integer bad : new Integer[]{0, -1, null}) {
            AddToCartRequest request = request(product.getId(), variant.getId(), bad);
            assertThrows(IllegalArgumentException.class, () -> service.addItem(ALICE, request),
                    "quantity " + bad + " must be rejected");
        }
        assertTrue(details.isEmpty());
    }

    // =====================================================
    // READ
    // =====================================================

    @Test
    void customerWithNoCartGetsEmptyResponseAndNoCartIsCreated() {
        CartResponse cart = service.getCart(ALICE);

        assertNull(cart.getCartId());
        assertTrue(cart.getShops().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
        assertMoney("0.00", cart.getSubtotal());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void customerCanRetrieveOwnCartWithDisplayFields() {
        add(ALICE, product.getId(), variant.getId(), 2);

        CartItemResponse line = service.getCart(ALICE).getShops().get(0).getItems().get(0);

        assertEquals(product.getId(), line.getProductId());
        assertEquals("Tee", line.getProductName());
        assertEquals(variant.getId(), line.getVariantId());
        assertEquals("TEE-M", line.getSku());
        assertEquals(8, line.getAvailableStock());
        assertTrue(line.getAvailable());
    }

    @Test
    void lineThatBecameUnavailableIsShownButExcludedFromTotals() {
        add(ALICE, product.getId(), variant.getId(), 2);
        product.setStatus(ProductStatus.INACTIVE);

        CartResponse cart = service.getCart(ALICE);

        assertEquals(1, lines(cart).size());
        assertFalse(lines(cart).get(0).getAvailable());
        assertEquals(0, cart.getTotalQuantity());
        assertMoney("0.00", cart.getSubtotal());
        assertMoney("0.00", cart.getShops().get(0).getSubtotal());
    }

    @Test
    void inactiveCustomerIsDenied() {
        usersByEmail.get(ALICE).setStatus(UserStatus.INACTIVE);

        assertThrows(AccessDeniedException.class, () -> service.getCart(ALICE));
        assertThrows(AccessDeniedException.class,
                () -> service.addItem(ALICE, request(product.getId(), variant.getId(), 1)));
    }

    // =====================================================
    // UPDATE QUANTITY
    // =====================================================

    @Test
    void updatingQuantityRecalculatesLineAndCartTotals() {
        add(ALICE, product.getId(), variant.getId(), 2);
        ProductVariant small = newVariant(product, "TEE-S", "5.00", ProductStatus.ACTIVE);
        newInventory(small, 10, 0);
        CartResponse before = add(ALICE, product.getId(), small.getId(), 1);
        assertMoney("44.98", before.getSubtotal());

        UUID lineId = lineFor(before, variant.getId()).getId();
        CartResponse after = service.updateItemQuantity(ALICE, lineId, update(3));

        CartItemResponse updated = lineFor(after, variant.getId());
        assertEquals(3, updated.getQuantity());
        assertMoney("59.97", updated.getLineSubtotal());
        assertEquals(4, after.getTotalQuantity());
        assertMoney("64.97", after.getSubtotal());
    }

    @Test
    void updatingQuantityBeyondFreeStockIsRejected() {
        add(ALICE, product.getId(), variant.getId(), 2);
        UUID lineId = details.get(0).getId();

        assertThrows(IllegalArgumentException.class,
                () -> service.updateItemQuantity(ALICE, lineId, update(9)));
        assertEquals(2, details.get(0).getQuantity());
    }

    @Test
    void zeroAndNegativeQuantitiesAreRejectedOnUpdate() {
        add(ALICE, product.getId(), variant.getId(), 2);
        UUID lineId = details.get(0).getId();

        assertThrows(IllegalArgumentException.class,
                () -> service.updateItemQuantity(ALICE, lineId, update(0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateItemQuantity(ALICE, lineId, update(-3)));
        assertEquals(2, details.get(0).getQuantity());
    }

    // =====================================================
    // REMOVE AND OWNERSHIP
    // =====================================================

    @Test
    void customerCannotModifyAnotherCustomersLine() {
        add(ALICE, product.getId(), variant.getId(), 1);
        UUID alicesLineId = details.get(0).getId();
        add(BOB, product.getId(), variant.getId(), 1);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateItemQuantity(BOB, alicesLineId, update(3)));
        assertThrows(ResourceNotFoundException.class,
                () -> service.removeItem(BOB, alicesLineId));

        assertEquals(2, details.size());
        assertEquals(1, service.getCart(ALICE).getShops().get(0).getItems().get(0).getQuantity());
    }

    @Test
    void removingItemAffectsOnlyTheAuthenticatedCustomersCart() {
        add(ALICE, product.getId(), variant.getId(), 1);
        add(BOB, product.getId(), variant.getId(), 2);
        UUID alicesLineId = service.getCart(ALICE).getShops().get(0).getItems().get(0).getId();

        CartResponse afterRemove = service.removeItem(ALICE, alicesLineId);

        assertTrue(afterRemove.getShops().isEmpty());
        assertEquals(1, service.getCart(BOB).getShops().size());
        assertEquals(2, lines(service.getCart(BOB)).get(0).getQuantity());
    }

    @Test
    void removingAnUnknownLineFails() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.removeItem(ALICE, UUID.randomUUID()));
    }

    // =====================================================
    // VARIANT DELETION
    // =====================================================

    @Test
    void deletingAVariantRemovesOnlyItsLinesAndEmptyShopGroups() {
        ProductVariant large = newVariant(product, "TEE-L", "5.00", ProductStatus.ACTIVE);
        newInventory(large, 4, 0);
        Shop bags = newShop("Bag Store", ShopStatus.ACTIVE);
        Product bag = newProduct(bags, ProductStatus.ACTIVE);
        ProductVariant bagVariant = newVariant(bag, "BAG-1", "30.00", ProductStatus.ACTIVE);
        newInventory(bagVariant, 5, 0);

        add(ALICE, product.getId(), variant.getId(), 1);
        add(ALICE, product.getId(), large.getId(), 1);
        add(ALICE, bag.getId(), bagVariant.getId(), 1);

        service.removeVariantFromAllCarts(variant.getId());
        assertEquals(2, subCarts.size(), "the shop still has another line, so its subcart stays");
        assertEquals(1, lineFor(service.getCart(ALICE), large.getId()).getQuantity());

        service.removeVariantFromAllCarts(bagVariant.getId());
        assertEquals(1, subCarts.size(), "the bag shop has no lines left, so its subcart is removed");
        assertEquals(1, service.getCart(ALICE).getShops().size());
    }

    // =====================================================
    // MONEY AND INVENTORY INTEGRITY
    // =====================================================

    @Test
    void monetaryTotalsAreExactAtScaleTwo() {
        ProductVariant cheap = newVariant(product, "CHEAP", "0.10", ProductStatus.ACTIVE);
        ProductVariant cheap2 = newVariant(product, "CHEAP2", "0.20", ProductStatus.ACTIVE);
        newInventory(cheap, 10, 0);
        newInventory(cheap2, 10, 0);

        add(ALICE, product.getId(), cheap.getId(), 1);
        CartResponse cart = add(ALICE, product.getId(), cheap2.getId(), 1);

        assertMoney("0.30", cart.getSubtotal());
        assertEquals(2, cart.getSubtotal().scale());
    }

    @Test
    void addingToCartDoesNotChangeInventory() {
        add(ALICE, product.getId(), variant.getId(), 3);
        add(ALICE, product.getId(), variant.getId(), 2);

        Inventory inventory = inventoryByVariant.get(variant.getId());
        assertEquals(10, inventory.getQuantity());
        assertEquals(2, inventory.getReservedQuantity());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void addingExactlyTheFreeStockLeavesPhysicalAndReservedStockUnchanged() {
        CartResponse cart = add(ALICE, product.getId(), variant.getId(), 8);

        assertEquals(8, cart.getTotalQuantity());
        Inventory inventory = inventoryByVariant.get(variant.getId());
        assertEquals(10, inventory.getQuantity());
        assertEquals(2, inventory.getReservedQuantity());
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private CartService.AddItemResult addResult(String email, UUID productId, UUID variantId, Integer qty) {
        return service.addItem(email, request(productId, variantId, qty));
    }

    private CartResponse add(String email, UUID productId, UUID variantId, Integer qty) {
        return addResult(email, productId, variantId, qty).cart();
    }

    private static AddToCartRequest request(UUID productId, UUID variantId, Integer qty) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setVariantId(variantId);
        request.setQuantity(qty);
        return request;
    }

    private static UpdateCartItemRequest update(Integer qty) {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(qty);
        return request;
    }

    /** All lines of the cart across every shop group. */
    private static List<CartItemResponse> lines(CartResponse cart) {
        return cart.getShops().stream()
                .flatMap(group -> group.getItems().stream())
                .toList();
    }

    private static CartItemResponse lineFor(CartResponse cart, UUID variantId) {
        return lines(cart).stream()
                .filter(line -> line.getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow();
    }

    private static SubCartResponse groupFor(CartResponse cart, UUID shopId) {
        return cart.getShops().stream()
                .filter(group -> group.getShopId().equals(shopId))
                .findFirst()
                .orElseThrow();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual, "money value must not be null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    private User user(String email, UserStatus status) {
        User user = new User();
        setField(user, "id", UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test");
        user.setStatus(status);
        usersByEmail.put(email, user);
        return user;
    }

    private Shop newShop(String name, ShopStatus status) {
        Shop s = new Shop();
        s.setId(UUID.randomUUID());
        s.setShopName(name);
        s.setStatus(status);
        shopsById.put(s.getId(), s);
        return s;
    }

    private Product newProduct(Shop owner, ProductStatus status) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setShopId(owner.getId());
        p.setName("Tee");
        p.setSlug("tee-" + UUID.randomUUID());
        p.setStatus(status);
        productsById.put(p.getId(), p);
        return p;
    }

    private ProductVariant newVariant(Product owner, String sku, String price, ProductStatus status) {
        ProductVariant v = new ProductVariant();
        UUID id = UUID.randomUUID();
        setField(v, "id", id);
        v.setProduct(owner);
        v.setSku(sku);
        v.setPrice(new BigDecimal(price));
        v.setStatus(status);
        variantsById.put(id, v);
        return v;
    }

    private Inventory newInventory(ProductVariant owner, int quantity, int reserved) {
        Inventory inv = new Inventory();
        inv.setVariant(owner);
        inv.setQuantity(quantity);
        inv.setReservedQuantity(reserved);
        inv.setSoldQuantity(0);
        inventoryByVariant.put(owner.getId(), inv);
        return inv;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- repository fakes that honour the same scoping as the real queries ----

    private void stubUsers() {
        when(userRepository.findByEmail(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(usersByEmail.get(inv.<String>getArgument(0))));
        when(userRepository.findByEmailForUpdate(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(usersByEmail.get(inv.<String>getArgument(0))));
    }

    private void stubCatalog() {
        when(productRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(productsById.get(inv.<UUID>getArgument(0))));
        when(productVariantRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(variantsById.get(inv.<UUID>getArgument(0))));
        when(shopRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(shopsById.get(inv.<UUID>getArgument(0))));
        when(inventoryRepository.findFirstByVariantId(any()))
                .thenAnswer(inv -> Optional.ofNullable(inventoryByVariant.get(inv.<UUID>getArgument(0))));
        when(inventoryRepository.findByVariantIdIn(any())).thenAnswer(inv -> {
            List<Inventory> found = new ArrayList<>();
            for (UUID id : inv.<Iterable<UUID>>getArgument(0)) {
                Inventory i = inventoryByVariant.get(id);
                if (i != null) {
                    found.add(i);
                }
            }
            return found;
        });
        when(variantOptionValueRepository.findWithOptionsByVariantIdIn(any())).thenReturn(List.of());
    }

    private void stubCarts() {
        when(cartRepository.findByUserId(any()))
                .thenAnswer(inv -> Optional.ofNullable(cartsByUserId.get(inv.<UUID>getArgument(0))));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart cart = inv.getArgument(0);
            if (cart.getId() == null) {
                cart.setId(UUID.randomUUID());
            }
            cartsByUserId.put(cart.getUser().getId(), cart);
            return cart;
        });

        when(subCartRepository.findByCartIdAndShopId(any(), any())).thenAnswer(inv -> {
            UUID cartId = inv.getArgument(0);
            UUID shopId = inv.getArgument(1);
            return subCarts.stream()
                    .filter(s -> s.getCart().getId().equals(cartId) && s.getShop().getId().equals(shopId))
                    .findFirst();
        });
        when(subCartRepository.save(any(SubCart.class))).thenAnswer(inv -> {
            SubCart subCart = inv.getArgument(0);
            if (subCart.getId() == null) {
                subCart.setId(UUID.randomUUID());
            }
            subCarts.add(subCart);
            return subCart;
        });
        doAnswer(inv -> {
            SubCart target = inv.getArgument(0);
            subCarts.removeIf(s -> s == target);
            return null;
        }).when(subCartRepository).delete(any(SubCart.class));

        when(cartDetailRepository.findDetailedByCartId(any())).thenAnswer(inv -> {
            UUID cartId = inv.getArgument(0);
            return details.stream()
                    .filter(d -> d.getSubCart().getCart().getId().equals(cartId))
                    .toList();
        });
        when(cartDetailRepository.findOwnedById(any(), any())).thenAnswer(inv -> {
            UUID detailId = inv.getArgument(0);
            UUID userId = inv.getArgument(1);
            return details.stream()
                    .filter(d -> d.getId().equals(detailId)
                            && d.getSubCart().getCart().getUser().getId().equals(userId))
                    .findFirst();
        });
        when(cartDetailRepository.findBySubCartIdAndVariantId(any(), any())).thenAnswer(inv -> {
            UUID subCartId = inv.getArgument(0);
            UUID variantId = inv.getArgument(1);
            return details.stream()
                    .filter(d -> d.getSubCart().getId().equals(subCartId)
                            && d.getVariant().getId().equals(variantId))
                    .findFirst();
        });
        when(cartDetailRepository.findSubCartsByVariantId(any())).thenAnswer(inv -> {
            UUID variantId = inv.getArgument(0);
            return details.stream()
                    .filter(d -> d.getVariant().getId().equals(variantId))
                    .map(CartDetail::getSubCart)
                    .distinct()
                    .toList();
        });
        when(cartDetailRepository.existsBySubCartId(any())).thenAnswer(inv -> {
            UUID subCartId = inv.getArgument(0);
            return details.stream().anyMatch(d -> d.getSubCart().getId().equals(subCartId));
        });
        when(cartDetailRepository.save(any(CartDetail.class))).thenAnswer(inv -> {
            CartDetail line = inv.getArgument(0);
            if (line.getId() == null) {
                line.setId(UUID.randomUUID());
            }
            details.add(line);
            return line;
        });
        doAnswer(inv -> {
            CartDetail target = inv.getArgument(0);
            details.removeIf(d -> d == target);
            return null;
        }).when(cartDetailRepository).delete(any(CartDetail.class));
        doAnswer(inv -> {
            UUID variantId = inv.getArgument(0);
            details.removeIf(d -> d.getVariant().getId().equals(variantId));
            return null;
        }).when(cartDetailRepository).deleteByVariantId(any());
    }
}
