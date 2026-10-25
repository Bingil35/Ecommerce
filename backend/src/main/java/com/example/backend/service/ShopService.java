package com.example.backend.service;

import com.example.backend.dto.request.CreateShopRequest;
import com.example.backend.entity.Shop;
import com.example.backend.entity.ShopStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ShopRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.UUID;

@Service
public class ShopService {
    private final ShopRepository shops; private final UserRepository users;
    public ShopService(ShopRepository shops, UserRepository users){this.shops=shops;this.users=users;}
    public Shop getMyShop(String email){User u=getUser(email);return shops.findByOwnerUserId(u.getId()).orElseThrow(()->new ResourceNotFoundException("Tài khoản chưa có cửa hàng."));}
    @Transactional public Shop createMyShop(String email, CreateShopRequest r){
        if(r==null||r.getShopName()==null||r.getShopName().isBlank()) throw new IllegalArgumentException("Tên cửa hàng là bắt buộc.");
        User u=getUser(email); if(shops.existsByOwnerUserId(u.getId())) throw new IllegalArgumentException("Mỗi chủ shop chỉ được sở hữu một cửa hàng.");
        String base=slug(r.getShopName()); String value=base; int i=2; while(shops.existsBySlug(value)) value=base+"-"+i++;
        Shop s=new Shop(); s.setOwnerUserId(u.getId());s.setShopName(r.getShopName().trim());s.setSlug(value);s.setPhone(r.getPhone());s.setEmail(r.getEmail()==null?u.getEmail():r.getEmail());s.setLogoUrl(r.getLogoUrl());s.setBannerUrl(r.getBannerUrl());s.setAddress(r.getAddress());s.setStatus(ShopStatus.PENDING);return shops.save(s);
    }
    public Shop getShopOwnedBy(UUID shopId,String email){User u=getUser(email);Shop s=shops.findById(shopId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy cửa hàng."));if(!s.getOwnerUserId().equals(u.getId())) throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền truy cập cửa hàng này.");return s;}
    private User getUser(String email){return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));}
    private String slug(String s){String v=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","").toLowerCase().trim().replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");if(v.isBlank())throw new IllegalArgumentException("Tên cửa hàng không hợp lệ.");return v;}
}
