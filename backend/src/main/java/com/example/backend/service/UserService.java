package com.example.backend.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.UserRole;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserRoleRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public Role getRoleByCode(String roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found"
                        )
                );
    }

    @Transactional
    public User register(
            String fullName,
            String email,
            String phone,
            String password
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        if (phone != null
                && !phone.isBlank()
                && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "Phone already exists"
            );
        }

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer role not found"
                        )
                );

        User user = new User();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(
                passwordEncoder.encode(password)
        );
        user.setRoleDefault(customerRole.getCode());

        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole(
                savedUser,
                customerRole
        );

        userRoleRepository.save(userRole);

        return savedUser;
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}