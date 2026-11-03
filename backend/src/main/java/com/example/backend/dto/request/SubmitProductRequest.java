package com.example.backend.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Complete product form. No product is persisted until the final submit action. */
public class SubmitProductRequest {
    private UUID categoryId;
    private UUID brandId;
    private String name;
    private String description;
    private List<OptionInput> options;
    private List<VariantInput> variants;
    private Integer thumbnailIndex;
    public UUID getCategoryId(){return categoryId;}
    public void setCategoryId(UUID v){categoryId=v;}
    public UUID getBrandId(){return brandId;}
    public void setBrandId(UUID v){brandId=v;}
    public String getName(){return name;}
    public void setName(String v){name=v;}
    public String getDescription(){return description;}
    public void setDescription(String v){description=v;}
    public List<OptionInput> getOptions(){return options;}
    public void setOptions(List<OptionInput> v){options=v;}
    public List<VariantInput> getVariants(){return variants;}
    public void setVariants(List<VariantInput> v){variants=v;}
    public Integer getThumbnailIndex(){return thumbnailIndex;}
    public void setThumbnailIndex(Integer v){thumbnailIndex=v;}
    public static class OptionInput {
        private String name;
        private List<String> values;
        public String getName(){return name;}
        public void setName(String v){name=v;}
        public List<String> getValues(){return values;}
        public void setValues(List<String> v){values=v;}
    }
    public static class VariantInput {
        private String sku;
        private BigDecimal price,comparePrice,costPrice;
        private Integer stockQuantity;
        private List<String> selectedValues;
        public String getSku(){return sku;}
        public void setSku(String v){sku=v;}
        public BigDecimal getPrice(){return price;}
        public void setPrice(BigDecimal v){price=v;}
        public BigDecimal getComparePrice(){return comparePrice;}
        public void setComparePrice(BigDecimal v){comparePrice=v;}
        public BigDecimal getCostPrice(){return costPrice;}
        public void setCostPrice(BigDecimal v){costPrice=v;}
        public Integer getStockQuantity(){return stockQuantity;}
        public void setStockQuantity(Integer v){stockQuantity=v;}
        /** One value for each option in the same order as options[]. */
        public List<String> getSelectedValues(){return selectedValues;}
        public void setSelectedValues(List<String> v){selectedValues=v;}
    }
}
