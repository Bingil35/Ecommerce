package com.example.backend.service;

import com.example.backend.config.RoleConstants;
import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ShopService {
    // Cột shops.phone dài 10 ký tự.
    private static final Pattern SHOP_PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ShopRepository shops;
    private final UserRepository users;
    private final UserService userService;

    public ShopService(ShopRepository shops, UserRepository users, UserService userService) {
        this.shops = shops;
        this.users = users;
        this.userService = userService;
    }

    public Shop getMyShop(String email) {
        User u = getUser(email);
        return shops.findByOwnerUserId(u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa có cửa hàng."));
    }

    /**
     * Luồng cũ: tài khoản ĐÃ có vai trò SHOP_OWNER tự tạo cửa hàng (POST /api/shop-owner/shop).
     */
    @Transactional
    public Shop createMyShop(String email, CreateShopRequest r) {
        return createShopFor(getUserForUpdate(email), r);
    }

    /**
     * Khách hàng đăng ký mở shop (POST /api/customer/shop-registration):
     * tạo cửa hàng ở trạng thái PENDING và cấp thêm vai trò SHOP_OWNER trong cùng một transaction.
     * Vai trò CUSTOMER được giữ nguyên.
     */
    @Transactional
    public Shop registerShopAsCustomer(String email, CreateShopRequest r) {
        User u = getUserForUpdate(email);
        Shop shop = createShopFor(u, r);
        userService.grantRoleIfAbsent(u, RoleConstants.SHOP_OWNER);
        return shop;
    }

    public Shop getShopOwnedBy(UUID shopId, String email) {
        User u = getUser(email);
        Shop s = shops.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng."));
        if (!s.getOwnerUserId().equals(u.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền truy cập cửa hàng này.");
        }
        return s;
    }

    private Shop createShopFor(User u, CreateShopRequest r) {
        validate(r);
        if (shops.existsByOwnerUserId(u.getId())) {
            throw new IllegalArgumentException("Mỗi chủ shop chỉ được sở hữu một cửa hàng.");
        }
        String name = r.getShopName().trim();
        String base = slug(name);
        String value = base;
        int i = 2;
        while (shops.existsBySlug(value)) {
            value = base + "-" + i++;
        }
        String shopEmail = trimToNull(r.getEmail());

        Shop s = new Shop();
        s.setOwnerUserId(u.getId());
        s.setShopName(name);
        s.setSlug(value);
        s.setPhone(trimToNull(r.getPhone()));
        s.setEmail(shopEmail == null ? u.getEmail() : shopEmail);
        s.setLogoUrl(trimToNull(r.getLogoUrl()));
        s.setBannerUrl(trimToNull(r.getBannerUrl()));
        s.setAddress(trimToNull(r.getAddress()));
        s.setStatus(ShopStatus.PENDING);
        return shops.save(s);
    }

    private void validate(CreateShopRequest r) {
        if (r == null || r.getShopName() == null || r.getShopName().isBlank()) {
            throw new IllegalArgumentException("Tên cửa hàng là bắt buộc.");
        }
        if (r.getShopName().trim().length() > 255) {
            throw new IllegalArgumentException("Tên cửa hàng tối đa 255 ký tự.");
        }
        String phone = trimToNull(r.getPhone());
        if (phone != null && !SHOP_PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại cửa hàng phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
        String email = trimToNull(r.getEmail());
        if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
            throw new IllegalArgumentException("Email cửa hàng không hợp lệ.");
        }
    }

    private User getUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    /** Khóa dòng user để hai request đăng ký shop cùng lúc không tạo ra hai cửa hàng. */
    private User getUserForUpdate(String email) {
        return users.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String slug(String s) {
        String v = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase().trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (v.isBlank()) throw new IllegalArgumentException("Tên cửa hàng không hợp lệ.");
        return v;
    }
}
