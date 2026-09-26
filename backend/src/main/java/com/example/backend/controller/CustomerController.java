package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

        UserResponse response = new UserResponse(user);

        return ApiResponse.success(
                "Lấy thông tin tài khoản thành công.",
                response
        );
    }
}