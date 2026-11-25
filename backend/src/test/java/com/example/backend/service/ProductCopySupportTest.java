package com.example.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProductCopySupportTest {
    @TempDir Path root;

    @Test void copiesAndDeletesMediaUnderConfiguredRoot() throws Exception {
        UUID source = UUID.randomUUID(), target = UUID.randomUUID();
        Path directory = root.resolve("products").resolve(source.toString());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("image.png"), "image");
        var storage = new LocalStorageService(root.toString());
        String copied = storage.copyProductMedia(target, "/uploads/products/" + source + "/image.png");
        Path copyPath = root.resolve(copied.substring("/uploads/".length()));
        assertEquals("image", Files.readString(copyPath));
        storage.deleteProductMedia(copied);
        assertFalse(Files.exists(copyPath));
        assertTrue(Files.exists(directory.resolve("image.png")));
        assertThrows(IllegalArgumentException.class, () -> storage.copyProductMedia(target, "/uploads/../secret.png"));
    }

    @Test void generatesValidDistinctSkuWithinLengthLimit() {
        String generated = SkuGenerator.copyOf("A".repeat(255), value -> value.endsWith("-COPY"));
        assertTrue(generated.endsWith("-COPY-2"));
        assertEquals(255, generated.length());
    }
}
