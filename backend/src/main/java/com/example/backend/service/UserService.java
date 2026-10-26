package com.example.backend.service;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

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

    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
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

        Role customerRole = roleRepository.findByName("CUSTOMER")
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

        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole(
                savedUser,
                customerRole
        );

        userRoleRepository.save(userRole);

        return savedUser;
    }

    @Transactional
    public User updateCustomerProfile(String currentEmail, String fullName, String email, String phone) {
        // Resolve the account from the authenticated principal; never accept a target user ID from the client.
        User user = getUserByEmail(currentEmail);

        String normalizedName = fullName == null ? "" : fullName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new IllegalArgumentException("Email này đã được sử dụng bởi tài khoản khác.");
        }

        String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
        if (normalizedPhone != null && userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi tài khoản khác.");
        }

        user.setFullName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        return userRepository.save(user);
    }

    @Transactional
    public void changeCustomerPassword(String currentEmail, String currentPassword, String newPassword) {
        User user = getUserByEmail(currentEmail);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 72 ký tự.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNamesByUserId(UUID userId) {
        return userRoleRepository.findRoleNamesByUserId(userId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}