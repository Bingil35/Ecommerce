package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    // Danh mục gốc, sắp xếp theo thứ tự hiển thị
    List<Category> findByParentIsNullOrderBySortOrderAscNameAsc();

    // Danh mục con của một danh mục cha
    List<Category> findByParentIdOrderBySortOrderAscNameAsc(UUID parentId);

    // Kiểm tra danh mục cha có danh mục con hay không
    boolean existsByParentId(UUID parentId);

    // Lấy danh mục theo trạng thái
    List<Category> findByStatusOrderBySortOrderAscNameAsc(CategoryStatus status);

    // Lấy danh mục ACTIVE ở cấp gốc
    List<Category> findByParentIsNullAndStatusOrderBySortOrderAscNameAsc(
            CategoryStatus status
    );
}