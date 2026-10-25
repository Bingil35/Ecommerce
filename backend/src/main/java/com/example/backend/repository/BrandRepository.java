package com.example.backend.repository;
import com.example.backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    boolean existsBySlug(String slug);
    Optional<Brand> findBySlug(String slug);
}
