package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import com.example.backend.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @RequestBody RegisterRequest request
    ) {

        if (request.getFullName() == null
                || request.getFullName().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Full name is required"
                    ));
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Email is required"
                    ));
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Password is required"
                    ));
        }

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(
                            "Passwords do not match"
                    ));
        }

        User user = userService.register(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword()
        );

        UserResponse response = new UserResponse(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Registration successful",
                                response
                        )
                );
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đăng nhập thành công.",
                        response
                )
        );
    }
}