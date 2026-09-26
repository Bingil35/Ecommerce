package com.example.backend.repository;

import com.example.backend.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository
        extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            UUID id
    );

    List<Category> findByParentIsNullOrderByNameAsc();

    List<Category> findByParentIdOrderByNameAsc(
            UUID parentId
    );

    boolean existsByParentId(UUID parentId);
}