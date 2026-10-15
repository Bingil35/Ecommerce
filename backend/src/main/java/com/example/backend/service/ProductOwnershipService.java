package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
}
