package com.example.backend.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.entity.BannerStatus;
import com.example.backend.entity.SystemBanner;
import com.example.backend.repository.SystemBannerRepository;

@RestController
@RequestMapping("/api/banners")
public class PublicBannerController {
    private final SystemBannerRepository repository;
    public PublicBannerController(SystemBannerRepository repository){this.repository=repository;}
    @GetMapping public ApiResponse<List<SystemBanner>> activeBanners(){
        return ApiResponse.success("Banner đang hoạt động",repository.findAllByOrderByPriorityAscCreatedAtDesc().stream().filter(b->b.getStatus()==BannerStatus.ACTIVE).toList());
    }
}
