package com.example.backend.controller;

import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ShopRegistrationResponse;
import com.example.backend.dto.response.ShopResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.Shop;
import com.example.backend.entity.User;
import com.example.backend.service.JwtService;
import com.example.backend.service.ShopService;
import com.example.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Khách hàng đăng ký mở shop.
 * Nằm dưới /api/customer/** nên SecurityConfig chỉ cho tài khoản có vai trò CUSTOMER gọi.
 */
@RestController
@RequestMapping("/api/customer/shop-registration")
public class ShopRegistrationController {

    private final ShopService shopService;
    private final UserService userService;
    private final JwtService jwtService;

    public ShopRegistrationController(
            ShopService shopService,
            UserService userService,
            JwtService jwtService
    ) {
        this.shopService = shopService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShopRegistrationResponse>> registerShop(
            Authentication authentication,
            @RequestBody CreateShopRequest request
    ) {
        String email = authentication.getName();

        Shop shop = shopService.registerShopAsCustomer(email, request);

        User user = userService.getUserByEmail(email);
        List<String> roles = userService.getRoleNamesByUserId(user.getId());
        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                roles.get(0)
        );

        ShopRegistrationResponse data = new ShopRegistrationResponse(
                new ShopResponse(shop),
                accessToken,
                "Bearer",
                new UserResponse(user, roles)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Đăng ký mở cửa hàng thành công. Cửa hàng đang chờ xác nhận.",
                        data
                ));
    }
}
