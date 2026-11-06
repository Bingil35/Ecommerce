package com.example.backend.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.entity.SystemBanner;
public interface SystemBannerRepository extends JpaRepository<SystemBanner, UUID> {
    List<SystemBanner> findAllByOrderByPriorityAscCreatedAtDesc();
}
