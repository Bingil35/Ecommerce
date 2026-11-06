package com.example.backend.service;

import com.example.backend.dto.request.*;
import com.example.backend.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.repository.ProductTagRepository;
import com.example.backend.repository.ProductTagMappingRepository;
import java.util.*;

/** Final-submit orchestrator; all database changes share one transaction. */
@Service
public class ProductSubmissionService {
    private final ProductService products;
    private final ProductVariantService variants;
    private final ProductMediaService media;
    private final ProductTagRepository tags;
    private final ProductTagMappingRepository tagMappings;
    public ProductSubmissionService(ProductService products, ProductVariantService variants,
                                    ProductMediaService media, ProductTagRepository tags,
                                    ProductTagMappingRepository tagMappings) {
        this.products=products; this.variants=variants; this.media=media; this.tags=tags; this.tagMappings=tagMappings;
    }

    @Transactional
    public Product submit(UUID shopId, SubmitProductRequest form,
                          List<MultipartFile> images, List<MultipartFile> videos) {
        if (form == null) throw new IllegalArgumentException("Product form is required");
        if (form.getVariants()==null || form.getVariants().isEmpty())
            throw new IllegalArgumentException("At least one variant is required");
        if (form.getVariants().size()>200) throw new IllegalArgumentException("Maximum 200 variants");
        if (images==null || images.isEmpty()) throw new IllegalArgumentException("At least one image is required");
        if (images.size()>10 || (videos!=null && videos.size()>3))
            throw new IllegalArgumentException("Maximum 10 images and 3 videos");
        if (form.getThumbnailIndex()!=null && (form.getThumbnailIndex()<0 || form.getThumbnailIndex()>=images.size()))
            throw new IllegalArgumentException("Invalid thumbnail index");
        CreateProductRequest productRequest=new CreateProductRequest();
        productRequest.setShopId(shopId);
        productRequest.setCategoryId(form.getCategoryId());
        productRequest.setBrandId(form.getBrandId());
        productRequest.setName(form.getName());
        productRequest.setDescription(form.getDescription());
        productRequest.setFeatured(false);
        Product product=products.createProduct(productRequest);
        List<UUID> tagIds=form.getTagIds()==null?List.of():form.getTagIds().stream().filter(Objects::nonNull).distinct().toList();
        if(tagIds.size()>10)throw new IllegalArgumentException("Maximum 10 product tags");
        List<ProductTag> selectedTags=tags.findAllById(tagIds);
        if(selectedTags.size()!=tagIds.size())throw new IllegalArgumentException("One or more product tags do not exist");
        selectedTags.forEach(tag->tagMappings.save(new ProductTagMapping(product.getId(),tag.getId())));
        List<Map<String,UUID>> optionIds=new ArrayList<>();
        if (form.getOptions()!=null) {
            if (form.getOptions().size()>10) throw new IllegalArgumentException("Too many options");
            for (SubmitProductRequest.OptionInput opt:form.getOptions()) {
                CreateProductOptionRequest optionReq=new CreateProductOptionRequest();
                optionReq.setName(opt.getName());
                ProductOption option=variants.createOption(product.getId(),optionReq);
                Map<String,UUID> ids=new HashMap<>();
                if (opt.getValues()==null || opt.getValues().isEmpty())
                    throw new IllegalArgumentException("Option values are required");
                for (String value:opt.getValues()) {
                    CreateProductOptionValueRequest valueReq=new CreateProductOptionValueRequest();
                    valueReq.setValue(value);
                    ProductOptionValue saved=variants.createOptionValue(option.getId(),valueReq);
                    if (ids.put(value,saved.getId())!=null)
                        throw new IllegalArgumentException("Duplicate option value: "+value);
                }
                optionIds.add(ids);
            }
        }
        GenerateVariantsRequest batch=new GenerateVariantsRequest();
        List<CreateProductVariantRequest> requests=new ArrayList<>();
        for (SubmitProductRequest.VariantInput v:form.getVariants()) {
            List<String> chosen=v.getSelectedValues();
            if (chosen==null) chosen=List.of();
            if (chosen.size()!=optionIds.size()) throw new IllegalArgumentException("Variant option count mismatch");
            List<UUID> selected=new ArrayList<>();
            for (int i=0;i<chosen.size();i++) {
                UUID id=optionIds.get(i).get(chosen.get(i));
                if (id==null) throw new IllegalArgumentException("Unknown option value: "+chosen.get(i));
                selected.add(id);
            }
            CreateProductVariantRequest r=new CreateProductVariantRequest();
            r.setProductId(product.getId()); r.setSku(v.getSku()); r.setPrice(v.getPrice());
            r.setComparePrice(v.getComparePrice()); r.setCostPrice(v.getCostPrice());
            r.setStockQuantity(v.getStockQuantity()); r.setOptionValueIds(selected);
            requests.add(r);
        }
        batch.setVariants(requests);
        variants.generateVariants(product.getId(),batch);
        for (int i=0;i<images.size();i++) {
            media.createMedia(product.getId(),images.get(i),i);
        }
        if (videos!=null) for (int i=0;i<videos.size();i++)
            media.createMedia(product.getId(),videos.get(i),images.size()+i);
        // ProductService already sets PENDING. No draft state or interim product API is used.
        return product;
    }
}
