package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;

public interface ProductRepository
        extends JpaRepository<Product, UUID>,
                JpaSpecificationExecutor<Product> {

    @org.springframework.data.jpa.repository.Query("select distinct p.category.id from Product p where p.shopId = :shopId and p.category is not null")
    List<UUID> findDistinctCategoryIdsByShopId(@org.springframework.data.repository.query.Param("shopId") UUID shopId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findForOptionWrite(@org.springframework.data.repository.query.Param("id") UUID id);

    java.util.Optional<Product> findBySlug(String slug);
    java.util.Optional<Product> findBySlugAndShopIdAndStatus(String slug, UUID shopId, ProductStatus status);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Product> findByShopIdOrderByCreatedAtDesc(
            UUID shopId
    );

    @org.springframework.data.jpa.repository.Query("select p from Product p left join fetch p.category where p.shopId = :shopId order by p.createdAt desc")
    List<Product> findAllForExcelByShopId(@org.springframework.data.repository.query.Param("shopId") UUID shopId);

    List<Product> findByShopIdAndStatusOrderByCreatedAtDesc(
            UUID shopId,
            ProductStatus status
    );

    List<Product> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId);
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
    List<Product> findByCategoryIdAndStatusOrderByCreatedAtDesc(UUID categoryId, ProductStatus status);
}
