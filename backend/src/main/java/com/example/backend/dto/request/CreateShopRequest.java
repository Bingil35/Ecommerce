package com.example.backend.dto.request;

public class CreateShopRequest {
    private String shopName; private String phone; private String email; private String logoUrl; private String bannerUrl; private String address;
    public String getShopName(){return shopName;} public void setShopName(String v){shopName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getLogoUrl(){return logoUrl;} public void setLogoUrl(String v){logoUrl=v;}
    public String getBannerUrl(){return bannerUrl;} public void setBannerUrl(String v){bannerUrl=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
}
