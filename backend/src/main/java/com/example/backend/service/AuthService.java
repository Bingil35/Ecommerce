package com.example.backend.service;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {

        if (request == null || request.getIdentifier() == null
                || request.getIdentifier().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new InvalidCredentialsException(
                    "Email, số điện thoại hoặc mật khẩu không chính xác."
            );
        }

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

        List<String> roles = userService.getRoleNamesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new InvalidCredentialsException(
                    "Tài khoản chưa được gán vai trò."
            );
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                roles.get(0)
        );

        UserResponse userResponse = new UserResponse(user, roles);

        return new LoginResponse(
                accessToken,
                "Bearer",
                userResponse
        );
    }
}