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

    /**
     * @param extension phần mở rộng do server xác định từ nội dung file (không có dấu chấm),
     *                  không bao giờ lấy từ tên file client gửi.
     */
    public String storeProductMedia(
            UUID productId,
            MultipartFile file,
            String extension
    ) {
        validateFile(file);

        // Chỉ nhận phần mở rộng an toàn do MediaFileInspector xác định.
        if (extension == null || !extension.matches("[a-z0-9]{2,5}")) {
            throw new IllegalArgumentException("Invalid file extension");
        }

        try {
            Path productDirectory = uploadRoot
                    .resolve("products")
                    .resolve(productId.toString())
                    .normalize();

            Files.createDirectories(productDirectory);

            String filename =
                    UUID.randomUUID() + "." + extension;

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

    public String storeShopImage(UUID shopId, MultipartFile file, String extension, String imageKind) {
        validateFile(file);
        if (extension == null || !extension.matches("[a-z0-9]{2,5}")
                || !java.util.Set.of("logo", "banner").contains(imageKind)) {
            throw new IllegalArgumentException("Invalid shop image");
        }
        try {
            Path directory = uploadRoot.resolve("shops").resolve(shopId.toString()).normalize();
            Files.createDirectories(directory);
            String filename = imageKind + "-" + UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Invalid file path");
            file.transferTo(target);
            return "/uploads/shops/" + shopId + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store shop image", exception);
        }
    }

    public String storeChatMedia(UUID conversationId, MultipartFile file, String extension) {
        validateFile(file);
        if (extension == null || !extension.matches("[a-z0-9]{2,5}")) throw new IllegalArgumentException("Invalid chat media extension");
        try {
            Path directory = uploadRoot.resolve("chat").resolve(conversationId.toString()).normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Invalid file path");
            file.transferTo(target);
            return "/uploads/chat/" + conversationId + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store chat media", exception);
        }
    }

    /**
     * Sao chép một file media sang thư mục của sản phẩm khác.
     * Trả về URL mới, hoặc null nếu file nguồn không tồn tại / không phải file nội bộ.
     */
    public String copyProductMedia(UUID targetProductId, String sourceUrl) {

        if (sourceUrl == null || !sourceUrl.startsWith("/uploads/")) {
            return null;
        }

        try {
            Path source = Paths.get(sourceUrl.substring(1))
                    .toAbsolutePath()
                    .normalize();

            if (!source.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            if (!Files.isRegularFile(source)) {
                return null;
            }

            Path directory = uploadRoot
                    .resolve("products")
                    .resolve(targetProductId.toString())
                    .normalize();

            Files.createDirectories(directory);

            String filename = UUID.randomUUID()
                    + getFileExtension(source.getFileName().toString());

            Path target = directory.resolve(filename).normalize();

            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            Files.copy(source, target);

            return "/uploads/products/" + targetProductId + "/" + filename;

        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy file", exception);
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
