package com.example.backend.service;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        String identifier = request.getIdentifier().trim();

        User user = userRepository
                .findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Email, số điện thoại hoặc mật khẩu không chính xác."
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException(
                    "Email, số điện thoại hoặc mật khẩu không chính xác."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException(
                    "Tài khoản hiện không thể đăng nhập."
            );
        }

        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRoleDefault()
        );

        UserResponse userResponse = new UserResponse(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                userResponse
        );
    }
}