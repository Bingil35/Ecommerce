package com.example.backend.dto.response;

import com.example.backend.entity.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public product detail DTO. Deliberately excludes internal cost price and owner contact details. */
public class ProductDetailResponse {
    private UUID id, shopId, categoryId, brandId;
    private String name, slug, description, thumbnailUrl, status;
    private Boolean featured;
    private BigDecimal ratingAverage, minPrice, maxPrice;
    private Integer ratingCount, stockQuantity;
    private PublicShop shop;
    private PublicCategory category;
    private List<Media> media;
    private List<Option> options;
    private List<Variant> variants;

    public ProductDetailResponse(Product product, PublicShop shop, List<Media> media, List<Option> options,
                                 List<Variant> variants, BigDecimal minPrice, BigDecimal maxPrice, Integer stockQuantity) {
        this.id=product.getId(); this.shopId=product.getShopId();
        this.categoryId=product.getCategory()==null?null:product.getCategory().getId(); this.brandId=product.getBrandId();
        this.name=product.getName(); this.slug=product.getSlug(); this.description=product.getDescription();
        this.thumbnailUrl=product.getThumbnailUrl(); this.status=product.getStatus().name(); this.featured=product.getFeatured();
        this.ratingAverage=product.getRatingAverage(); this.ratingCount=product.getRatingCount(); this.shop=shop;
        this.category=product.getCategory()==null?null:new PublicCategory(product.getCategory());
        this.media=media; this.options=options; this.variants=variants; this.minPrice=minPrice; this.maxPrice=maxPrice; this.stockQuantity=stockQuantity;
    }
    public UUID getId(){return id;} public UUID getShopId(){return shopId;} public UUID getCategoryId(){return categoryId;}
    public UUID getBrandId(){return brandId;} public String getName(){return name;} public String getSlug(){return slug;}
    public String getDescription(){return description;} public String getThumbnailUrl(){return thumbnailUrl;} public String getStatus(){return status;}
    public Boolean getFeatured(){return featured;} public BigDecimal getRatingAverage(){return ratingAverage;} public Integer getRatingCount(){return ratingCount;}
    public BigDecimal getMinPrice(){return minPrice;} public BigDecimal getMaxPrice(){return maxPrice;} public Integer getStockQuantity(){return stockQuantity;}
    public BigDecimal getPrice(){return minPrice;} public PublicShop getShop(){return shop;} public PublicCategory getCategory(){return category;} public List<Media> getMedia(){return media;} public List<Option> getOptions(){return options;} public List<Variant> getVariants(){return variants;}


    public static class PublicCategory {
        private UUID id; private String name, slug, imageUrl;
        public PublicCategory(Category c){id=c.getId();name=c.getName();slug=c.getSlug();imageUrl=c.getImageUrl();}
        public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;} public String getImageUrl(){return imageUrl;}
    }
    public static class PublicShop {
        private UUID id; private String name, slug, logoUrl, bannerUrl; private BigDecimal ratingAverage; private Integer ratingCount, followersCount;
        public PublicShop(Shop s){id=s.getId();name=s.getShopName();slug=s.getSlug();logoUrl=s.getLogoUrl();bannerUrl=s.getBannerUrl();ratingAverage=s.getRatingAverage();ratingCount=s.getRatingCount();followersCount=s.getFollowersCount();}
        public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;} public String getLogoUrl(){return logoUrl;} public String getBannerUrl(){return bannerUrl;} public BigDecimal getRatingAverage(){return ratingAverage;} public Integer getRatingCount(){return ratingCount;} public Integer getFollowersCount(){return followersCount;}
    }
    public static class Media {
        private UUID id; private String type, url; private Integer sortOrder;
        public Media(ProductMedia m){id=m.getId();type=m.getMediaType()==null?null:m.getMediaType().name();url=m.getUrl();sortOrder=m.getSortOrder()==null?0:m.getSortOrder().intValue();}
        public UUID getId(){return id;} public String getType(){return type;} public String getUrl(){return url;} public Integer getSortOrder(){return sortOrder;}
    }
    public static class Option {
        private UUID id; private String name; private Integer sortOrder; private List<OptionValue> values;
        public Option(ProductOption o,List<OptionValue> values){id=o.getId();name=o.getName();sortOrder=o.getSortOrder()==null?0:o.getSortOrder().intValue();this.values=values;}
        public UUID getId(){return id;} public String getName(){return name;} public Integer getSortOrder(){return sortOrder;} public List<OptionValue> getValues(){return values;}
    }
    public static class OptionValue {
        private UUID id; private String value; private Integer sortOrder;
        public OptionValue(ProductOptionValue v){id=v.getId();value=v.getValue();sortOrder=v.getSortOrder()==null?0:v.getSortOrder().intValue();}
        public UUID getId(){return id;} public String getValue(){return value;} public Integer getSortOrder(){return sortOrder;}
    }
    public static class Variant {
        private UUID id; private String sku, status; private BigDecimal price, comparePrice; private String imageUrl; private Integer stockQuantity; private List<SelectedOption> selectedOptions;
        public Variant(ProductVariant v,Integer stock,List<SelectedOption> selectedOptions){id=v.getId();sku=v.getSku();status=v.getStatus()==null?null:v.getStatus().name();price=v.getPrice();comparePrice=v.getComparePrice();imageUrl=v.getImageUrl();stockQuantity=stock;this.selectedOptions=selectedOptions;}
        public UUID getId(){return id;} public String getSku(){return sku;} public String getStatus(){return status;} public BigDecimal getPrice(){return price;} public BigDecimal getComparePrice(){return comparePrice;} public String getImageUrl(){return imageUrl;} public Integer getStockQuantity(){return stockQuantity;} public List<SelectedOption> getSelectedOptions(){return selectedOptions;}
    }
    public static class SelectedOption {
        private UUID optionId, optionValueId; private String optionName, value;
        public SelectedOption(ProductOptionValue v){optionValueId=v.getId();value=v.getValue();ProductOption o=v.getProductOption();optionId=o.getId();optionName=o.getName();}
        public UUID getOptionId(){return optionId;} public UUID getOptionValueId(){return optionValueId;} public String getOptionName(){return optionName;} public String getValue(){return value;}
    }
}
