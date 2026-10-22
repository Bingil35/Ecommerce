package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findAllByUser_IdOrderByRole_NameAsc(UUID userId);
    @Query("""
    SELECT DISTINCT r.name
    FROM UserRole ur
    JOIN ur.role r
    WHERE ur.user.id = :userId
    ORDER BY r.name
""")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
