package com.example.backend.service;

import com.example.backend.entity.CategoryStatus;
import com.example.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductExcelPreviewTest {
    @Test void previewValidWorkbookAndReportInvalidStock() {
        var workbook = new ProductExcelWorkbook();
        var categories = mock(CategoryRepository.class);
        var category = mock(CategoryRepository.ExcelName.class);
        when(category.getId()).thenReturn(UUID.randomUUID());
        when(category.getName()).thenReturn("Danh mục");
        when(category.getStatus()).thenReturn(CategoryStatus.ACTIVE);
        when(categories.findExcelNames()).thenReturn(List.of(category));
        var products = mock(ProductService.class);
        var variants = mock(ProductVariantService.class);
        var brands = mock(BrandRepository.class);
        var validation = new ProductExcelValidation(products, variants, categories, brands);
        var service = new ProductExcelService(workbook, validation, products, variants, mock(ProductMediaService.class),
                brands, mock(ProductOptionRepository.class), mock(ProductOptionValueRepository.class), mock(ProductRepository.class),
                mock(ObjectMapper.class), mock(PlatformTransactionManager.class));
        UUID shop = UUID.randomUUID();
        var valid = service.preview("seller@example.com", shop, file(workbook, "5"));
        assertTrue(valid.errors().isEmpty(), valid.errors().toString());
        assertNotNull(valid.previewToken());
        assertEquals(1, valid.valid());
        var invalid = service.preview("seller@example.com", shop, file(workbook, "abc"));
        assertNull(invalid.previewToken());
        assertTrue(invalid.errors().stream().anyMatch(issue -> issue.column().equals("stock_quantity")));
    }

    private MockMultipartFile file(ProductExcelWorkbook workbook, String stock) {
        var data = Map.of(
                "Products", List.of(List.of("", "P1", "Sản phẩm", "Danh mục", "", "", "")),
                "Variants", List.of(List.of("P1", "SKU1", "{}", "1000", "", stock)),
                "Media", List.of(List.of("P1", "IMAGE", "https://example.com/image.jpg", "0", "true")));
        return new MockMultipartFile("file", "products.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook.write(data));
    }
}
