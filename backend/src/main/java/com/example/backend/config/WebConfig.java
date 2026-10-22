package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${storage.local.root:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        String uploadLocation =
                Paths.get(uploadRoot)
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();

        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}