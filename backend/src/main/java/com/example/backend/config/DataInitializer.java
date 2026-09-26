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

            createRoleIfNotExists(
                    roleRepository,
                    "CUSTOMER",
                    "Customer",
                    "Khách hàng"
            );

            createRoleIfNotExists(
                    roleRepository,
                    "SHOP_OWNER",
                    "Shop Owner",
                    "Chủ cửa hàng"
            );

            createRoleIfNotExists(
                    roleRepository,
                    "ADMIN",
                    "Admin",
                    "Quản trị viên"
            );
        };
    }

    private void createRoleIfNotExists(
            RoleRepository roleRepository,
            String code,
            String name,
            String description
    ) {
        if (!roleRepository.existsByCode(code)) {
            roleRepository.save(
                    new Role(
                            code,
                            name,
                            description
                    )
            );
        }
    }
}