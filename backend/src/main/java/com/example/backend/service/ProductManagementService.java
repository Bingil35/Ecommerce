package com.example.backend.service;

import com.example.backend.dto.request.CreateProductOptionRequest;
import com.example.backend.dto.request.CreateProductOptionValueRequest;
import com.example.backend.dto.request.CreateProductRequest;
import com.example.backend.dto.request.CreateProductVariantRequest;
import com.example.backend.dto.response.ProductImportResponse;
import com.example.backend.dto.response.ProductStatsResponse;
import com.example.backend.dto.response.ProductSummary;
import com.example.backend.entity.Category;
import com.example.backend.entity.CategoryStatus;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductMedia;
import com.example.backend.entity.ProductOption;
import com.example.backend.entity.ProductOptionValue;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.StockStatus;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.OwnerProductSpecifications;
import com.example.backend.repository.ProductMediaRepository;
import com.example.backend.repository.ProductOptionRepository;
import com.example.backend.repository.ProductOptionValueRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductTagMappingRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.VariantOptionValueRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Nghiệp vụ quản lý sản phẩm dành cho shop owner:
 * tìm kiếm/lọc/sắp xếp, thống kê tab, xóa dây chuyền, nhân bản,
 * thao tác hàng loạt, xuất và nhập CSV.
 *
 * Service này KHÔNG tự kiểm tra quyền; controller phải xác định shopId
 * từ tài khoản đăng nhập và dùng assertProduct cho thao tác theo id.
 * Các thao tác hàng loạt tự kiểm tra tất cả id thuộc shopId được truyền vào.
 */
@Service
public class ProductManagementService {

    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final int MAX_IMPORT_ROWS = 1_000;
    private static final long MAX_IMPORT_BYTES = 2L * 1024 * 1024;
    private static final int MAX_IMPORT_ERRORS = 100;
    private static final int MAX_NAME_LENGTH = 255;
    private static final String COPY_NAME_SUFFIX = " (Bản sao)";

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter EXPORT_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Set<String> SORT_KEYS = Set.of(
            "newest", "oldest", "name_asc", "name_desc",
            "price_asc", "price_desc", "stock_asc", "stock_desc"
    );

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductOptionRepository optionRepository;
    private final ProductOptionValueRepository optionValueRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;
    private final ProductMediaRepository mediaRepository;
    private final ProductTagMappingRepository tagMappingRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final ProductHistoryGuard historyGuard;
    private final ProductOwnershipService ownership;
    private final ProductResponseMapper mapper;
    private final LocalStorageService localStorageService;
    private final TransactionTemplate transactionTemplate;
    private final int lowStockThreshold;

    public ProductManagementService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductOptionRepository optionRepository,
            ProductOptionValueRepository optionValueRepository,
            VariantOptionValueRepository variantOptionValueRepository,
            ProductMediaRepository mediaRepository,
            ProductTagMappingRepository tagMappingRepository,
            CategoryRepository categoryRepository,
            ProductService productService,
            ProductVariantService productVariantService,
            ProductHistoryGuard historyGuard,
            ProductOwnershipService ownership,
            ProductResponseMapper mapper,
            LocalStorageService localStorageService,
            PlatformTransactionManager transactionManager,
            @Value("${app.inventory.low-stock-threshold:10}") int lowStockThreshold
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.optionRepository = optionRepository;
        this.optionValueRepository = optionValueRepository;
        this.variantOptionValueRepository = variantOptionValueRepository;
        this.mediaRepository = mediaRepository;
        this.tagMappingRepository = tagMappingRepository;
        this.categoryRepository = categoryRepository;
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.historyGuard = historyGuard;
        this.ownership = ownership;
        this.mapper = mapper;
        this.localStorageService = localStorageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.lowStockThreshold = lowStockThreshold;
    }

    // =========================================================
    // SEARCH / FILTER / SORT
    // =========================================================

    /** Bộ lọc danh sách sản phẩm của shop owner. */
    public record Filter(
            UUID shopId,
            String keyword,
            UUID categoryId,
            ProductStatus status,
            StockStatus stockStatus,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate createdFrom,
            LocalDate createdTo,
            String sort
    ) {
    }

    @Transactional(readOnly = true)
    public Page<Product> search(Filter filter, int page, int size) {

        String sortKey = normalizeSort(filter.sort());

        Pageable pageable = PageRequest.of(page, size, sortOf(sortKey));

        return productRepository.findAll(specOf(filter, sortKey), pageable);
    }

    @Transactional(readOnly = true)
    public ProductStatsResponse stats(UUID shopId) {

        return new ProductStatsResponse(
                count(shopId, null, null),
                count(shopId, ProductStatus.PENDING, null),
                count(shopId, ProductStatus.REJECTED, null),
                count(shopId, ProductStatus.ACTIVE, StockStatus.IN_STOCK),
                count(shopId, ProductStatus.ACTIVE, StockStatus.OUT_OF_STOCK),
                count(shopId, ProductStatus.INACTIVE, null),
                count(shopId, ProductStatus.ACTIVE, StockStatus.LOW_STOCK)
        );
    }

    private long count(UUID shopId, ProductStatus status, StockStatus stockStatus) {
        return productRepository.count(
                OwnerProductSpecifications.of(
                        shopId, null, null, status, stockStatus,
                        lowStockThreshold, null, null, null, null, "newest"
                )
        );
    }

    private org.springframework.data.jpa.domain.Specification<Product> specOf(
            Filter f,
            String sortKey
    ) {
        if (f.minPrice() != null && f.maxPrice() != null
                && f.minPrice().compareTo(f.maxPrice()) > 0) {
            throw new IllegalArgumentException(
                    "Giá thấp nhất không được lớn hơn giá cao nhất."
            );
        }

        if (f.createdFrom() != null && f.createdTo() != null
                && f.createdFrom().isAfter(f.createdTo())) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc."
            );
        }

        OffsetDateTime from = f.createdFrom() == null
                ? null
                : f.createdFrom().atStartOfDay(ZONE).toOffsetDateTime();

        OffsetDateTime toExclusive = f.createdTo() == null
                ? null
                : f.createdTo().plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();

        return OwnerProductSpecifications.of(
                f.shopId(),
                f.keyword(),
                f.categoryId(),
                f.status(),
                f.stockStatus(),
                lowStockThreshold,
                f.minPrice(),
                f.maxPrice(),
                from,
                toExclusive,
                sortKey
        );
    }

    private String normalizeSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return "newest";
        }

        String key = sort.trim().toLowerCase(Locale.ROOT);

        if (!SORT_KEYS.contains(key)) {
            throw new IllegalArgumentException(
                    "Giá trị sort không hợp lệ. Cho phép: " + String.join(", ", SORT_KEYS.stream().sorted().toList())
            );
        }

        return key;
    }

    private Sort sortOf(String key) {
        return switch (key) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            // Giá/tồn kho: thứ tự được đặt trong Specification.
            case "price_asc", "price_desc", "stock_asc", "stock_desc" -> Sort.unsorted();
            default -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
        };
    }

    // =========================================================
    // DELETE (DỌN DỮ LIỆU LIÊN QUAN)
    // =========================================================

    /**
     * Xóa sản phẩm cùng toàn bộ dữ liệu liên quan:
     * variant (kèm tồn kho, giỏ hàng, liên kết option), media, giá trị option, option,
     * tag, wishlist. Schema không có ON DELETE CASCADE nên mọi bước đều tường minh.
     * File ảnh chỉ bị xóa sau khi giao dịch commit thành công.
     */
    @Transactional
    public void deleteProduct(UUID productId) {

        Product product = productService.getProductById(productId);

        List<ProductVariant> variants =
                variantRepository.findByProductIdOrderByCreatedAtAsc(productId);

        // 0) Kiểm tra đơn hàng/đánh giá TRƯỚC khi xóa bất kỳ dữ liệu nào,
        //    để không bao giờ xảy ra xóa một phần rồi mới phát hiện lịch sử giao dịch.
        historyGuard.assertProductDeletable(
                productId,
                product.getName(),
                variants.stream().map(ProductVariant::getId).toList()
        );

        // Schema KHÔNG có ON DELETE CASCADE: xóa theo đúng thứ tự khóa ngoại.
        // 1) variant (kèm tồn kho, giỏ hàng, liên kết option)
        for (ProductVariant variant : variants) {
            productVariantService.deleteVariantWithProduct(variant.getId());
        }
        variantRepository.flush();

        // 2) media (variant đã xóa nên không còn tham chiếu primary_media_id)
        List<ProductMedia> media =
                mediaRepository.findByProductIdOrderBySortOrderAsc(productId);

        List<String> urls = media.stream()
                .map(ProductMedia::getMediaUrl)
                .filter(Objects::nonNull)
                .toList();

        mediaRepository.deleteAll(media);

        // 3) giá trị option rồi tới option
        List<ProductOption> options =
                optionRepository.findByProductIdOrderByNameAsc(productId);

        for (ProductOption option : options) {
            optionValueRepository.deleteAll(
                    optionValueRepository
                            .findByProductOptionIdOrderBySortOrderAsc(option.getId())
            );
        }
        optionValueRepository.flush();

        optionRepository.deleteAll(options);

        // 4) tag, wishlist rồi sản phẩm
        tagMappingRepository.deleteAllByProductId(productId);
        mediaRepository.flush();

        // Nhật ký xem gần đây (recentlyviewedproduct) tham chiếu products.
        historyGuard.deleteViewHistory(productId);

        productService.deleteProduct(productId);

        // Lưới an toàn: còn bảng nào đó tham chiếu mà chưa được xử lý -> báo lỗi nghiệp vụ
        // (transaction rollback, không xóa một phần).
        try {
            productRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    "Không thể xóa sản phẩm vì vẫn còn dữ liệu liên quan cần được bảo toàn."
            );
        }

        TransactionCallbacks.afterCommit(() -> {
            for (String url : urls) {
                try {
                    localStorageService.deleteProductMedia(url);
                } catch (RuntimeException ignored) {
                    // File mồ côi không làm hỏng việc xóa dữ liệu.
                }
            }
        });
    }

    // =========================================================
    // BULK
    // =========================================================

    /**
     * Xóa hàng loạt: kiểm tra quyền sở hữu VÀ lịch sử giao dịch của TẤT CẢ sản phẩm
     * trước khi xóa bất kỳ sản phẩm nào. Một sản phẩm không hợp lệ thì rollback toàn bộ.
     * (Đổi trạng thái hàng loạt nằm ở ProductStatusService.bulkChange.)
     */
    @Transactional
    public int bulkDelete(UUID shopId, List<UUID> ids) {

        List<Product> products = ownership.loadOwnedProducts(shopId, ids);

        for (Product product : products) {
            historyGuard.assertProductDeletable(
                    product.getId(),
                    product.getName(),
                    variantRepository
                            .findByProductIdOrderByCreatedAtAsc(product.getId())
                            .stream()
                            .map(ProductVariant::getId)
                            .toList()
            );
        }

        for (Product product : products) {
            deleteProduct(product.getId());
        }

        return products.size();
    }

    // =========================================================
    // DUPLICATE
    // =========================================================

    /**
     * Nhân bản sản phẩm: sao chép thông tin, option, giá trị option, media (file được
     * sao chép thật) và variant. Bản sao là sản phẩm mới nên ở trạng thái PENDING
     * (chờ Admin duyệt), tồn kho = 0, SKU thêm hậu tố "-COPY" (và số thứ tự nếu trùng).
     */
    @Transactional
    public Product duplicate(UUID sourceId) {

        Product source = productService.getProductById(sourceId);

        CreateProductRequest request = new CreateProductRequest();
        request.setShopId(source.getShopId());
        request.setCategoryId(
                source.getCategory() == null ? null : source.getCategory().getId()
        );
        request.setBrandId(source.getBrandId());
        request.setFeatured(source.getFeatured());
        request.setName(copyName(source.getName()));
        request.setDescription(source.getDescription());

        Product copy = productService.createProduct(request);

        // ---- option + giá trị option ----
        Map<UUID, UUID> valueMap = new HashMap<>();

        for (ProductOption option :
                optionRepository.findByProductIdOrderBySortOrderAsc(sourceId)) {

            CreateProductOptionRequest optionRequest = new CreateProductOptionRequest();
            optionRequest.setName(option.getName());

            ProductOption newOption =
                    productVariantService.createOption(copy.getId(), optionRequest);
            newOption.setSortOrder(option.getSortOrder());

            for (ProductOptionValue value :
                    optionValueRepository.findByProductOptionIdOrderBySortOrderAsc(option.getId())) {

                CreateProductOptionValueRequest valueRequest =
                        new CreateProductOptionValueRequest();
                valueRequest.setValue(value.getValue());

                ProductOptionValue newValue =
                        productVariantService.createOptionValue(newOption.getId(), valueRequest);
                newValue.setSortOrder(value.getSortOrder());

                valueMap.put(value.getId(), newValue.getId());
            }
        }

        // ---- media (sao chép file thật) ----
        Map<UUID, UUID> mediaMap = new HashMap<>();
        Map<String, String> urlMap = new HashMap<>();
        List<String> copiedUrls = new ArrayList<>();

        for (ProductMedia media :
                mediaRepository.findByProductIdOrderBySortOrderAsc(sourceId)) {

            String newUrl =
                    localStorageService.copyProductMedia(copy.getId(), media.getMediaUrl());

            if (newUrl == null) {
                continue;
            }

            copiedUrls.add(newUrl);

            ProductMedia newMedia = new ProductMedia();
            newMedia.setProduct(copy);
            newMedia.setMediaType(media.getMediaType());
            newMedia.setMediaUrl(newUrl);
            newMedia.setSortOrder(media.getSortOrder());
            newMedia = mediaRepository.save(newMedia);

            mediaMap.put(media.getId(), newMedia.getId());
            urlMap.put(media.getMediaUrl(), newUrl);
        }

        TransactionCallbacks.afterRollback(() -> deleteFilesQuietly(copiedUrls));

        String thumbnail = source.getThumbnailUrl();

        if (thumbnail != null && !thumbnail.isBlank()) {
            if (urlMap.containsKey(thumbnail)) {
                copy.setThumbnailUrl(urlMap.get(thumbnail));
            } else if (!thumbnail.startsWith("/uploads/")) {
                copy.setThumbnailUrl(thumbnail);
            }
        }

        // ---- variant (tồn kho = 0) ----
        Set<String> usedSkus = new java.util.HashSet<>();

        for (ProductVariant variant :
                variantRepository.findByProductIdOrderByCreatedAtAsc(sourceId)) {

            CreateProductVariantRequest variantRequest = new CreateProductVariantRequest();
            variantRequest.setProductId(copy.getId());
            variantRequest.setSku(SkuGenerator.copyOf(
                    variant.getSku(),
                    candidate -> usedSkus.contains(candidate)
                            || variantRepository.existsByProductIdAndSku(copy.getId(), candidate)
            ));
            variantRequest.setPrice(variant.getPrice());
            variantRequest.setComparePrice(variant.getComparePrice());
            variantRequest.setCostPrice(variant.getCostPrice());
            variantRequest.setStockQuantity(0);

            if (variant.getPrimaryMedia() != null) {
                variantRequest.setPrimaryMediaId(
                        mediaMap.get(variant.getPrimaryMedia().getId())
                );
            }

            variantRequest.setOptionValueIds(
                    variantOptionValueRepository.findByVariantId(variant.getId())
                            .stream()
                            .map(link -> valueMap.get(link.getOptionValue().getId()))
                            .filter(Objects::nonNull)
                            .toList()
            );

            ProductVariant newVariant =
                    productVariantService.createVariant(variantRequest);

            usedSkus.add(newVariant.getSku());

            if (variant.getStatus() != null) {
                newVariant.setStatus(variant.getStatus());
            }
        }

        return productRepository.save(copy);
    }

    private String copyName(String name) {

        String base = name == null ? "" : name;

        int max = MAX_NAME_LENGTH - COPY_NAME_SUFFIX.length();

        if (base.length() > max) {
            base = base.substring(0, max);
        }

        return base + COPY_NAME_SUFFIX;
    }

    // =========================================================
    // EXPORT CSV
    // =========================================================

    private static final String[] EXPORT_HEADER = {
            "sku", "name", "category", "status",
            "min_price", "max_price", "total_stock",
            "variant_count", "created_at"
    };

    /** Xuất danh sách theo cùng bộ lọc với màn hình danh sách (tối đa 10.000 dòng). */
    @Transactional(readOnly = true)
    public byte[] exportCsv(Filter filter) {

        List<Product> products =
                search(filter, 0, MAX_EXPORT_ROWS).getContent();

        Map<UUID, ProductSummary> summaries = productService.getSummaries(
                products.stream().map(Product::getId).toList()
        );

        Map<UUID, String> categories = mapper.categoryNames(products);

        StringBuilder csv = new StringBuilder("\uFEFF");

        appendRow(csv, EXPORT_HEADER);

        for (Product product : products) {

            ProductSummary summary =
                    summaries.getOrDefault(product.getId(), ProductSummary.EMPTY);

            String category = product.getCategory() == null
                    ? ""
                    : categories.getOrDefault(product.getCategory().getId(), "");

            String createdAt = product.getCreatedAt() == null
                    ? ""
                    : product.getCreatedAt().atZoneSameInstant(ZONE).format(EXPORT_DATE);

            appendRow(csv,
                    textCell(summary.sku()),
                    textCell(product.getName()),
                    textCell(category),
                    product.getStatus() == null ? "" : product.getStatus().name(),
                    summary.minPrice() == null ? "" : summary.minPrice().toPlainString(),
                    summary.maxPrice() == null ? "" : summary.maxPrice().toPlainString(),
                    String.valueOf(summary.totalStock()),
                    String.valueOf(summary.variantCount()),
                    createdAt
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] importTemplateCsv() {

        StringBuilder csv = new StringBuilder("\uFEFF");

        appendRow(csv, IMPORT_HEADER);
        appendRow(csv, "Sổ tay ghi chú", "Văn phòng phẩm", "Sổ tay bìa cứng A5",
                "KALA-NOTE-01", "25000", "120");

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder out, String... cells) {

        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escapeCsv(cells[i]));
        }

        out.append("\r\n");
    }

    /** Chặn CSV/Excel formula injection cho ô văn bản do người dùng nhập. */
    private static String textCell(String value) {

        if (value == null || value.isEmpty()) {
            return "";
        }

        char first = value.charAt(0);

        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + value;
        }

        return value;
    }

    private static String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        boolean quote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")
                || value.contains(";");

        String escaped = value.replace("\"", "\"\"");

        return quote ? "\"" + escaped + "\"" : escaped;
    }

    // =========================================================
    // IMPORT CSV
    // =========================================================

    private static final String[] IMPORT_HEADER = {
            "name", "category", "description", "sku", "price", "stock"
    };

    /**
     * Nhập sản phẩm từ CSV (UTF-8, phân cách bằng dấu phẩy hoặc chấm phẩy).
     * Mỗi dòng tạo 1 sản phẩm (PENDING, chờ Admin duyệt) + 1 variant mặc định (không có option),
     * trong một giao dịch riêng; dòng lỗi được báo lại, không ảnh hưởng dòng khác.
     */
    public ProductImportResponse importProducts(UUID shopId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống.");
        }

        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new IllegalArgumentException("File quá lớn (tối đa 2MB).");
        }

        String text;

        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không đọc được file.");
        }

        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        List<List<String>> rows = parseCsv(text);

        if (rows.size() < 2) {
            throw new IllegalArgumentException(
                    "File phải có dòng tiêu đề và ít nhất một dòng dữ liệu."
            );
        }

        if (rows.size() - 1 > MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException(
                    "Chỉ được nhập tối đa " + MAX_IMPORT_ROWS + " sản phẩm mỗi lần."
            );
        }

        Map<String, Integer> columns = new HashMap<>();
        List<String> header = rows.get(0);

        for (int i = 0; i < header.size(); i++) {
            columns.putIfAbsent(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        }

        for (String required : new String[]{"name", "category", "price"}) {
            if (!columns.containsKey(required)) {
                throw new IllegalArgumentException(
                        "Thiếu cột bắt buộc: " + required
                );
            }
        }

        Map<String, Category> categories = loadActiveCategories();

        int total = 0;
        int created = 0;
        List<ProductImportResponse.RowError> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {

            List<String> cells = rows.get(i);

            if (isBlankRow(cells)) {
                continue;
            }

            total++;

            int rowNumber = i + 1;

            try {
                transactionTemplate.executeWithoutResult(status ->
                        createFromRow(shopId, columns, cells, categories)
                );
                created++;
            } catch (IllegalArgumentException | ResourceNotFoundException exception) {
                addImportError(errors, rowNumber, exception.getMessage());
            } catch (RuntimeException exception) {
                // Không trả chi tiết lỗi hệ thống (SQL, ràng buộc DB...) cho client.
                addImportError(errors, rowNumber, "Không thể tạo sản phẩm ở dòng này.");
            }
        }

        return new ProductImportResponse(total, created, total - created, errors);
    }

    private static void addImportError(
            List<ProductImportResponse.RowError> errors,
            int row,
            String message
    ) {
        if (errors.size() < MAX_IMPORT_ERRORS) {
            errors.add(new ProductImportResponse.RowError(
                    row,
                    message == null ? "Lỗi không xác định." : message
            ));
        }
    }

    private void createFromRow(
            UUID shopId,
            Map<String, Integer> columns,
            List<String> cells,
            Map<String, Category> categories
    ) {
        String name = cell(columns, cells, "name");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm là bắt buộc.");
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên sản phẩm quá dài (tối đa 255 ký tự).");
        }

        String categoryText = cell(columns, cells, "category");

        Category category = categories.get(categoryText.toLowerCase(Locale.ROOT));

        if (category == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy danh mục: \"" + categoryText + "\"."
            );
        }

        BigDecimal price = parsePrice(cell(columns, cells, "price"));
        int stock = parseStock(cell(columns, cells, "stock"));

        String sku = cell(columns, cells, "sku");

        if (sku.isEmpty()) {
            sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }

        CreateProductRequest request = new CreateProductRequest();
        request.setShopId(shopId);
        request.setCategoryId(category.getId());
        request.setName(name);
        request.setDescription(emptyToNull(cell(columns, cells, "description")));

        // Sản phẩm nhập từ CSV luôn là PENDING (chờ Admin duyệt); cột status (nếu có) bị bỏ qua.
        Product product = productService.createProduct(request);

        CreateProductVariantRequest variantRequest = new CreateProductVariantRequest();
        variantRequest.setProductId(product.getId());
        variantRequest.setSku(sku);
        variantRequest.setPrice(price);
        variantRequest.setStockQuantity(stock);
        variantRequest.setOptionValueIds(List.of());

        productVariantService.createVariant(variantRequest);
    }

    private Map<String, Category> loadActiveCategories() {

        Map<String, Category> map = new HashMap<>();

        for (Category category :
                categoryRepository.findByStatusOrderBySortOrderAscNameAsc(CategoryStatus.ACTIVE)) {

            if (category.getName() != null) {
                map.putIfAbsent(category.getName().trim().toLowerCase(Locale.ROOT), category);
            }

            map.putIfAbsent(category.getId().toString().toLowerCase(Locale.ROOT), category);
        }

        return map;
    }

    private static String cell(Map<String, Integer> columns, List<String> cells, String column) {

        Integer index = columns.get(column);

        if (index == null || index >= cells.size()) {
            return "";
        }

        return cells.get(index).trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static boolean isBlankRow(List<String> cells) {
        return cells.stream().allMatch(c -> c == null || c.trim().isEmpty());
    }

    private static BigDecimal parsePrice(String raw) {

        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Giá là bắt buộc.");
        }

        String text = raw.replace(" ", "").replace("đ", "").replace("₫", "");

        // Chỉ nhận số nguyên hoặc tối đa 2 chữ số thập phân với dấu chấm,
        // để "25.000" không bị hiểu nhầm thành 25.
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException(
                    "Giá không hợp lệ: \"" + raw + "\" (chỉ dùng số, ví dụ 25000)."
            );
        }

        return new BigDecimal(text);
    }

    private static int parseStock(String raw) {

        if (raw.isEmpty()) {
            return 0;
        }

        try {
            int value = Integer.parseInt(raw);

            if (value < 0) {
                throw new IllegalArgumentException("Tồn kho không được âm.");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Tồn kho không hợp lệ: \"" + raw + "\".");
        }
    }

    /** Bộ phân tích CSV tối giản: dấu ngoặc kép, "" thoát, xuống dòng trong ô. */
    private static List<List<String>> parseCsv(String text) {

        char delimiter = detectDelimiter(text);

        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == delimiter) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("File CSV sai định dạng (dấu ngoặc kép chưa đóng).");
        }

        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }

        return rows;
    }

    /** Excel tiếng Việt thường lưu CSV với dấu chấm phẩy. */
    private static char detectDelimiter(String text) {

        int end = text.indexOf('\n');
        String firstLine = end < 0 ? text : text.substring(0, end);

        long commas = firstLine.chars().filter(ch -> ch == ',').count();
        long semicolons = firstLine.chars().filter(ch -> ch == ';').count();

        return semicolons > commas ? ';' : ',';
    }

    // =========================================================
    // FILES
    // =========================================================

    private void deleteFilesQuietly(List<String> urls) {

        for (String url : urls) {
            try {
                localStorageService.deleteProductMedia(url);
            } catch (RuntimeException ignored) {
                // Best effort: file mồ côi không làm hỏng nghiệp vụ.
            }
        }
    }
}
