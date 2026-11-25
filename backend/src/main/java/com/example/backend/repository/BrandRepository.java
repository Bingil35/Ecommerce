package com.example.backend.repository;
import com.example.backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    interface ExcelName { UUID getId(); String getName(); }
    @org.springframework.data.jpa.repository.Query("select b.id as id, b.name as name from Brand b")
    java.util.List<ExcelName> findExcelNames();
    boolean existsBySlug(String slug);
    Optional<Brand> findBySlug(String slug);
}
