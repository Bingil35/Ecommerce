package com.example.backend.service;

import java.util.UUID;

import com.example.backend.config.RoleConstants;
import com.example.backend.entity.User;
import com.example.backend.entity.UserStatus;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserRoleRepository;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
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

        String[] roles = getValidatedRoles(user.getId());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(roles)
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

        String[] roles = getValidatedRoles(user.getId());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(roles)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    private String[] getValidatedRoles(UUID userId) {
        // Query role names directly instead of traversing UserRole.role lazy proxies.
        // The previous entity-based query could return detached Role proxies and throw
        // LazyInitializationException when getName() was called outside the session.
        List<String> roles = userRoleRepository
                .findRoleNamesByUserId(userId)
                .stream()
                .map(this::normalizeRole)
                .distinct()
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Tài khoản chưa được gán vai trò."
            );
        }
        return roles.toArray(new String[0]);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalStateException("Role name is not configured.");
        }

        return switch (role) {
            case RoleConstants.CUSTOMER,
                 RoleConstants.SHOP_OWNER,
                 RoleConstants.ADMIN -> role;
            default -> throw new IllegalStateException("Unsupported user role: " + role);
        };
    }
}