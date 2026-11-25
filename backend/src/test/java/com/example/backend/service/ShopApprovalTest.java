package com.example.backend.service;

import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.User;
import com.example.backend.controller.AdminManagementController;
import com.example.backend.config.RoleConstants;
import com.example.backend.exception.ShopOwnerRequiredException;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShopApprovalTest {
    @Test void registrationCreatesPendingShopWithoutGrantingSellerRole() {
        var shops = mock(ShopRepository.class); var users = mock(UserRepository.class); var roles = mock(UserService.class);
        var user = mock(User.class); UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id); when(user.getEmail()).thenReturn("user@example.com");
        when(users.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(shops.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CreateShopRequest(); request.setShopName("Test shop");
        Shop shop = new ShopService(shops, users, roles).registerShopAsCustomer("user@example.com", request);
        assertEquals(ShopStatus.PENDING, shop.getStatus());
        verify(roles, never()).grantRoleIfAbsent(any(), anyString());
    }

    @Test void pendingShopIsRejectedWithApprovalMessage() {
        var shops = mock(ShopRepository.class); var users = mock(UserRepository.class);
        var user = mock(User.class); UUID id = UUID.randomUUID(); when(user.getId()).thenReturn(id);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        Shop shop = new Shop(); shop.setStatus(ShopStatus.PENDING);
        when(shops.findByOwnerUserId(id)).thenReturn(Optional.of(shop));
        var service = new ShopService(shops, users, mock(UserService.class));
        assertTrue(assertThrows(ShopOwnerRequiredException.class, () -> service.requireActiveShop("user@example.com")).getMessage().contains("chưa được duyệt"));
        shop.setStatus(ShopStatus.ACTIVE);
        assertSame(shop, service.requireActiveShop("user@example.com"));
    }

    @Test void adminApprovalGrantsSellerRole() {
        var shops = mock(ShopRepository.class); var users = mock(UserRepository.class); var roles = mock(UserService.class);
        UUID shopId = UUID.randomUUID(), ownerId = UUID.randomUUID();
        Shop shop = new Shop(); shop.setOwnerUserId(ownerId); shop.setStatus(ShopStatus.PENDING);
        User owner = new User();
        when(shops.findById(shopId)).thenReturn(Optional.of(shop));
        when(users.findById(ownerId)).thenReturn(Optional.of(owner));
        var controller = new AdminManagementController(users, null, shops, null, null, null, null, null, null, null, null, roles);
        controller.shopStatus(shopId, new AdminManagementController.StatusRequest("ACTIVE"));
        assertEquals(ShopStatus.ACTIVE, shop.getStatus());
        verify(roles).grantRoleIfAbsent(owner, RoleConstants.SHOP_OWNER);
    }
}
