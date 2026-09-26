package com.example.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
}