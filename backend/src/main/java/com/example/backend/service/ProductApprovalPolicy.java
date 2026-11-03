package com.example.backend.service;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.repository.ProductRepository;

import org.springframework.stereotype.Component;

/**
 * Quy tắc "sửa nội dung quan trọng thì phải được duyệt lại".
 *
 * Gọi markContentChanged khi nội dung cần kiểm duyệt thay đổi (tên, mô tả, danh mục,
 * brand, hình ảnh, option, giá trị option, biến thể). Không gọi cho thao tác vận hành
 * (nhập/điều chỉnh tồn kho, ngưỡng cảnh báo, sắp xếp media, đặt ảnh đại diện).
 *
 *  - ACTIVE, INACTIVE : chuyển về PENDING (chờ Admin duyệt lại)
 *  - PENDING          : giữ nguyên
 *  - REJECTED         : giữ nguyên (chưa có API gửi duyệt lại trong phạm vi hiện tại)
 */
@Component
public class ProductApprovalPolicy {

    private final ProductRepository productRepository;

    public ProductApprovalPolicy(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** @return true nếu trạng thái đã được chuyển về PENDING. */
    public boolean markContentChanged(Product product) {

        if (product == null) {
            return false;
        }

        ProductStatus status = product.getStatus();

        if (status == ProductStatus.ACTIVE || status == ProductStatus.INACTIVE) {
            product.setStatus(ProductStatus.PENDING);
            productRepository.save(product);
            return true;
        }

        return false;
    }
}
