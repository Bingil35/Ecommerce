package com.example.backend.controller;

import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopResponse;
import com.example.backend.entity.Shop;
import com.example.backend.service.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.service.LocalStorageService;
import com.example.backend.service.MediaFileInspector;
import com.example.backend.entity.ProductMediaType;
import com.example.backend.service.TransactionCallbacks;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/shop-owner/shop")
public class ShopOwnerController {
    private final ShopService service; private final LocalStorageService storage; private final MediaFileInspector inspector;
    public ShopOwnerController(ShopService service, LocalStorageService storage, MediaFileInspector inspector){this.service=service;this.storage=storage;this.inspector=inspector;}
    @GetMapping("/me") public ApiResponse<ShopResponse> getMyShop(Authentication auth){return ApiResponse.success("Lấy thông tin cửa hàng thành công.",new ShopResponse(service.getMyShop(auth.getName())));}
    @PutMapping("/me") public ApiResponse<ShopResponse> updateMyShop(Authentication auth,@RequestBody CreateShopRequest request){return ApiResponse.success("Cập nhật thông tin cửa hàng thành công.",new ShopResponse(service.updateMyShop(auth.getName(),request)));}
    @PostMapping(value="/me/images", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ApiResponse<ShopResponse> uploadImage(Authentication auth, @RequestParam("file") MultipartFile file, @RequestParam("kind") String kind) {
        Shop shop = service.getMyShop(auth.getName());
        if (!"logo".equals(kind) && !"banner".equals(kind)) throw new IllegalArgumentException("Loại ảnh cửa hàng không hợp lệ.");
        String previousUrl = "logo".equals(kind) ? shop.getLogoUrl() : shop.getBannerUrl();
        MediaFileInspector.Detected detected = inspector.inspect(file);
        if (detected.type() != ProductMediaType.IMAGE) throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
        String imageUrl = storage.storeShopImage(shop.getId(), file, detected.extension(), kind);
        TransactionCallbacks.afterRollback(() -> storage.deleteProductMedia(imageUrl));
        Shop updated = service.updateMyShopImage(auth.getName(), kind, imageUrl);
        if (previousUrl != null && previousUrl.startsWith("/uploads/shops/"))
            TransactionCallbacks.afterCommit(() -> storage.deleteProductMedia(previousUrl));
        return ApiResponse.success("Tải ảnh cửa hàng thành công.", new ShopResponse(updated));
    }
    @PostMapping public org.springframework.http.ResponseEntity<ApiResponse<ShopResponse>> create(Authentication auth,@RequestBody CreateShopRequest request){Shop s=service.createMyShop(auth.getName(),request);return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo cửa hàng thành công. Cửa hàng đang chờ xác nhận.",new ShopResponse(s)));}
}
