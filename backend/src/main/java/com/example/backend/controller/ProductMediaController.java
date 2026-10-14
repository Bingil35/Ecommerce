package com.example.backend.controller;

import com.example.backend.dto.request.UpdateProductMediaRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductMediaResponse;
import com.example.backend.entity.ProductMedia;
import com.example.backend.service.ProductMediaService;
import com.example.backend.service.ProductOwnershipService;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop-owner")
public class ProductMediaController {

    private final ProductMediaService productMediaService;
    private final ProductOwnershipService ownership;

    public ProductMediaController(
            ProductMediaService productMediaService,
            ProductOwnershipService ownership
    ) {
        this.productMediaService = productMediaService;
        this.ownership = ownership;
    }

    @PostMapping(
            value = "/products/{productId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ProductMediaResponse> createMedia(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "displayOrder",
                    required = false
            ) Integer displayOrder,
            Authentication authentication
    ) {
        ownership.assertProduct(productId, authentication.getName());
        ProductMedia media =
                productMediaService.createMedia(
                        productId,
                        file,
                        displayOrder
                );

        return ApiResponse.success(
                "Upload product media thành công.",
                new ProductMediaResponse(media)
        );
    }

    @GetMapping("/products/{productId}/media")
    public ApiResponse<List<ProductMediaResponse>> getMedia(
            @PathVariable UUID productId, Authentication authentication
    ) {
        ownership.assertProduct(productId, authentication.getName());
        List<ProductMediaResponse> response =
                productMediaService
                        .getMediaByProduct(productId)
                        .stream()
                        .map(ProductMediaResponse::new)
                        .toList();

        return ApiResponse.success(
                "Lấy danh sách product media thành công.",
                response
        );
    }

    @GetMapping("/media/{mediaId}")
    public ApiResponse<ProductMediaResponse> getMediaById(
            @PathVariable UUID mediaId, Authentication authentication
    ) {
        ownership.assertMedia(mediaId, authentication.getName());
        ProductMedia media =
                productMediaService.getMediaById(mediaId);

        return ApiResponse.success(
                "Lấy product media thành công.",
                new ProductMediaResponse(media)
        );
    }

    @PutMapping("/media/{mediaId}")
    public ApiResponse<ProductMediaResponse> updateMedia(
            @PathVariable UUID mediaId,
            @RequestBody UpdateProductMediaRequest request,
            Authentication authentication
    ) {
        ownership.assertMedia(mediaId, authentication.getName());
        ProductMedia media =
                productMediaService.updateMedia(
                        mediaId,
                        request
                );

        return ApiResponse.success(
                "Cập nhật product media thành công.",
                new ProductMediaResponse(media)
        );
    }

    @DeleteMapping("/media/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @PathVariable UUID mediaId, Authentication authentication
    ) {
        ownership.assertMedia(mediaId, authentication.getName());
        productMediaService.deleteMedia(mediaId);

        return ApiResponse.success(
                "Xóa product media thành công.",
                null
        );
    }
}