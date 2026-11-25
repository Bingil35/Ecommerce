package com.example.backend.controller;

import com.example.backend.entity.Shop;
import com.example.backend.service.ProductExcelSchema;
import com.example.backend.service.ProductExcelService;
import com.example.backend.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductExcelControllerTest {
    @Test void acceptsMultipartExcelAndReturnsPreview() throws Exception {
        var excel = mock(ProductExcelService.class);
        var shops = mock(ShopService.class);
        var shop = mock(Shop.class);
        UUID shopId = UUID.randomUUID();
        when(shop.getId()).thenReturn(shopId);
        when(shops.getMyShop("seller@example.com")).thenReturn(shop);
        when(excel.preview(eq("seller@example.com"), eq(shopId), any())).thenReturn(
                new ProductExcelSchema.Preview(1, 1, 0, List.of(), List.of(), List.of(), "token", null, 1, 2, "PENDING"));
        var mvc = MockMvcBuilders.standaloneSetup(new ProductExcelController(excel, shops)).build();
        var file = new MockMultipartFile("file", "products.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2});
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("seller@example.com");
        mvc.perform(multipart("/api/shop-owner/products/excel/preview").file(file).principal((Principal) auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.previewToken").value("token"));
        verify(excel).preview(eq("seller@example.com"), eq(shopId), any());
    }
}
