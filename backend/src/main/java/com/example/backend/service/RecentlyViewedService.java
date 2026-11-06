package com.example.backend.service;

import java.util.*;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

@Service
public class RecentlyViewedService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ProductRepository products;
    private final ProductResponseMapper mapper;
    private final UserRepository users;

    public RecentlyViewedService(NamedParameterJdbcTemplate jdbc,ProductRepository products,ProductResponseMapper mapper,UserRepository users){
        this.jdbc=jdbc;this.products=products;this.mapper=mapper;this.users=users;
    }

    /** Keep existing installations usable when the optional history table was not in their initial schema. */
    @PostConstruct
    void ensureHistoryTable(){
        jdbc.getJdbcTemplate().execute("""
                create table if not exists recentlyviewedproduct (
                    id uuid primary key default gen_random_uuid(),
                    user_id uuid not null references users(id),
                    product_id uuid not null references products(id),
                    viewed_at timestamptz not null default now()
                )
                """);
    }

    @Transactional
    public void record(String email,UUID productId){
        User user=user(email);
        Product product=products.findById(productId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy sản phẩm."));
        if(product.getStatus()!=ProductStatus.ACTIVE) return;
        MapSqlParameterSource params=new MapSqlParameterSource().addValue("userId",user.getId()).addValue("productId",productId);
        jdbc.update("delete from recentlyviewedproduct where user_id=:userId and product_id=:productId",params);
        jdbc.update("insert into recentlyviewedproduct(user_id,product_id,viewed_at) values (:userId,:productId,NOW())",params);
    }

    @Transactional(readOnly=true)
    public List<ProductResponse> list(String email){
        UUID userId=user(email).getId();
        List<UUID> ids=jdbc.query("select product_id from recentlyviewedproduct where user_id=:userId order by viewed_at desc limit 30",
                new MapSqlParameterSource("userId",userId),(rs,row)->rs.getObject("product_id",UUID.class));
        if(ids.isEmpty())return List.of();
        Map<UUID,Product> byId=new HashMap<>();products.findAllById(ids).stream().filter(p->p.getStatus()==ProductStatus.ACTIVE).forEach(p->byId.put(p.getId(),p));
        List<Product> ordered=ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return mapper.toResponses(ordered);
    }
    private User user(String email){return users.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản."));}
}
