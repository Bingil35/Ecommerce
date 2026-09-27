package com.example.backend.controller;

import com.example.backend.dto.request.CreateProductOptionRequest;
import com.example.backend.dto.request.CreateProductOptionValueRequest;
import com.example.backend.dto.request.CreateProductVariantRequest;
import com.example.backend.dto.request.UpdateProductOptionRequest;
import com.example.backend.dto.request.UpdateProductOptionValueRequest;
import com.example.backend.dto.request.UpdateProductVariantRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductOptionResponse;
import com.example.backend.dto.response.ProductOptionValueResponse;
import com.example.backend.dto.response.ProductVariantResponse;
import com.example.backend.entity.ProductOption;
import com.example.backend.entity.ProductOptionValue;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.ProductVariantStatus;
import com.example.backend.service.ProductVariantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(
            ProductVariantService productVariantService
    ) {
        this.productVariantService = productVariantService;
    }

    // =====================================================
    // PRODUCT OPTION
    // =====================================================

    @PostMapping("/products/{productId}/options")
    public ApiResponse<ProductOptionResponse> createOption(
            @PathVariable UUID productId,
            @RequestBody CreateProductOptionRequest request
    ) {
        ProductOption option =
                productVariantService.createOption(
                        productId,
                        request
                );

        return ApiResponse.success(
                "Tạo product option thành công.",
                new ProductOptionResponse(option)
        );
    }

    @GetMapping("/products/{productId}/options")
    public ApiResponse<List<ProductOptionResponse>> getOptions(
            @PathVariable UUID productId
    ) {
        List<ProductOptionResponse> response =
                productVariantService
                        .getOptions(productId)
                        .stream()
                        .map(ProductOptionResponse::new)
                        .toList();

        return ApiResponse.success(
                "Lấy danh sách product option thành công.",
                response
        );
    }

    @GetMapping("/options/{optionId}")
    public ApiResponse<ProductOptionResponse> getOption(
            @PathVariable UUID optionId
    ) {
        ProductOption option =
                productVariantService.getOptionById(
                        optionId
                );

        return ApiResponse.success(
                "Lấy product option thành công.",
                new ProductOptionResponse(option)
        );
    }

    @PutMapping("/options/{optionId}")
    public ApiResponse<ProductOptionResponse> updateOption(
            @PathVariable UUID optionId,
            @RequestBody UpdateProductOptionRequest request
    ) {
        ProductOption option =
                productVariantService.updateOption(
                        optionId,
                        request
                );

        return ApiResponse.success(
                "Cập nhật product option thành công.",
                new ProductOptionResponse(option)
        );
    }

    @DeleteMapping("/options/{optionId}")
    public ApiResponse<Void> deleteOption(
            @PathVariable UUID optionId
    ) {
        productVariantService.deleteOption(optionId);

        return ApiResponse.success(
                "Xóa product option thành công.",
                null
        );
    }

    // =====================================================
    // PRODUCT OPTION VALUE
    // =====================================================

    @PostMapping("/options/{optionId}/values")
    public ApiResponse<ProductOptionValueResponse> createOptionValue(
            @PathVariable UUID optionId,
            @RequestBody CreateProductOptionValueRequest request
    ) {
        ProductOptionValue optionValue =
                productVariantService.createOptionValue(
                        optionId,
                        request
                );

        return ApiResponse.success(
                "Tạo option value thành công.",
                new ProductOptionValueResponse(optionValue)
        );
    }

    @GetMapping("/options/{optionId}/values")
    public ApiResponse<List<ProductOptionValueResponse>> getOptionValues(
            @PathVariable UUID optionId
    ) {
        List<ProductOptionValueResponse> response =
                productVariantService
                        .getOptionValues(optionId)
                        .stream()
                        .map(ProductOptionValueResponse::new)
                        .toList();

        return ApiResponse.success(
                "Lấy danh sách option value thành công.",
                response
        );
    }

    @GetMapping("/option-values/{valueId}")
    public ApiResponse<ProductOptionValueResponse> getOptionValue(
            @PathVariable UUID valueId
    ) {
        ProductOptionValue optionValue =
                productVariantService.getOptionValueById(
                        valueId
                );

        return ApiResponse.success(
                "Lấy option value thành công.",
                new ProductOptionValueResponse(optionValue)
        );
    }

    @PutMapping("/option-values/{valueId}")
    public ApiResponse<ProductOptionValueResponse> updateOptionValue(
            @PathVariable UUID valueId,
            @RequestBody UpdateProductOptionValueRequest request
    ) {
        ProductOptionValue optionValue =
                productVariantService.updateOptionValue(
                        valueId,
                        request
                );

        return ApiResponse.success(
                "Cập nhật option value thành công.",
                new ProductOptionValueResponse(optionValue)
        );
    }

    @DeleteMapping("/option-values/{valueId}")
    public ApiResponse<Void> deleteOptionValue(
            @PathVariable UUID valueId
    ) {
        productVariantService.deleteOptionValue(valueId);

        return ApiResponse.success(
                "Xóa option value thành công.",
                null
        );
    }

    // =====================================================
    // PRODUCT VARIANT
    // =====================================================

    @PostMapping("/products/{productId}/variants")
    public ApiResponse<ProductVariantResponse> createVariant(
            @PathVariable UUID productId,
            @RequestBody CreateProductVariantRequest request
    ) {
        request.setProductId(productId);

        ProductVariant variant =
                productVariantService.createVariant(
                        request
                );

        List<com.example.backend.entity.VariantOptionValue>
                optionValues =
                productVariantService.getVariantOptionValues(
                        variant.getId()
                );

        return ApiResponse.success(
                "Tạo product variant thành công.",
                new ProductVariantResponse(
                        variant,
                        optionValues
                )
        );
    }

    @GetMapping("/products/{productId}/variants")
    public ApiResponse<List<ProductVariantResponse>> getVariants(
            @PathVariable UUID productId
    ) {
        List<ProductVariantResponse> response =
                productVariantService
                        .getVariants(productId)
                        .stream()
                        .map(variant ->
                                new ProductVariantResponse(
                                        variant,
                                        productVariantService
                                                .getVariantOptionValues(
                                                        variant.getId()
                                                )
                                )
                        )
                        .toList();

        return ApiResponse.success(
                "Lấy danh sách product variant thành công.",
                response
        );
    }

    @GetMapping("/products/{productId}/variants/status")
    public ApiResponse<List<ProductVariantResponse>> getVariantsByStatus(
            @PathVariable UUID productId,
            @RequestParam ProductVariantStatus status
    ) {
        List<ProductVariantResponse> response =
                productVariantService
                        .getVariantsByStatus(
                                productId,
                                status
                        )
                        .stream()
                        .map(variant ->
                                new ProductVariantResponse(
                                        variant,
                                        productVariantService
                                                .getVariantOptionValues(
                                                        variant.getId()
                                                )
                                )
                        )
                        .toList();

        return ApiResponse.success(
                "Lấy danh sách product variant theo trạng thái thành công.",
                response
        );
    }

    @GetMapping("/variants/{variantId}")
    public ApiResponse<ProductVariantResponse> getVariant(
            @PathVariable UUID variantId
    ) {
        ProductVariant variant =
                productVariantService.getVariantById(
                        variantId
                );

        return ApiResponse.success(
                "Lấy product variant thành công.",
                new ProductVariantResponse(
                        variant,
                        productVariantService
                                .getVariantOptionValues(
                                        variantId
                                )
                )
        );
    }

    @PutMapping("/variants/{variantId}")
    public ApiResponse<ProductVariantResponse> updateVariant(
            @PathVariable UUID variantId,
            @RequestBody UpdateProductVariantRequest request
    ) {
        ProductVariant variant =
                productVariantService.updateVariant(
                        variantId,
                        request
                );

        return ApiResponse.success(
                "Cập nhật product variant thành công.",
                new ProductVariantResponse(
                        variant,
                        productVariantService
                                .getVariantOptionValues(
                                        variantId
                                )
                )
        );
    }

    @DeleteMapping("/variants/{variantId}")
    public ApiResponse<Void> deleteVariant(
            @PathVariable UUID variantId
    ) {
        productVariantService.deleteVariant(variantId);

        return ApiResponse.success(
                "Xóa product variant thành công.",
                null
        );
    }
}