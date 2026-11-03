package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductOwnershipService {
    private final ProductRepository products;
    private final ProductMediaRepository media;
    private final ProductOptionRepository options;
    private final ProductOptionValueRepository values;
    private final ProductVariantRepository variants;
    private final ShopService shops;

    public ProductOwnershipService(ProductRepository products, ProductMediaRepository media,
                                   ProductOptionRepository options, ProductOptionValueRepository values,
                                   ProductVariantRepository variants, ShopService shops) {
        this.products=products; this.media=media; this.options=options; this.values=values;
        this.variants=variants; this.shops=shops;
    }
    public void assertProduct(UUID productId, String email) {
        Product p=products.findById(productId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        shops.getShopOwnedBy(p.getShopId(), email);
    }
    public void assertMedia(UUID id, String email) {
        ProductMedia m=media.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy media."));
        assertProduct(m.getProduct().getId(), email);
    }
    public void assertOption(UUID id, String email) {
        ProductOption o=options.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tùy chọn sản phẩm."));
        assertProduct(o.getProduct().getId(), email);
    }
    public void assertOptionValue(UUID id, String email) {
        ProductOptionValue v=values.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy giá trị tùy chọn."));
        assertOption(v.getProductOption().getId(), email);
    }
    public void assertVariant(UUID id, String email) {
        ProductVariant v=variants.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy phân loại sản phẩm."));
        assertProduct(v.getProduct().getId(), email);
    }

    private static final int MAX_BULK = 100;

    /**
     * Nạp nhiều sản phẩm cho thao tác hàng loạt. TẤT CẢ id phải tồn tại và thuộc shopId;
     * nếu không, ném lỗi và không thao tác gì (không có thành công một phần).
     */
    public List<Product> loadOwnedProducts(UUID shopId, List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm không được để trống.");
        }

        Set<UUID> unique = new LinkedHashSet<>(ids);

        if (unique.contains(null)) {
            throw new IllegalArgumentException("Danh sách sản phẩm chứa id không hợp lệ.");
        }

        if (unique.size() > MAX_BULK) {
            throw new IllegalArgumentException(
                    "Chỉ được thao tác tối đa " + MAX_BULK + " sản phẩm mỗi lần."
            );
        }

        List<Product> found = products.findAllById(unique);

        if (found.size() != unique.size()) {
            throw new ResourceNotFoundException("Có sản phẩm không tồn tại.");
        }

        for (Product product : found) {
            if (!shopId.equals(product.getShopId())) {
                throw new AccessDeniedException(
                        "Bạn không có quyền thao tác trên sản phẩm của cửa hàng khác."
                );
            }
        }

        return found;
    }
}
