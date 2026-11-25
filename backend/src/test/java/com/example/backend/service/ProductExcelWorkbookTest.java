package com.example.backend.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProductExcelWorkbookTest {
    private final ProductExcelWorkbook workbook = new ProductExcelWorkbook();

    @Test
    void templateCanBeReadAndHasTheExpectedVersion() {
        var rows = workbook.read(workbook.write(Map.of()));
        assertEquals(ProductExcelSchema.COLUMNS.keySet(), rows.keySet());
        assertTrue(rows.get("Products").isEmpty());
        assertTrue(rows.get("Guide").stream().anyMatch(row -> row.get("key").equals("schema_version") && row.get("value").equals(ProductExcelSchema.VERSION)));
    }

    @Test
    void preservesProductValuesAndRejectsInvalidFiles() {
        var data = Map.of("Products", List.of(List.of("", "P01", "Sản phẩm", "Danh mục", "", "Mô tả", "")));
        var rows = workbook.read(workbook.write(data));
        assertEquals("Sản phẩm", rows.get("Products").getFirst().get("name"));
        assertThrows(IllegalArgumentException.class, () -> workbook.read(new byte[]{1, 2, 3}));
    }
}
