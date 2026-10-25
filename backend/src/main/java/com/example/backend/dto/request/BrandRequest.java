package com.example.backend.dto.request;
public class BrandRequest {
    private String name; private String slug; private String logoUrl;
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getLogoUrl(){return logoUrl;} public void setLogoUrl(String v){logoUrl=v;}
}
