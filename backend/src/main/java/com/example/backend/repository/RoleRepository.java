package com.example.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
