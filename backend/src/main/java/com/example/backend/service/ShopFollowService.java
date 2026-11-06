package com.example.backend.service;

import java.util.List;
import java.util.UUID;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopFollower;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ShopFollowerRepository;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopFollowService {
    private final ShopRepository shops;
    private final ShopFollowerRepository followers;
    private final UserRepository users;

    public ShopFollowService(ShopRepository shops, ShopFollowerRepository followers, UserRepository users) {
        this.shops=shops; this.followers=followers; this.users=users;
    }

    @Transactional(readOnly=true)
    public boolean isFollowing(String email, UUID shopId) {
        User user=user(email);
        return followers.existsByShop_IdAndUser_Id(shopId,user.getId());
    }

    @Transactional
    public boolean follow(String email, UUID shopId) {
        Shop shop=shop(shopId);
        User user=user(email);
        if(!followers.existsByShop_IdAndUser_Id(shopId,user.getId())) {
            ShopFollower link=new ShopFollower(); link.setShop(shop); link.setUser(user); followers.save(link);
            shop.setFollowersCount(Math.toIntExact(followers.countByShop_Id(shopId)));
        }
        return true;
    }

    @Transactional
    public boolean unfollow(String email, UUID shopId) {
        Shop shop=shop(shopId);
        User user=user(email);
        followers.findByShop_IdAndUser_Id(shopId,user.getId()).ifPresent(followers::delete);
        shop.setFollowersCount(Math.toIntExact(followers.countByShop_Id(shopId)));
        return false;
    }

    @Transactional(readOnly=true)
    public List<FollowedShop> followed(String email) {
        return followers.findByUser_IdOrderByCreatedAtDesc(user(email).getId()).stream()
                .map(link -> FollowedShop.from(link.getShop())).toList();
    }

    private Shop shop(UUID id) {
        return shops.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy cửa hàng."));
    }
    private User user(String email) {
        return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    public record FollowedShop(UUID id,String shopName,String slug,String logoUrl,String bannerUrl,String address,
            java.math.BigDecimal ratingAverage,Integer ratingCount,Integer followersCount) {
        static FollowedShop from(Shop s) { return new FollowedShop(s.getId(),s.getShopName(),s.getSlug(),s.getLogoUrl(),s.getBannerUrl(),s.getAddress(),s.getRatingAverage(),s.getRatingCount(),s.getFollowersCount()); }
    }
}
