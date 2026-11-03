package com.example.backend.service;

import com.example.backend.entity.ProductMediaType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Kiểm tra file media upload dựa trên NỘI DUNG THẬT (magic bytes), không tin
 * Content-Type hay tên file do client gửi. Phần mở rộng lưu trên đĩa được chọn theo
 * loại đã nhận diện, không lấy từ tên file gốc.
 *
 * Chấp nhận: JPEG, PNG, GIF, WebP (ảnh); MP4, WebM (video). Từ chối SVG, HTML, HEIC...
 */
@Component
public class MediaFileInspector {

    /** Kết quả nhận diện. extension không có dấu chấm. */
    public record Detected(ProductMediaType type, String extension) {
    }

    private static final int HEAD_SIZE = 16;

    private static final Set<String> MP4_BRANDS = Set.of(
            "isom", "iso2", "iso4", "iso5", "iso6",
            "mp41", "mp42", "avc1", "M4V ", "M4VH", "mmp4", "dash"
    );

    private final long maxImageBytes;
    private final long maxVideoBytes;

    public MediaFileInspector(
            @Value("${app.media.max-image-bytes:5242880}") long maxImageBytes,
            @Value("${app.media.max-video-bytes:52428800}") long maxVideoBytes
    ) {
        this.maxImageBytes = maxImageBytes;
        this.maxVideoBytes = maxVideoBytes;
    }

    public Detected inspect(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống.");
        }

        byte[] head;

        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(HEAD_SIZE);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không đọc được file.");
        }

        Detected detected = detect(head);

        if (detected == null) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận ảnh JPEG, PNG, GIF, WebP hoặc video MP4, WebM."
            );
        }

        long limit = detected.type() == ProductMediaType.IMAGE
                ? maxImageBytes
                : maxVideoBytes;

        if (file.getSize() > limit) {
            throw new IllegalArgumentException(
                    "File vượt quá dung lượng cho phép ("
                            + (limit / (1024 * 1024)) + " MB)."
            );
        }

        return detected;
    }

    /** null nếu không nhận diện được. */
    static Detected detect(byte[] h) {

        if (h == null || h.length < 4) {
            return null;
        }

        if (u(h, 0) == 0xFF && u(h, 1) == 0xD8 && u(h, 2) == 0xFF) {
            return new Detected(ProductMediaType.IMAGE, "jpg");
        }

        if (h.length >= 8
                && u(h, 0) == 0x89 && u(h, 1) == 0x50 && u(h, 2) == 0x4E && u(h, 3) == 0x47
                && u(h, 4) == 0x0D && u(h, 5) == 0x0A && u(h, 6) == 0x1A && u(h, 7) == 0x0A) {
            return new Detected(ProductMediaType.IMAGE, "png");
        }

        if (h.length >= 6 && ascii(h, 0, 3).equals("GIF")
                && (ascii(h, 3, 6).equals("87a") || ascii(h, 3, 6).equals("89a"))) {
            return new Detected(ProductMediaType.IMAGE, "gif");
        }

        if (h.length >= 12 && ascii(h, 0, 4).equals("RIFF") && ascii(h, 8, 12).equals("WEBP")) {
            return new Detected(ProductMediaType.IMAGE, "webp");
        }

        if (h.length >= 12 && ascii(h, 4, 8).equals("ftyp")
                && MP4_BRANDS.contains(ascii(h, 8, 12))) {
            return new Detected(ProductMediaType.VIDEO, "mp4");
        }

        if (u(h, 0) == 0x1A && u(h, 1) == 0x45 && u(h, 2) == 0xDF && u(h, 3) == 0xA3) {
            return new Detected(ProductMediaType.VIDEO, "webm");
        }

        return null;
    }

    private static int u(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    private static String ascii(byte[] b, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to && i < b.length; i++) {
            sb.append((char) (b[i] & 0xFF));
        }
        return sb.toString();
    }
}
