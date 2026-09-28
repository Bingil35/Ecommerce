package com.example.backend.repository;

import com.example.backend.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository riêng cho chức năng lọc sản phẩm, để không phải sửa
 * ProductRepository hiện có.
 */
public interface ProductFilterRepository
        extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {
}