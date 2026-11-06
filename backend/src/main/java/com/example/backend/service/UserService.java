package com.example.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.config.RoleConstants;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.UserRole;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserRoleRepository;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?\\d{9,15}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Thứ tự ưu tiên khi một tài khoản có nhiều vai trò.
     * Vai trò đứng đầu danh sách được dùng làm vai trò mặc định (roleDefault / claim "role" trong JWT).
     */
    private static final List<String> ROLE_PRIORITY = List.of(
            RoleConstants.ADMIN,
            RoleConstants.SHOP_OWNER,
            RoleConstants.CUSTOMER
    );

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

    /**
     * Đăng ký tài khoản mới. Mọi tài khoản đăng ký công khai đều mặc định có vai trò CUSTOMER.
     * Muốn trở thành chủ cửa hàng, khách hàng đăng ký mở shop qua
     * POST /api/customer/shop-registration (xem ShopService#registerShopAsCustomer).
     */
    @Transactional
    public User register(
            String fullName,
            String email,
            String phone,
            String password
    ) {
        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedEmail = email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = phone == null || phone.isBlank()
                ? null
                : phone.trim();

        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên là bắt buộc.");
        }
        if (normalizedName.length() > 255) {
            throw new IllegalArgumentException("Họ và tên tối đa 255 ký tự.");
        }
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email là bắt buộc.");
        }
        if (normalizedEmail.length() > 255
                || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (normalizedPhone != null
                && !PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException(
                    "Số điện thoại không hợp lệ (9–15 chữ số)."
            );
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự."
            );
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        if (normalizedPhone != null
                && userRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException(
                    "Số điện thoại đã được sử dụng."
            );
        }

        Role customerRole = getRoleByName(RoleConstants.CUSTOMER);

        User user = new User();

        user.setFullName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
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

    /**
     * Cấp thêm một vai trò cho tài khoản (không làm gì nếu đã có).
     * Không gỡ các vai trò đang có: chủ shop vẫn là khách hàng nên vẫn mua sắm được.
     */
    @Transactional
    public void grantRoleIfAbsent(User user, String roleName) {
        List<String> current =
                userRoleRepository.findRoleNamesByUserId(user.getId());
        if (current.contains(roleName)) {
            return;
        }
        userRoleRepository.save(
                new UserRole(user, getRoleByName(roleName))
        );
    }

    /**
     * Danh sách vai trò, đã sắp xếp theo mức ưu tiên (ADMIN > SHOP_OWNER > CUSTOMER)
     * nên phần tử đầu tiên chính là vai trò mặc định của tài khoản.
     */

    @Transactional(readOnly = true)
    public List<String> getRoleNamesByUserId(UUID userId) {
        return userRoleRepository.findRoleNamesByUserId(userId)
                .stream()
                .sorted(Comparator.comparingInt(UserService::rolePriority))
                .collect(Collectors.toList());
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private static int rolePriority(String role) {
        int index = ROLE_PRIORITY.indexOf(role);
        return index < 0 ? ROLE_PRIORITY.size() : index;
    }
}
