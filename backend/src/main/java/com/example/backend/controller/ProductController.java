package com.example.backend.controller;

import com.example.backend.dto.request.BulkProductRequest;
import com.example.backend.dto.request.ChangeProductStatusRequest;
import com.example.backend.dto.request.CreateProductRequest;
import com.example.backend.dto.request.UpdateProductRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.BulkResultResponse;
import com.example.backend.dto.response.CategoryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.dto.response.ProductStatsResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.Shop;
import com.example.backend.entity.StockStatus;
import com.example.backend.service.ProductManagementService;
import com.example.backend.service.ProductOwnershipService;
import com.example.backend.service.ProductResponseMapper;
import com.example.backend.service.ProductService;
import com.example.backend.service.ProductStatusService;
import com.example.backend.service.ShopService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

/**
 * API quản lý sản phẩm của shop owner.
 *
 * Quy tắc bảo mật:
 *  - shopId KHÔNG lấy từ client mà suy ra từ tài khoản đang đăng nhập.
 *  - Mọi thao tác theo id sản phẩm đều kiểm tra sản phẩm thuộc shop của owner
 *    (ProductOwnershipService.assertProduct) trước khi đọc/sửa/xóa.
 *  - Thao tác hàng loạt kiểm tra TẤT CẢ id thuộc shop của owner, nếu có một id
 *    không hợp lệ thì không thao tác gì.
 */
@RestController
@RequestMapping("/api/shop-owner/products")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;
    private final com.example.backend.service.ProductExcelService excel;
    private final com.example.backend.service.ProductSubmissionService submissionService;
    private final ProductManagementService managementService;
    private final ProductStatusService statusService;
    private final ProductResponseMapper mapper;
    private final ProductOwnershipService ownership;
    private final ShopService shopService;
    private final com.example.backend.service.ProductEditService editService;

    public ProductController(
            ProductService productService,
            com.example.backend.service.ProductSubmissionService submissionService,
            ProductManagementService managementService,
            ProductStatusService statusService,
            ProductResponseMapper mapper,
            ProductOwnershipService ownership,
            ShopService shopService,
            com.example.backend.service.ProductEditService editService,
            com.example.backend.service.ProductExcelService excel
    ) {
        this.productService = productService;
        this.submissionService = submissionService;
        this.managementService = managementService;
        this.statusService = statusService;
        this.mapper = mapper;
        this.ownership = ownership;
        this.shopService = shopService;
        this.editService = editService;
        this.excel = excel;
    }

    /** One final request after the user has reviewed all steps. Multipart part
     * "product" is JSON; "images" and "videos" are repeated file parts. */
    @PostMapping(value="/submit", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> submitProduct(
            @RequestPart("product") com.example.backend.dto.request.SubmitProductRequest form,
            @RequestPart("images") java.util.List<MultipartFile> images,
            @RequestPart(value="videos",required=false) java.util.List<MultipartFile> videos,
            Authentication authentication) {
        Shop myShop=shopService.getMyShop(authentication.getName());
        Product product=submissionService.submit(myShop.getId(),form,images,videos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi sản phẩm chờ duyệt.",mapper.toResponse(product)));
    }

    // =========================
    // CREATE
    // =========================

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody CreateProductRequest request,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        // Nếu client có gửi shopId thì phải trùng với shop của mình.
        if (request != null
                && request.getShopId() != null
                && !request.getShopId().equals(myShop.getId())) {
            throw new AccessDeniedException(
                    "Bạn không có quyền tạo sản phẩm cho cửa hàng khác."
            );
        }

        if (request != null) {
            request.setShopId(myShop.getId());
        }

        Product product = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Tạo sản phẩm thành công.",
                        mapper.toResponse(product)
                ));
    }

    // =========================
    // LIST: tìm kiếm, lọc, sắp xếp, phân trang (CHỈ SẢN PHẨM CỦA SHOP MÌNH)
    // =========================

    /**
     * Ánh xạ các tab trên giao diện:
     *  - Tất cả   : không truyền status/stockStatus
     *  - Đang bán : status=ACTIVE&stockStatus=IN_STOCK
     *  - Hết hàng : status=ACTIVE&stockStatus=OUT_OF_STOCK
     *  - Tạm ẩn   : status=INACTIVE
     *  - Chờ duyệt: status=PENDING
     *  - Từ chối  : status=REJECTED
     *
     * sort: newest (mặc định), oldest, name_asc, name_desc,
     *       price_asc, price_desc, stock_asc, stock_desc
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getMyProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        ProductManagementService.Filter filter = new ProductManagementService.Filter(
                myShop.getId(), keyword, categoryId, categoryIds, status, stockStatus,
                minPrice, maxPrice, createdFrom, createdTo, sort
        );

        return ApiResponse.success(
                "Lấy danh sách sản phẩm thành công.",
                mapper.toPage(managementService.search(filter, safePage, safeSize))
        );
    }

    // =========================
    // STATS (SỐ ĐẾM CHO CÁC TAB)
    // =========================

    @GetMapping("/stats")
    public ApiResponse<ProductStatsResponse> getStats(
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        return ApiResponse.success(
                "Lấy thống kê sản phẩm thành công.",
                managementService.stats(myShop.getId())
        );
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getMyProductRootCategories(Authentication authentication) {
        Shop myShop = shopService.getMyShop(authentication.getName());
        return ApiResponse.success("Lấy danh mục sản phẩm của cửa hàng thành công.",
                managementService.rootCategoriesForShop(myShop.getId()));
    }

    // =========================
    // EXPORT ALL PRODUCTS OF THE AUTHENTICATED SHOP
    // =========================

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) String sort,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        ProductManagementService.Filter filter = new ProductManagementService.Filter(
                myShop.getId(), keyword, categoryId, categoryIds, status, stockStatus,
                minPrice, maxPrice, createdFrom, createdTo, sort
        );

        return csvResponse(
                managementService.exportCsv(filter),
                "products-" + LocalDate.now() + ".csv"
        );
    }

    // =========================
    // IMPORT EXCEL: PREVIEW -> CONFIRM
    // =========================

    @GetMapping({"/import/template", "/import-template"})
    public ResponseEntity<byte[]> downloadImportTemplate(Authentication authentication) {
        shopService.getMyShop(authentication.getName());
        return excelResponse(
                excel.template(),
                "Kala_Products_Template.xlsx"
        );
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<com.example.backend.service.ProductExcelSchema.Preview> previewImport(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        return ApiResponse.success("Đã kiểm tra file.", excel.preview(authentication.getName(), myShop.getId(), file));
    }

    public record ConfirmImportRequest(String previewToken) {}

    @PostMapping("/import/confirm")
    public ApiResponse<com.example.backend.service.ProductExcelSchema.Result> confirmImport(
            @RequestBody ConfirmImportRequest request, Authentication authentication) {
        Shop shop = shopService.getMyShop(authentication.getName());
        return ApiResponse.success("Đã nhập sản phẩm chờ duyệt.",
                excel.confirm(authentication.getName(), shop.getId(), request == null ? null : request.previewToken()));
    }

    // =========================
    // BULK
    // =========================

    @PatchMapping("/bulk-status")
    public ApiResponse<BulkResultResponse> bulkUpdateStatus(
            @RequestBody BulkProductRequest request,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        // Dùng chung quy tắc với API đổi trạng thái đơn lẻ; tất cả hoặc không gì cả.
        int affected = statusService.bulkChange(
                myShop.getId(),
                request == null ? null : request.ids(),
                request == null ? null : request.status()
        );

        return ApiResponse.success(
                "Cập nhật trạng thái thành công.",
                new BulkResultResponse(affected)
        );
    }

    @PostMapping("/bulk-delete")
    public ApiResponse<BulkResultResponse> bulkDelete(
            @RequestBody BulkProductRequest request,
            Authentication authentication
    ) {
        Shop myShop = shopService.getMyShop(authentication.getName());

        int affected = managementService.bulkDelete(
                myShop.getId(),
                request == null ? null : request.ids()
        );

        return ApiResponse.success(
                "Xóa sản phẩm thành công.",
                new BulkResultResponse(affected)
        );
    }

    // =========================
    // CHANGE STATUS (ACTIVE <-> INACTIVE)
    // =========================

    /**
     * Shop Owner chỉ được bật/tắt kinh doanh sản phẩm ĐÃ ĐƯỢC DUYỆT.
     * PENDING và REJECTED không thể đổi trạng thái qua API này.
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<ProductResponse> changeStatus(
            @PathVariable UUID id,
            @RequestBody ChangeProductStatusRequest request,
            Authentication authentication
    ) {
        ownership.assertProduct(id, authentication.getName());

        Product product = statusService.changeStatus(
                id,
                request == null ? null : request.status()
        );

        return ApiResponse.success(
                "Cập nhật trạng thái sản phẩm thành công.",
                mapper.toResponse(product)
        );
    }

    // =========================
    // GET BY ID
    // =========================

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ownership.assertProduct(id, authentication.getName());

        return ApiResponse.success(
                "Lấy thông tin sản phẩm thành công.",
                mapper.toResponse(productService.getProductById(id))
        );
    }

    // =========================
    // UPDATE
    // =========================

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request,
            Authentication authentication
    ) {
        ownership.assertProduct(id, authentication.getName());

        Product product = productService.updateProduct(id, request);

        return ApiResponse.success(
                "Cập nhật sản phẩm thành công.",
                mapper.toResponse(product)
        );
    }

    // =========================
    // DELETE (DỌN TOÀN BỘ DỮ LIỆU LIÊN QUAN)
    // =========================

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> updateProductWithMedia(
            @PathVariable UUID id,
            @RequestPart("product") com.example.backend.dto.request.EditProductRequest form,
            @RequestPart(value = "images", required = false) java.util.List<MultipartFile> images,
            @RequestPart(value = "videos", required = false) java.util.List<MultipartFile> videos,
            Authentication authentication) {
        Product product = editService.update(id, authentication.getName(), form, images, videos);
        return ApiResponse.success("Đã lưu sản phẩm và media.", mapper.toResponse(product));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        ownership.assertProduct(id, authentication.getName());

        managementService.deleteProduct(id);

        return ApiResponse.success("Xóa sản phẩm thành công.", null);
    }

    // =========================
    // HELPERS
    // =========================

    private ResponseEntity<byte[]> csvResponse(byte[] body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] body, String filename) {

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
