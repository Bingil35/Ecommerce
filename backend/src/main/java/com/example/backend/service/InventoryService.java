package com.example.backend.service;

import com.example.backend.dto.response.InventoryResponse;
import com.example.backend.entity.Inventory;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductVariant;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.InventoryRepository;
import com.example.backend.repository.ProductVariantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Quản lý tồn kho của shop owner trên bảng Inventories HIỆN CÓ:
 * nhập kho, điều chỉnh, ngưỡng cảnh báo và danh sách tồn thấp.
 *
 * Không có bảng lịch sử biến động trong schema hiện tại, nên mỗi thay đổi số lượng
 * chỉ được ghi nhật ký ứng dụng (log INFO, tiền tố "INVENTORY"), không lưu DB.
 *
 * Service không tự kiểm tra quyền sở hữu; controller phải gọi
 * ProductOwnershipService.assertVariant trước, và truyền shopId lấy từ tài khoản.
 *
 * Mọi thay đổi khóa dòng variant rồi dòng Inventories (PESSIMISTIC_WRITE):
 * bảng Inventories không có unique trên variant_id nên khóa variant bảo đảm
 * không bao giờ tạo trùng dòng tồn kho khi có request đồng thời.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private static final int MAX_STOCK = 100_000_000;
    private static final int MAX_NOTE_LENGTH = 500;

    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;
    private final NotificationService notificationService;
    private final int defaultLowStockThreshold;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductVariantRepository variantRepository,
            NotificationService notificationService,
            @Value("${app.inventory.low-stock-threshold:10}") int defaultLowStockThreshold
    ) {
        this.inventoryRepository = inventoryRepository;
        this.variantRepository = variantRepository;
        this.notificationService = notificationService;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID variantId) {

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(this::variantNotFound);

        Inventory inventory =
                inventoryRepository.findFirstByVariantId(variantId).orElse(null);

        return toResponse(variant, inventory);
    }

    /** Biến thể ACTIVE của shop có tồn khả dụng <= ngưỡng cảnh báo. */
    @Transactional(readOnly = true)
    public Page<InventoryResponse> lowStock(UUID shopId, int page, int size) {

        return inventoryRepository
                .findLowStock(shopId, defaultLowStockThreshold, PageRequest.of(page, size))
                .map(inventory -> toResponse(inventory.getVariant(), inventory));
    }

    @Transactional(readOnly = true)
    public java.util.List<InventoryResponse> listByShop(UUID shopId) {
        var variants = variantRepository.findAllByShopId(shopId);
        var inventories = inventoryRepository.findByVariantIdIn(variants.stream().map(ProductVariant::getId).toList());
        var byVariant = inventories.stream().collect(java.util.stream.Collectors.toMap(i -> i.getVariant().getId(), i -> i, (a, b) -> a));
        return variants.stream().map(v -> toResponse(v, byVariant.get(v.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<InventoryResponse> listByProduct(UUID productId) {
        var variants = variantRepository.findByProductIdOrderByCreatedAtAsc(productId);
        var inventories = inventoryRepository.findByVariantIdIn(variants.stream().map(ProductVariant::getId).toList());
        var byVariant = inventories.stream().collect(java.util.stream.Collectors.toMap(i -> i.getVariant().getId(), i -> i, (a, b) -> a));
        return variants.stream().map(v -> toResponse(v, byVariant.get(v.getId()))).toList();
    }

    // =========================================================
    // STOCK IN / ADJUST / THRESHOLD
    // =========================================================

    @Transactional
    public InventoryResponse stockIn(UUID variantId, Integer quantity, String note) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
        }

        String cleanedNote = cleanNote(note);

        ProductVariant variant = lockVariant(variantId);
        Inventory inventory = lockOrCreate(variant);
        boolean wasLow = toResponse(variant, inventory).lowStock();

        int before = value(inventory.getQuantity());
        long after = (long) before + quantity;

        if (after > MAX_STOCK) {
            throw new IllegalArgumentException("Tồn kho vượt quá giới hạn cho phép.");
        }

        inventory.setQuantity((int) after);
        inventoryRepository.save(inventory);
        notifyLowStockTransition(variant, inventory, wasLow);

        audit("STOCK_IN", variant, before, (int) after, cleanedNote);

        return toResponse(variant, inventory);
    }

    @Transactional
    public InventoryResponse adjust(
            UUID variantId,
            Integer delta,
            Integer newQuantity,
            String reason
    ) {
        if ((delta == null) == (newQuantity == null)) {
            throw new IllegalArgumentException(
                    "Chỉ truyền một trong hai: delta hoặc newQuantity."
            );
        }

        String cleanedReason = cleanNote(reason);

        ProductVariant variant = lockVariant(variantId);
        Inventory inventory = lockOrCreate(variant);
        boolean wasLow = toResponse(variant, inventory).lowStock();

        int before = value(inventory.getQuantity());
        int reserved = value(inventory.getReservedQuantity());

        long target = newQuantity != null
                ? newQuantity
                : (long) before + delta;

        if (target < 0) {
            throw new IllegalArgumentException("Tồn kho sau điều chỉnh không được âm.");
        }

        if (target > MAX_STOCK) {
            throw new IllegalArgumentException("Tồn kho vượt quá giới hạn cho phép.");
        }

        if (target < reserved) {
            throw new IllegalArgumentException(
                    "Tồn kho không được nhỏ hơn số lượng đang được giữ chỗ (" + reserved + ")."
            );
        }

        int after = (int) target;

        if (after == before) {
            throw new IllegalArgumentException("Số lượng tồn kho không thay đổi.");
        }

        inventory.setQuantity(after);
        inventoryRepository.save(inventory);
        notifyLowStockTransition(variant, inventory, wasLow);

        audit("ADJUSTMENT", variant, before, after, cleanedReason);

        return toResponse(variant, inventory);
    }

    /**
     * Đặt ngưỡng cảnh báo riêng cho biến thể.
     * null hoặc 0 = dùng ngưỡng mặc định (cột low_stock_threshold có DEFAULT 0 trong DB,
     * nên 0 được hiểu là "chưa đặt").
     */
    @Transactional
    public InventoryResponse setLowStockThreshold(UUID variantId, Integer threshold) {

        if (threshold != null && (threshold < 0 || threshold > MAX_STOCK)) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo không hợp lệ.");
        }

        ProductVariant variant = lockVariant(variantId);
        Inventory inventory = lockOrCreate(variant);
        boolean wasLow = toResponse(variant, inventory).lowStock();

        inventory.setLowStockThreshold(threshold == null ? 0 : threshold);
        inventoryRepository.save(inventory);
        notifyLowStockTransition(variant, inventory, wasLow);

        return toResponse(variant, inventory);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private ResourceNotFoundException variantNotFound() {
        return new ResourceNotFoundException("Product variant not found");
    }

    private ProductVariant lockVariant(UUID variantId) {
        return variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(this::variantNotFound);
    }

    /** Khóa dòng tồn kho; tạo mới (số lượng 0) nếu biến thể chưa có. */
    private Inventory lockOrCreate(ProductVariant variant) {

        return inventoryRepository.findByVariantIdForUpdate(variant.getId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setVariant(variant);
                    created.setQuantity(0);
                    created.setReservedQuantity(0);
                    created.setSoldQuantity(0);
                    return inventoryRepository.save(created);
                });
    }

    private InventoryResponse toResponse(ProductVariant variant, Inventory inventory) {

        int quantity = inventory == null ? 0 : value(inventory.getQuantity());
        int reserved = inventory == null ? 0 : value(inventory.getReservedQuantity());
        int sold = inventory == null ? 0 : value(inventory.getSoldQuantity());

        int rawThreshold = inventory == null ? 0 : value(inventory.getLowStockThreshold());

        // 0 (mặc định của DB) hoặc null nghĩa là chưa đặt ngưỡng riêng.
        Integer ownThreshold = rawThreshold > 0 ? rawThreshold : null;
        int effective = ownThreshold != null ? ownThreshold : defaultLowStockThreshold;

        int available = Math.max(0, quantity - reserved);

        Product product = variant.getProduct();

        return new InventoryResponse(
                variant.getId(),
                product.getId(),
                product.getName(),
                variant.getSku(),
                quantity,
                reserved,
                sold,
                available,
                ownThreshold,
                effective,
                available <= effective
        );
    }

    private void audit(String action, ProductVariant variant, int before, int after, String note) {

        log.info("INVENTORY {} variantId={} sku={} before={} after={} change={} by={} note={}",
                action,
                variant.getId(),
                variant.getSku(),
                before,
                after,
                after - before,
                currentActor(),
                note == null ? "" : note.replaceAll("[\\r\\n]+", " "));
    }

    private void notifyLowStockTransition(ProductVariant variant, Inventory inventory, boolean wasLow) {
        InventoryResponse current = toResponse(variant, inventory);
        if (wasLow || !current.lowStock()) return;
        UUID shopId = variant.getProduct().getShopId();
        String message = current.productName() + " còn " + current.availableQuantity() + " sản phẩm khả dụng (ngưỡng " + current.effectiveLowStockThreshold() + ").";
        notificationService.notifyShopOwner(shopId, "Cảnh báo tồn kho thấp", message, com.example.backend.entity.NotificationType.SYSTEM, variant.getId());
        notificationService.notifyRole("ADMIN", "Cảnh báo tồn kho trên sàn", message, com.example.backend.entity.NotificationType.SYSTEM, variant.getId());
    }

    private static int value(Integer number) {
        return number == null ? 0 : number;
    }

    private static String cleanNote(String note) {

        if (note == null || note.isBlank()) {
            return null;
        }

        String trimmed = note.trim();

        if (trimmed.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự."
            );
        }

        return trimmed;
    }

    private static String currentActor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication == null ? null : authentication.getName();
    }
}
