package com.example.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shops")
public class Shop {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "shop_name", length = 255)
    private String shopName;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "logo_url", columnDefinition = "text") private String logoUrl;
    @Column(name = "banner_url", columnDefinition = "text") private String bannerUrl;
    @Column(name = "phone", length = 10) private String phone;
    @Column(name = "email", length = 255) private String email;
    @Column(name = "address", columnDefinition = "text") private String address;
    @Column(name = "rating_average", precision = 2, scale = 1) private BigDecimal ratingAverage;
    @Column(name = "rating_count") private Integer ratingCount = 0;
    @Column(name = "followers_count") private Integer followersCount = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "shop_status_enum")
    private ShopStatus status = ShopStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { OffsetDateTime now=OffsetDateTime.now(); createdAt=now; updatedAt=now; if(status==null) status=ShopStatus.PENDING; if(ratingCount==null) ratingCount=0; if(followersCount==null) followersCount=0; }
    @PreUpdate void onUpdate() { updatedAt=OffsetDateTime.now(); }
    public Shop() {}
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getOwnerUserId(){return ownerUserId;} public void setOwnerUserId(UUID v){ownerUserId=v;}
    public String getShopName(){return shopName;} public void setShopName(String v){shopName=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getLogoUrl(){return logoUrl;} public void setLogoUrl(String v){logoUrl=v;}
    public String getBannerUrl(){return bannerUrl;} public void setBannerUrl(String v){bannerUrl=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public BigDecimal getRatingAverage(){return ratingAverage;} public void setRatingAverage(BigDecimal v){ratingAverage=v;}
    public Integer getRatingCount(){return ratingCount;} public void setRatingCount(Integer v){ratingCount=v;}
    public Integer getFollowersCount(){return followersCount;} public void setFollowersCount(Integer v){followersCount=v;}
    public ShopStatus getStatus(){return status;} public void setStatus(ShopStatus v){status=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
