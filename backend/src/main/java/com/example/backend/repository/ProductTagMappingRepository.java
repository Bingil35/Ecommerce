package com.example.backend.repository;
import com.example.backend.entity.ProductTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ProductTagMappingRepository extends JpaRepository<ProductTagMapping, UUID> {
    List<ProductTagMapping> findAllByProductId(UUID productId);
    boolean existsByProductIdAndTagId(UUID productId, UUID tagId);
    Optional<ProductTagMapping> findByProductIdAndTagId(UUID productId, UUID tagId);
    void deleteAllByProductId(UUID productId);
}
