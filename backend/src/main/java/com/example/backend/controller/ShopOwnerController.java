package com.example.backend.controller;

import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopResponse;
import com.example.backend.entity.Shop;
import com.example.backend.service.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-owner/shop")
public class ShopOwnerController {
    private final ShopService service; public ShopOwnerController(ShopService service){this.service=service;}
    @GetMapping("/me") public ApiResponse<ShopResponse> getMyShop(Authentication auth){return ApiResponse.success("Lấy thông tin cửa hàng thành công.",new ShopResponse(service.getMyShop(auth.getName())));}
    @PostMapping public org.springframework.http.ResponseEntity<ApiResponse<ShopResponse>> create(Authentication auth,@RequestBody CreateShopRequest request){Shop s=service.createMyShop(auth.getName(),request);return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo cửa hàng thành công. Cửa hàng đang chờ xác nhận.",new ShopResponse(s)));}
}
