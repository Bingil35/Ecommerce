package com.example.backend.service;

import java.util.UUID;

import com.example.backend.config.RoleConstants;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản."
                        )
                );

        String role = normalizeRole(user.getRoleDefault());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(role)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    public UserDetails loadUserById(String userId)
            throws UsernameNotFoundException {

        User user = userRepository.findById(
                UUID.fromString(userId)
        ).orElseThrow(() ->
                new UsernameNotFoundException(
                        "Không tìm thấy tài khoản."
                )
        );

        String role = normalizeRole(user.getRoleDefault());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(role)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            throw new IllegalStateException(
                    "User role is not configured."
            );
        }

        return switch (role) {
            case RoleConstants.CUSTOMER,
                 RoleConstants.SHOP_OWNER,
                 RoleConstants.ADMIN -> role;

            default -> throw new IllegalStateException(
                    "Unsupported user role: " + role
            );
        };
    }
}