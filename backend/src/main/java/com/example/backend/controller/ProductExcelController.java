package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.service.ProductExcelSchema;
import com.example.backend.service.ProductExcelService;
import com.example.backend.service.ProductExcelValidationException;
import com.example.backend.service.ShopService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shop-owner/products/excel")
public class ProductExcelController {
    private final ProductExcelService excel;
    private final ShopService shops;

    public ProductExcelController(ProductExcelService excel, ShopService shops) {
        this.excel = excel;
        this.shops = shops;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(Authentication auth) {
        shops.getMyShop(auth.getName());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel.template());
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductExcelSchema.Preview> preview(@RequestPart("file") MultipartFile file, Authentication auth) {
        return ApiResponse.success("Đã kiểm tra file.", excel.preview(auth.getName(), shops.getMyShop(auth.getName()).getId(), file));
    }

    public record ConfirmRequest(String token) {}

    @PostMapping("/confirm")
    public ApiResponse<ProductExcelSchema.Result> confirm(@RequestBody ConfirmRequest request, Authentication auth) {
        return ApiResponse.success("Đã nhập sản phẩm.", excel.confirm(auth.getName(), shops.getMyShop(auth.getName()).getId(), request.token()));
    }

    @ExceptionHandler(ProductExcelValidationException.class)
    public ResponseEntity<ApiResponse<ProductExcelSchema.Preview>> validationError(ProductExcelValidationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(false, error.getMessage(), error.getPreview()));
    }
}
