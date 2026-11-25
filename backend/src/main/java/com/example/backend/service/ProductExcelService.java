package com.example.backend.service;

import com.example.backend.dto.request.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.*;
import java.util.*;
import static com.example.backend.service.ProductExcelSchema.*;

@Service
public class ProductExcelService {
    private final ProductExcelWorkbook workbook;
    private final ProductExcelValidation validation;
    private final ProductService products;
    private final ProductVariantService variants;
    private final ProductMediaService media;
    private final BrandRepository brands;
    private final ProductOptionRepository options;
    private final ProductOptionValueRepository values;
    private final ProductRepository productRepository;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    // Server-owned bytes bind each token to exactly the file that passed preview.
    private record CatalogIdentity(UUID categoryId, UUID brandId) {}
    private record Session(String actor, UUID shop, byte[] bytes, Instant expires, Map<String,CatalogIdentity> catalog) {}
    private final Map<String,Session> sessions = new HashMap<>();
    private static final long CACHE_BYTES = 32L * 1024 * 1024;

    public ProductExcelService(ProductExcelWorkbook workbook, ProductExcelValidation validation,
            ProductService products, ProductVariantService variants, ProductMediaService media,
            BrandRepository brands, ProductOptionRepository options,
            ProductOptionValueRepository values, ProductRepository productRepository,
            ObjectMapper json, PlatformTransactionManager transactionManager) {
        this.workbook=workbook; this.validation=validation; this.products=products; this.variants=variants;
        this.media=media; this.brands=brands; this.options=options; this.values=values;
        this.productRepository=productRepository; this.json=json;
        transaction=new TransactionTemplate(transactionManager);
    }
    public byte[] template() { return workbook.write(Map.of()); }

    @Transactional(readOnly=true)
    public byte[] export(UUID shopId) {
        List<Product> allProducts=productRepository.findAllForExcelByShopId(shopId);
        Set<UUID> brandIds=new HashSet<>();
        for (var product : allProducts) if (product.getBrandId()!=null) brandIds.add(product.getBrandId());
        Map<UUID,String> brandNames=new HashMap<>();
        if (!brandIds.isEmpty()) for (var brand : brands.findAllById(brandIds)) brandNames.put(brand.getId(),brand.getName());
        Map<String,List<List<String>>> data=new LinkedHashMap<>();
        COLUMNS.keySet().forEach(s -> data.put(s,new ArrayList<>()));
        long rowCount=30, cellCount=100;
        for (Product p : allProducts) {
            long previousRows=data.values().stream().mapToLong(List::size).sum();
            long previousCells=data.entrySet().stream().mapToLong(e -> (long)e.getValue().size()*COLUMNS.get(e.getKey()).size()).sum();
            String code=p.getId().toString();
            data.get("Products").add(row(p.getId(),code,p.getName(),p.getCategory()==null?null:p.getCategory().getName(),brandNames.get(p.getBrandId()),p.getDescription(),p.getThumbnailUrl()));
            for (ProductOption option : options.findByProductIdOrderBySortOrderAsc(p.getId())) {
                for (ProductOptionValue value : values.findByProductOptionIdOrderBySortOrderAsc(option.getId()))
                    data.get("Options").add(row(code,option.getName(),value.getValue(),value.getSortOrder()));
            }
            for (ProductVariant variant : variants.getVariants(p.getId())) {
                Map<String,String> selected=new LinkedHashMap<>();
                for (VariantOptionValue link : variants.getVariantOptionValues(variant.getId()))
                    selected.put(link.getOptionValue().getProductOption().getName(),link.getOptionValue().getValue());
                data.get("Variants").add(row(code,variant.getSku(),json.writeValueAsString(selected),variant.getPrice(),variant.getComparePrice(),variant.getStockQuantity()==null?0:variant.getStockQuantity()));
            }
            boolean marked=false;
            for (ProductMedia m : media.getMediaByProduct(p.getId())) {
                boolean thumbnail=!marked && m.getMediaType()==ProductMediaType.IMAGE && Objects.equals(m.getUrl(),p.getThumbnailUrl());
                if(thumbnail) marked=true;
                data.get("Media").add(row(code,m.getMediaType(),m.getUrl(),m.getSortOrder()==null?0:m.getSortOrder(),thumbnail));
            }
            rowCount+=data.values().stream().mapToLong(List::size).sum()-previousRows;
            cellCount+=data.entrySet().stream().mapToLong(e -> (long)e.getValue().size()*COLUMNS.get(e.getKey()).size()).sum()-previousCells;
            if(rowCount>MAX_ROWS || cellCount>MAX_CELLS)
                throw new IllegalArgumentException("Toàn bộ dữ liệu vượt giới hạn workbook an toàn (30000 dòng, 200000 ô). Không có file xuất một phần.");
        }
        byte[] bytes=workbook.write(data);
        if(bytes.length>MAX_BYTES) throw new IllegalArgumentException("Toàn bộ dữ liệu xuất vượt 5 MiB. Không có file xuất một phần.");
        return bytes;
    }
    private List<String> row(Object... objects) { return Arrays.stream(objects).map(o -> o==null?"":o.toString()).toList(); }

    @Transactional(readOnly=true)
    public Preview preview(String actor, UUID shop, MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>MAX_BYTES || file.getOriginalFilename()==null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new IllegalArgumentException("Chọn file .xlsx không rỗng, tối đa 5 MiB.");
        byte[] bytes;
        try (var stream=file.getInputStream()) {
            bytes=stream.readNBytes((int)MAX_BYTES+1);
        } catch(IOException e) { throw new IllegalArgumentException("Không đọc được file tải lên."); }
        Map<String,List<ExcelRow>> rows;
        try { rows=workbook.read(bytes); }
        catch (IllegalArgumentException e) {
            Issue issue=e instanceof ProductExcelWorkbook.FormatException f ? f.issue() : new Issue("","Workbook",0,"",e.getMessage());
            return new Preview(0,0,0,List.of(),List.of(),List.of(issue),null,null,0,0,"PENDING");
        }
        var checked=validation.validate(shop,rows);
        String token=null; Instant expires=null;
        if(checked.errors().isEmpty()) {
            expires=Instant.now().plus(Duration.ofMinutes(15));
            synchronized(sessions) {
                evictExpired();
                sessions.entrySet().removeIf(e -> e.getValue().actor().equals(actor) && e.getValue().shop().equals(shop));
                long used=sessions.values().stream().mapToLong(s -> s.bytes().length).sum();
                if(sessions.size()>=64 || used+bytes.length>CACHE_BYTES) throw new IllegalArgumentException("Bộ nhớ xem trước đang đầy. Vui lòng thử lại sau.");
                token=UUID.randomUUID().toString();
                Map<String,CatalogIdentity> catalog=new HashMap<>();
                for (var item : checked.items()) catalog.put(item.code(),new CatalogIdentity(item.request().getCategoryId(),item.request().getBrandId()));
                sessions.put(token,new Session(actor,shop,bytes,expires,Map.copyOf(catalog)));
            }
        }
        return response(checked,token,expires);
    }
    private void evictExpired() { sessions.entrySet().removeIf(e -> !e.getValue().expires().isAfter(Instant.now())); }
    private Preview response(ProductExcelValidation.Checked checked, String token, Instant expires) {
        List<ProductPreview> preview=checked.items().stream().map(item -> new ProductPreview(item.code(),item.request().getName(),
                item.categoryName(),item.brandName(),
                item.request().getThumbnailUrl(),item.options().size(),item.variants().size(),stock(item))).toList();
        List<VariantPreview> variantPreview=checked.items().stream().flatMap(item -> item.variants().stream().map(variant -> {
            Map<String,String> selected=new LinkedHashMap<>();
            for(int i=0;i<item.options().size();i++) selected.put(item.options().get(i).name(),variant.values().get(i));
            var request=variant.request();
            return new VariantPreview(item.code(),variant.row(),request.getSku(),Collections.unmodifiableMap(selected),
                    request.getPrice(),request.getComparePrice(),request.getStockQuantity());
        })).toList();
        return new Preview(checked.total(),preview.size(),checked.total()-preview.size(),preview,variantPreview,checked.errors(),token,
                expires==null?null:expires.toString(),checked.items().stream().mapToInt(i -> i.variants().size()).sum(),
                checked.items().stream().mapToLong(this::stock).sum(),"PENDING");
    }
    private long stock(ProductExcelValidation.Item item) { return item.variants().stream().mapToLong(v -> v.request().getStockQuantity()).sum(); }

    public Result confirm(String actor, UUID shop, String token) {
        Session session;
        synchronized(sessions) { evictExpired(); session=sessions.get(token); }
        if(session==null || !session.actor().equals(actor) || !session.shop().equals(shop))
            throw new IllegalArgumentException("Phiên xem trước không hợp lệ hoặc đã hết hạn. Hãy tải file để kiểm tra lại.");
        // Hold the per-token lock through commit; concurrent requests cannot replay it.
        synchronized(session) {
            synchronized(sessions) {
                if(sessions.get(token)!=session || !session.expires().isAfter(Instant.now()))
                    throw new IllegalArgumentException("Phiên đã dùng hoặc hết hạn. Hãy xem trước lại.");
            }
            Result result=transaction.execute(status -> {
                var rows=workbook.read(session.bytes());
                var checked=validation.validate(shop,rows);
                if(!checked.errors().isEmpty()) throw new ProductExcelValidationException(response(checked,null,null));
                List<Issue> changed=new ArrayList<>();
                for(var item : checked.items()) {
                    var original=session.catalog().get(item.code());
                    int row=rows.get("Products").stream().filter(r -> r.get("product_code").equals(item.code())).findFirst().map(ExcelRow::row).orElse(0);
                    if(original==null || !Objects.equals(original.categoryId(),item.request().getCategoryId()))
                        changed.add(new Issue(item.code(),"Products",row,"category_name","Danh mục đã thay đổi sau Preview. Hãy kiểm tra file lại."));
                    if(original==null || !Objects.equals(original.brandId(),item.request().getBrandId()))
                        changed.add(new Issue(item.code(),"Products",row,"brand_name","Thương hiệu đã thay đổi sau Preview. Hãy kiểm tra file lại."));
                }
                if(!changed.isEmpty()) throw new ProductExcelValidationException(new Preview(checked.total(),0,checked.total(),List.of(),List.of(),List.copyOf(changed),null,null,0,0,"PENDING"));
                for(var item : checked.items()) {
                    try { create(item); productRepository.flush(); }
                    catch (org.springframework.dao.DataIntegrityViolationException | IllegalArgumentException |
                           com.example.backend.exception.ResourceNotFoundException failure) {
                        int row=rows.get("Products").stream().filter(r -> r.get("product_code").equals(item.code())).findFirst().map(ExcelRow::row).orElse(0);
                        String message=failure instanceof org.springframework.dao.DataIntegrityViolationException
                                ? "Dữ liệu tham chiếu hoặc ràng buộc đã thay đổi. Hãy kiểm tra file lại." : failure.getMessage();
                        var issue=new Issue(item.code(),"Products",row,"product_code",message);
                        // The whole transaction rolls back; none of these products are imported.
                        throw new ProductExcelValidationException(new Preview(checked.total(),0,checked.total(),List.of(),List.of(),List.of(issue),null,null,0,0,"PENDING"));
                    }
                }
                return new Result(checked.items().size(),checked.items().stream().mapToInt(i -> i.variants().size()).sum(),checked.items().stream().mapToLong(this::stock).sum(),"PENDING");
            });
            synchronized(sessions) { sessions.remove(token,session); }
            return result;
        }
    }
    private void create(ProductExcelValidation.Item item) {
        Product product=products.createProduct(item.request());
        List<Map<String,UUID>> ids=new ArrayList<>();
        for (var option : item.options()) {
            var request=new CreateProductOptionRequest(); request.setName(option.name());
            ProductOption saved=variants.createOption(product.getId(),request);
            saved.setSortOrder((short) ids.size());
            Map<String,UUID> mapping=new HashMap<>();
            for(int i=0;i<option.values().size();i++) {
                var valueRequest=new CreateProductOptionValueRequest(); valueRequest.setValue(option.values().get(i));
                var value=variants.createOptionValue(saved.getId(),valueRequest); value.setSortOrder(option.orders().get(i));
                mapping.put(option.values().get(i),value.getId());
            }
            ids.add(mapping);
        }
        var batch=new GenerateVariantsRequest(); List<CreateProductVariantRequest> requests=new ArrayList<>();
        for(var variant : item.variants()) {
            var request=variant.request(); request.setProductId(product.getId());
            List<UUID> selected=new ArrayList<>();
            for(int i=0;i<ids.size();i++) selected.add(ids.get(i).get(variant.values().get(i)));
            request.setOptionValueIds(selected); requests.add(request);
        }
        batch.setVariants(requests); variants.generateVariants(product.getId(),batch);
        for(var input : item.media()) media.createMediaReference(product.getId(),input.type(),input.url(),input.order());
    }
}
