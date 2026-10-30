package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findAllByUser_IdOrderByIsDefaultDescCreatedAtDesc(UUID userId);
    Optional<UserAddress> findByIdAndUser_Id(UUID id, UUID userId);
    boolean existsByUser_Id(UUID userId);
}
