package com.example.backend.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.backend.entity.ShopFollower;
public interface ShopFollowerRepository extends JpaRepository<ShopFollower,UUID> {
    @Query(value="select user_id from shopfollowers where shop_id=:shopId",nativeQuery=true)
    List<UUID> findFollowerUserIds(@Param("shopId") UUID shopId);
    boolean existsByShop_IdAndUser_Id(UUID shopId, UUID userId);
    java.util.Optional<ShopFollower> findByShop_IdAndUser_Id(UUID shopId, UUID userId);
    long countByShop_Id(UUID shopId);
    List<ShopFollower> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
