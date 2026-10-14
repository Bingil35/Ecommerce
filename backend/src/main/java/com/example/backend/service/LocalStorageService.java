package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService {

    private final Path uploadRoot;

    public LocalStorageService(
            @Value("${storage.local.root:uploads}") String uploadRoot
    ) {
        this.uploadRoot = Paths.get(uploadRoot)
                .toAbsolutePath()
                .normalize();
    }

    public String storeProductMedia(
            UUID productId,
            MultipartFile file
    ) {
        validateFile(file);

        try {
            Path productDirectory = uploadRoot
                    .resolve("products")
                    .resolve(productId.toString())
                    .normalize();

            Files.createDirectories(productDirectory);

            String extension = getFileExtension(
                    file.getOriginalFilename()
            );

            String filename =
                    UUID.randomUUID() + extension;

            Path targetPath = productDirectory
                    .resolve(filename)
                    .normalize();

            if (!targetPath.startsWith(productDirectory)) {
                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            file.transferTo(targetPath);

            return "/uploads/products/"
                    + productId
                    + "/"
                    + filename;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to store file",
                    exception
            );
        }
    }

    public void deleteProductMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return;
        }

        try {
            String relativePath = mediaUrl;

            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path filePath = Paths.get(relativePath)
                    .toAbsolutePath()
                    .normalize();

            if (!filePath.startsWith(uploadRoot)) {
                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            Files.deleteIfExists(filePath);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to delete file",
                    exception
            );
        }
    }

    private String getFileExtension(
            String originalFilename
    ) {
        if (originalFilename == null
                || originalFilename.isBlank()) {
            return "";
        }

        int lastDotIndex =
                originalFilename.lastIndexOf(".");

        if (lastDotIndex < 0) {
            return "";
        }

        return originalFilename
                .substring(lastDotIndex)
                .toLowerCase();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }
    }
}