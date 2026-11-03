package com.example.backend.dto.request;

import java.util.List;
import java.util.UUID;

/** Danh sách id media của sản phẩm theo thứ tự hiển thị mới. */
public record ReorderMediaRequest(List<UUID> mediaIds) {
}
