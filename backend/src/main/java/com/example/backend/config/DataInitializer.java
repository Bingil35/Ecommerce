package com.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backend.entity.Role;
import com.example.backend.repository.RoleRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            createRoleIfNotExists(roleRepository, RoleConstants.CUSTOMER, "Khách hàng");
            createRoleIfNotExists(roleRepository, RoleConstants.SHOP_OWNER, "Chủ cửa hàng");
            createRoleIfNotExists(roleRepository, RoleConstants.ADMIN, "Quản trị viên");
        };
    }

    private void createRoleIfNotExists(
            RoleRepository roleRepository,
            String roleName,
            String description
    ) {
        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(new Role(roleName, description));
        }
    }
}
