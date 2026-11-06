package com.example.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.service.RecentlyViewedService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/recently-viewed")
public class RecentlyViewedController {
    private final RecentlyViewedService recentlyViewed;
    public RecentlyViewedController(RecentlyViewedService recentlyViewed){this.recentlyViewed=recentlyViewed;}

    @PostMapping("/{productId}")
    public ApiResponse<Void> record(Authentication auth,@PathVariable UUID productId){
        recentlyViewed.record(auth.getName(),productId);
        return ApiResponse.success("Đã cập nhật lịch sử xem",null);
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(Authentication auth){
        return ApiResponse.success("Sản phẩm đã xem gần đây",recentlyViewed.list(auth.getName()));
    }
}
