package com.example.backend.entity;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="orderaddresses")
public class OrderAddress {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="recipient_name",length=255) private String recipientName;
 @Column(name="recipient_phone",length=10) private String recipientPhone;
 @Column(name="recipient_city",columnDefinition="text") private String recipientCity;
 @Column(name="ward",columnDefinition="text") private String ward;
 @Column(name="address_detail",columnDefinition="text") private String addressDetail;
 @Column(name="is_default") private Boolean isDefault=false;
 public OrderAddress(){} public UUID getId(){return id;} public String getRecipientName(){return recipientName;} public void setRecipientName(String v){recipientName=v;} public String getRecipientPhone(){return recipientPhone;} public void setRecipientPhone(String v){recipientPhone=v;} public String getRecipientCity(){return recipientCity;} public void setRecipientCity(String v){recipientCity=v;} public String getWard(){return ward;} public void setWard(String v){ward=v;} public String getAddressDetail(){return addressDetail;} public void setAddressDetail(String v){addressDetail=v;} public Boolean getIsDefault(){return isDefault;} public void setIsDefault(Boolean v){isDefault=v;}
}