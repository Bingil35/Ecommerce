package com.example.backend.dto.request;
import java.util.UUID;
public class ProductTagMappingRequest {
    private UUID productId; private UUID tagId;
    public UUID getProductId(){return productId;} public void setProductId(UUID v){productId=v;}
    public UUID getTagId(){return tagId;} public void setTagId(UUID v){tagId=v;}
}
