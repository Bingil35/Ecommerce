package com.example.backend.service;

import com.example.backend.entity.CategoryStatus;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Thay đổi trạng thái kinh doanh của sản phẩm (ACTIVE <-> INACTIVE).
 * API đơn lẻ và API hàng loạt dùng CHUNG validateTransition, nên không thể bỏ qua quy tắc.
 *
 * Shop Owner KHÔNG được thay đổi trạng thái duyệt: PENDING và REJECTED bị từ chối.
 * Service không kiểm tra quyền sở hữu; controller phải gọi assertProduct (đơn lẻ),
 * còn bulkChange tự kiểm tra tất cả id thuộc shop.
 */
@Service
public class ProductStatusService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductOwnershipService ownership;

    public ProductStatusService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductOwnershipService ownership
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.ownership = ownership;
    }

    @Transactional
    public Product changeStatus(UUID productId, ProductStatus target) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy sản phẩm."));

        validateTransition(product, target);

        product.setStatus(target);

        return productRepository.save(product);
    }

    /**
     * Tất cả hoặc không gì cả: kiểm tra từng sản phẩm TRƯỚC khi sửa bất kỳ sản phẩm nào;
     * nếu có sản phẩm không hợp lệ thì ném lỗi và transaction rollback toàn bộ.
     */
    @Transactional
    public int bulkChange(UUID shopId, List<UUID> ids, ProductStatus target) {

        List<Product> products = ownership.loadOwnedProducts(shopId, ids);

        for (Product product : products) {
            validateTransition(product, target);
        }

        for (Product product : products) {
            product.setStatus(target);
        }

        productRepository.saveAll(products);

        return products.size();
    }

    /** Quy tắc chuyển trạng thái; ném BusinessException nếu không hợp lệ. */
    void validateTransition(Product product, ProductStatus target) {

        if (target != ProductStatus.ACTIVE && target != ProductStatus.INACTIVE) {
            throw new BusinessException(
                    "Chỉ được chuyển trạng thái sang ACTIVE hoặc INACTIVE."
            );
        }

        String label = "Sản phẩm \"" + product.getName() + "\"";
        ProductStatus current = product.getStatus();

        if (current == ProductStatus.PENDING) {
            throw new BusinessException(
                    label + " đang chờ Admin duyệt, chưa thể thay đổi trạng thái kinh doanh."
            );
        }

        if (current == ProductStatus.REJECTED) {
            throw new BusinessException(
                    label + " đã bị Admin từ chối, không thể thay đổi trạng thái kinh doanh."
            );
        }

        if (current != ProductStatus.ACTIVE && current != ProductStatus.INACTIVE) {
            throw new BusinessException(
                    label + " có trạng thái không hợp lệ để thay đổi."
            );
        }

        if (current == target) {
            throw new BusinessException(
                    label + " đã ở trạng thái " + target + "."
            );
        }

        if (target == ProductStatus.ACTIVE) {
            assertActivatable(product, label);
        }
    }

    /**
     * Điều kiện INACTIVE -> ACTIVE. Không yêu cầu tồn kho > 0.
     */
    private void assertActivatable(Product product, String label) {

        if (product.getName() == null || product.getName().isBlank()) {
            throw new BusinessException(label + " chưa có tên.");
        }

        if (product.getCategory() == null
                || product.getCategory().getStatus() != CategoryStatus.ACTIVE) {
            throw new BusinessException(
                    label + " chưa có danh mục hợp lệ (danh mục phải đang hoạt động)."
            );
        }

        List<ProductVariant> activeVariants =
                variantRepository.findByProductIdAndStatusOrderByCreatedAtAsc(
                        product.getId(), ProductStatus.ACTIVE
                );

        if (activeVariants.isEmpty()) {
            throw new BusinessException(
                    label + " chưa có biến thể nào đang hoạt động."
            );
        }

        boolean hasSellableVariant = activeVariants.stream().anyMatch(v ->
                v.getSku() != null && !v.getSku().isBlank()
                        && v.getPrice() != null
                        && v.getPrice().compareTo(BigDecimal.ZERO) > 0
        );

        if (!hasSellableVariant) {
            throw new BusinessException(
                    label + " chưa có biến thể hoạt động nào có SKU và giá bán hợp lệ."
            );
        }
    }
}
