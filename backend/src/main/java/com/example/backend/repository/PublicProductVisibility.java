package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.CategoryStatus;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Điều kiện DUY NHẤT quyết định một sản phẩm có hiển thị công khai cho khách hay không.
 * Mọi truy vấn công khai (tìm kiếm, lọc) phải dùng lớp này để không bị lệch nhau.
 *
 *  - Product.status = ACTIVE
 *  - Shop đang ACTIVE
 *  - Category tồn tại và đang ACTIVE
 *  - Có ít nhất một ProductVariant ACTIVE
 *
 * Hết hàng KHÔNG phải điều kiện ẩn: tồn kho = 0 vẫn hiển thị (Frontend không cho mua).
 */
public final class PublicProductVisibility {

    private PublicProductVisibility() {
    }

    public static List<Predicate> predicates(
            Root<Product> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

        Subquery<UUID> activeShop = query.subquery(UUID.class);
        Root<Shop> shop = activeShop.from(Shop.class);
        activeShop.select(shop.get("id"))
                .where(cb.equal(shop.get("id"), root.get("shopId")),
                        cb.equal(shop.get("status"), ShopStatus.ACTIVE));
        predicates.add(cb.exists(activeShop));

        Subquery<UUID> activeCategory = query.subquery(UUID.class);
        Root<Category> category = activeCategory.from(Category.class);
        activeCategory.select(category.get("id"))
                .where(cb.equal(category.get("id"), root.get("category").get("id")),
                        cb.equal(category.get("status"), CategoryStatus.ACTIVE));
        predicates.add(cb.exists(activeCategory));

        Subquery<Integer> activeVariant = query.subquery(Integer.class);
        Root<ProductVariant> variant = activeVariant.from(ProductVariant.class);
        activeVariant.select(cb.literal(1))
                .where(cb.equal(variant.get("product").get("id"), root.get("id")),
                        cb.equal(variant.get("status"), ProductStatus.ACTIVE));
        predicates.add(cb.exists(activeVariant));

        return predicates;
    }
}
