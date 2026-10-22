//package com.example.backend.controller;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.util.UUID;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.security.core.Authentication;
//
//import com.example.backend.dto.request.AddToCartRequest;
//import com.example.backend.dto.request.UpdateCartItemRequest;
//import com.example.backend.dto.response.ApiResponse;
//import com.example.backend.dto.response.CartResponse;
//import com.example.backend.service.CartService;
//
///**
// * Direct controller tests: HTTP status choice, response envelope, and the
// * controller's local error handlers. Routing and security are not covered here;
// * they need a Spring MVC test context.
// */
//class CartControllerTest {
//
//    private static final String ALICE = "alice@example.com";
//
//    private CartService cartService;
//    private CartController controller;
//    private Authentication alice;
//
//    @BeforeEach
//    void setUp() {
//        cartService = mock(CartService.class);
//        controller = new CartController(cartService);
//        alice = mock(Authentication.class);
//        when(alice.getName()).thenReturn(ALICE);
//    }
//
//    @Test
//    void getCartReturnsOkEnvelopeForAuthenticatedCustomer() {
//        CartResponse cart = CartResponse.empty(null);
//        when(cartService.getCart(ALICE)).thenReturn(cart);
//
//        ApiResponse<CartResponse> body = controller.getCart(alice);
//
//        assertTrue(body.isSuccess());
//        assertEquals(cart, body.getData());
//    }
//
//    @Test
//    void addingNewLineReturnsCreated() {
//        CartResponse cart = CartResponse.empty(UUID.randomUUID());
//        when(cartService.addItem(eq(ALICE), any(AddToCartRequest.class)))
//                .thenReturn(new CartService.AddItemResult(cart, true));
//
//        ResponseEntity<ApiResponse<CartResponse>> response =
//                controller.addItem(new AddToCartRequest(), alice);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertTrue(response.getBody().isSuccess());
//        assertEquals(cart, response.getBody().getData());
//    }
//
//    @Test
//    void mergingIntoExistingLineReturnsOk() {
//        CartResponse cart = CartResponse.empty(UUID.randomUUID());
//        when(cartService.addItem(eq(ALICE), any(AddToCartRequest.class)))
//                .thenReturn(new CartService.AddItemResult(cart, false));
//
//        ResponseEntity<ApiResponse<CartResponse>> response =
//                controller.addItem(new AddToCartRequest(), alice);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void customerIdentityComesFromPrincipalNotFromRequestBody() {
//        CartResponse cart = CartResponse.empty(null);
//        when(cartService.addItem(eq(ALICE), any(AddToCartRequest.class)))
//                .thenReturn(new CartService.AddItemResult(cart, true));
//
//        AddToCartRequest request = new AddToCartRequest();
//        request.setProductId(UUID.randomUUID());
//        request.setVariantId(UUID.randomUUID());
//        request.setQuantity(1);
//        controller.addItem(request, alice);
//
//        verify(cartService).addItem(eq(ALICE), eq(request));
//    }
//
//    @Test
//    void updateAndRemovePassCallerAndItemIdThrough() {
//        UUID itemId = UUID.randomUUID();
//        UpdateCartItemRequest update = new UpdateCartItemRequest();
//        update.setQuantity(3);
//        CartResponse cart = CartResponse.empty(null);
//        when(cartService.updateItemQuantity(ALICE, itemId, update)).thenReturn(cart);
//        when(cartService.removeItem(ALICE, itemId)).thenReturn(cart);
//
//        ApiResponse<CartResponse> updated = controller.updateItemQuantity(itemId, update, alice);
//        ApiResponse<CartResponse> removed = controller.removeItem(itemId, alice);
//
//        assertTrue(updated.isSuccess());
//        assertTrue(removed.isSuccess());
//        verify(cartService).updateItemQuantity(ALICE, itemId, update);
//        verify(cartService).removeItem(ALICE, itemId);
//    }
//
//    @Test
//    void malformedBodyReturnsBadRequestWithoutInternalDetails() {
//        ResponseEntity<ApiResponse<Void>> response =
//                controller.handleUnreadableBody(new HttpMessageNotReadableException("Unexpected character at line 1"));
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        assertFalse(response.getBody().isSuccess());
//        assertFalse(response.getBody().getMessage().contains("Unexpected character"));
//    }
//
//    @Test
//    void persistenceConflictReturnsConflictWithoutDatabaseDetails() {
//        ResponseEntity<ApiResponse<Void>> response =
//                controller.handleConflict(new DataIntegrityViolationException("duplicate key uq_cartdetails_subcart_variant"));
//
//        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
//        assertFalse(response.getBody().isSuccess());
//        assertFalse(response.getBody().getMessage().contains("uq_cartdetails"));
//        assertNull(response.getBody().getData());
//    }
//}
