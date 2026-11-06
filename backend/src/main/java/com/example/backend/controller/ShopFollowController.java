package com.example.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.service.ShopFollowService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/shops")
public class ShopFollowController {
    private final ShopFollowService follows;
    public ShopFollowController(ShopFollowService follows){this.follows=follows;}

    @GetMapping("/{shopId}/follow")
    public ApiResponse<Map<String,Boolean>> status(Authentication auth,@PathVariable UUID shopId){
        return ApiResponse.success("Trạng thái theo dõi",Map.of("following",follows.isFollowing(auth.getName(),shopId)));
    }

    @PostMapping("/{shopId}/follow")
    public ApiResponse<Map<String,Boolean>> follow(Authentication auth,@PathVariable UUID shopId){
        return ApiResponse.success("Đã theo dõi cửa hàng",Map.of("following",follows.follow(auth.getName(),shopId)));
    }

    @DeleteMapping("/{shopId}/follow")
    public ApiResponse<Map<String,Boolean>> unfollow(Authentication auth,@PathVariable UUID shopId){
        return ApiResponse.success("Đã bỏ theo dõi cửa hàng",Map.of("following",follows.unfollow(auth.getName(),shopId)));
    }

    @GetMapping("/followed")
    public ApiResponse<List<ShopFollowService.FollowedShop>> followed(Authentication auth){
        return ApiResponse.success("Cửa hàng đang theo dõi",follows.followed(auth.getName()));
    }
}
