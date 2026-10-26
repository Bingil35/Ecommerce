package com.example.backend.controller;

import com.example.backend.dto.request.UpdateCustomerProfileRequest;
import com.example.backend.dto.request.ChangeCustomerPasswordRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;

import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final UserService userService;

    public CustomerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userService.getUserByEmail(email);

        UserResponse response = new UserResponse(
                user, userService.getRoleNamesByUserId(user.getId())
        );

        return ApiResponse.success(
                "Lấy thông tin tài khoản thành công.",
                response
        );
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangeCustomerPasswordRequest request
    ) {
        userService.changeCustomerPassword(
                authentication.getName(), request.getCurrentPassword(), request.getNewPassword()
        );
        return ApiResponse.success("Đổi mật khẩu thành công.", null);
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        User user = userService.updateCustomerProfile(
                authentication.getName(), request.getFullName(), request.getEmail(), request.getPhone()
        );
        UserResponse response = new UserResponse(
                user, userService.getRoleNamesByUserId(user.getId())
        );
        return ApiResponse.success("Cập nhật thông tin tài khoản thành công.", response);
    }
}