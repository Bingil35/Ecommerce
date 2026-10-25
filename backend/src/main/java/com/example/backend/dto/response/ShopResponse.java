package com.example.backend.dto.response;

import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ShopResponse {
    private UUID id; private String shopName; private String slug; private String logoUrl; private String bannerUrl; private String phone; private String email; private String address; private ShopStatus status; private OffsetDateTime createdAt;
    public ShopResponse(Shop s){id=s.getId();shopName=s.getShopName();slug=s.getSlug();logoUrl=s.getLogoUrl();bannerUrl=s.getBannerUrl();phone=s.getPhone();email=s.getEmail();address=s.getAddress();status=s.getStatus();createdAt=s.getCreatedAt();}
    public UUID getId(){return id;} public String getShopName(){return shopName;} public String getSlug(){return slug;} public String getLogoUrl(){return logoUrl;} public String getBannerUrl(){return bannerUrl;} public String getPhone(){return phone;} public String getEmail(){return email;} public String getAddress(){return address;} public ShopStatus getStatus(){return status;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
