package com.example.backend.service;

import com.example.backend.dto.request.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import static com.example.backend.service.ProductExcelSchema.*;

/** Read-only validation builds the exact requests later passed to the existing services. */
@Component
public class ProductExcelValidation {
    private final ProductService products;
    private final ProductVariantService variants;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final JsonMapper json = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public ProductExcelValidation(ProductService products, ProductVariantService variants, CategoryRepository categories, BrandRepository brands) {
        this.products=products; this.variants=variants; this.categories=categories; this.brands=brands;
    }
    public record Option(String name, List<String> values, List<Short> orders) {}
    public record Media(ProductMediaType type, String url, short order, boolean thumbnail) {}
    public record Variant(CreateProductVariantRequest request, List<String> values, int row) {}
    public record Item(String code, CreateProductRequest request, String categoryName, String brandName,
                       List<Option> options, List<Variant> variants, List<Media> media) {}
    public record Checked(int total, List<Item> items, List<Issue> errors) {}

    private static class Context {
        final List<Issue> errors = new ArrayList<>();
        <T> T at(ExcelRow row, String column, Supplier<T> task) {
            try { return task.get(); }
            catch (IllegalArgumentException e) { error(row,column,e.getMessage()); return null; }
        }
        void error(ExcelRow row, String column, String message) {
            errors.add(new Issue(row.get("product_code"),row.sheet(),row.row(),column,message));
        }
    }
    public Checked validate(UUID shop, Map<String,List<ExcelRow>> workbook) {
        Context ctx = new Context();
        List<ExcelRow> productRows = workbook.get("Products");
        if (productRows.isEmpty() || productRows.size() > MAX_PRODUCTS)
            throw new IllegalArgumentException("Mỗi file cần từ 1 đến 100 sản phẩm.");
        Set<String> categoryKeys = new HashSet<>(), brandKeys = new HashSet<>();
        for (var row : productRows) {
            categoryKeys.add(ProductOptionValidation.key(row.get("category_name")));
            if (!ProductOptionValidation.clean(row.get("brand_name")).isEmpty()) brandKeys.add(ProductOptionValidation.key(row.get("brand_name")));
        }
        Map<String,List<CategoryRepository.ExcelName>> categoryLookup = new HashMap<>();
        for (var category : categories.findExcelNames()) {
            String key = ProductOptionValidation.key(category.getName());
            if (categoryKeys.contains(key)) categoryLookup.computeIfAbsent(key,k -> new ArrayList<>()).add(category);
        }
        Map<String,List<BrandRepository.ExcelName>> brandLookup = new HashMap<>();
        if (!brandKeys.isEmpty()) for (var brand : brands.findExcelNames()) {
            String key = ProductOptionValidation.key(brand.getName());
            if (brandKeys.contains(key)) brandLookup.computeIfAbsent(key,k -> new ArrayList<>()).add(brand);
        }
        Map<String,List<ExcelRow>> grouped = new LinkedHashMap<>();
        for (ExcelRow row : productRows) grouped.computeIfAbsent(row.get("product_code"), k -> new ArrayList<>()).add(row);
        for (var entry : grouped.entrySet()) {
            for (var row : entry.getValue()) {
                if (entry.getKey().isBlank()) ctx.error(row,"product_code","Mã tham chiếu không được rỗng.");
                if (entry.getKey().length()>255) ctx.error(row,"product_code","Mã tham chiếu tối đa 255 ký tự.");
                if (entry.getValue().size()>1) ctx.error(row,"product_code","Mã tham chiếu bị trùng trong file.");
            }
        }
        for (String sheet : List.of("Options","Variants","Media")) for (ExcelRow row : workbook.get(sheet))
            if (!grouped.containsKey(row.get("product_code"))) ctx.error(row,"product_code","Không tìm thấy sản phẩm được tham chiếu.");
        List<Item> valid = new ArrayList<>();
        for (ExcelRow row : productRows) {
            int before = ctx.errors.size();
            String code = row.get("product_code");
            CreateProductRequest request = new CreateProductRequest();
            request.setShopId(shop); request.setName(ProductOptionValidation.clean(row.get("name")));
            request.setDescription(row.get("description")); request.setFeatured(false);
            if (!row.get("product_id").isBlank()) ctx.error(row,"product_id","Chỉ hỗ trợ tạo mới: để trống product_id trong mẫu tạo mới, hoặc chờ chế độ cập nhật sản phẩm.");
            var category = ctx.at(row,"category_name", () -> {
                String name = catalogName(row.get("category_name"), "Danh mục");
                var matches = categoryLookup.getOrDefault(ProductOptionValidation.key(name),List.of());
                if (matches.isEmpty()) throw new IllegalArgumentException("Danh mục '"+name+"' không tồn tại trong hệ thống.");
                if (matches.size()!=1) throw new IllegalArgumentException("Danh mục '"+name+"' không xác định duy nhất.");
                var match = matches.getFirst();
                if (match.getStatus()!=CategoryStatus.ACTIVE) throw new IllegalArgumentException("Danh mục '"+name+"' hiện không được phép sử dụng.");
                return match;
            });
            request.setCategoryId(category==null?null:category.getId());
            var brand = ProductOptionValidation.clean(row.get("brand_name")).isEmpty() ? null : ctx.at(row,"brand_name", () -> {
                String name = catalogName(row.get("brand_name"), "Thương hiệu");
                var matches = brandLookup.getOrDefault(ProductOptionValidation.key(name),List.of());
                if (matches.isEmpty()) throw new IllegalArgumentException("Thương hiệu '"+name+"' không tồn tại trong hệ thống.");
                if (matches.size()!=1) throw new IllegalArgumentException("Thương hiệu '"+name+"' không xác định duy nhất.");
                return matches.getFirst();
            });
            request.setBrandId(brand==null?null:brand.getId());
            if (category!=null) ctx.at(row,"name", () -> { products.validateCreateRequest(request); return true; });
            else if (request.getName().isBlank() || request.getName().length()>255)
                ctx.error(row,"name","Tên sản phẩm bắt buộc và tối đa 255 ký tự.");
            List<Option> options = options(ctx, rows(workbook,"Options",code));
            List<Variant> selected = variants(ctx, row, rows(workbook,"Variants",code), options);
            List<Media> media = media(ctx, row, rows(workbook,"Media",code), request);
            if (before==ctx.errors.size() && grouped.get(code).size()==1 && !code.isBlank() && code.length()<=255)
                valid.add(new Item(code,request,category.getName(),brand==null?null:brand.getName(),options,selected,media));
        }
        return new Checked(productRows.size(),List.copyOf(valid),List.copyOf(ctx.errors));
    }

    private List<ExcelRow> rows(Map<String,List<ExcelRow>> book, String sheet, String code) {
        return book.get(sheet).stream().filter(r -> r.get("product_code").equals(code)).toList();
    }
    private List<Option> options(Context ctx, List<ExcelRow> rows) {
        Map<String,List<ExcelRow>> groups = new LinkedHashMap<>();
        for (var row : rows) groups.computeIfAbsent(ProductOptionValidation.key(row.get("option_name")),k -> new ArrayList<>()).add(row);
        List<Option> options = new ArrayList<>();
        if (groups.size()>10) ctx.error(rows.getFirst(),"option_name","Tối đa 10 thuộc tính.");
        for (var group : groups.values()) {
            String name = ProductOptionValidation.clean(group.getFirst().get("option_name"));
            List<String> values = new ArrayList<>(); List<Short> orders = new ArrayList<>(); Set<String> seen = new HashSet<>();
            Map<ExcelRow,Short> orderMap = new HashMap<>();
            for (var row : group) {
                if (!ProductOptionValidation.clean(row.get("option_name")).equals(name))
                    ctx.error(row,"option_name","Tên thuộc tính trùng sau chuẩn hóa; dùng cùng một tên cho các giá trị.");
                if (name.isBlank() || name.length()>255) ctx.error(row,"option_name","Tên thuộc tính bắt buộc và tối đa 255 ký tự.");
                String value = ProductOptionValidation.clean(row.get("option_value"));
                if (value.isBlank() || !seen.add(ProductOptionValidation.key(value))) ctx.error(row,"option_value","Giá trị rỗng hoặc trùng sau chuẩn hóa.");
                Short order = ctx.at(row,"sort_order", () -> order(row.get("sort_order")));
                orderMap.put(row,order==null?0:order);
            }
            group.sort(Comparator.comparingInt(orderMap::get));
            for (var row : group) { values.add(ProductOptionValidation.clean(row.get("option_value"))); orders.add(orderMap.get(row)); }
            options.add(new Option(name,List.copyOf(values),List.copyOf(orders)));
        }
        if (!rows.isEmpty()) ctx.at(rows.getFirst(),"option_name", () -> {
            List<SubmitProductRequest.OptionInput> input = options.stream().map(o -> {
                var v = new SubmitProductRequest.OptionInput(); v.setName(o.name()); v.setValues(o.values()); return v;
            }).toList();
            ProductOptionValidation.validateOptions(input); return true;
        });
        return List.copyOf(options);
    }
    private List<Variant> variants(Context ctx, ExcelRow product, List<ExcelRow> rows, List<Option> options) {
        if (rows.isEmpty() || rows.size()>200) ctx.error(product,"product_code","Sản phẩm cần từ 1 đến 200 biến thể.");
        Set<String> skus = new HashSet<>(); Set<List<String>> combinations = new HashSet<>(); List<Variant> result = new ArrayList<>();
        for (var row : rows) {
            var request = new CreateProductVariantRequest();
            // Validation only needs a non-null product ID; actual ID is assigned after creation.
            request.setProductId(new UUID(0,0)); request.setSku(row.get("sku"));
            if (!skus.add(row.get("sku").toLowerCase(Locale.ROOT))) ctx.error(row,"sku","SKU trùng trong sản phẩm.");
            request.setPrice(ctx.at(row,"price", () -> money(row.get("price"))));
            if (!row.get("compare_price").isBlank()) request.setComparePrice(ctx.at(row,"compare_price", () -> money(row.get("compare_price"))));
            request.setStockQuantity(ctx.at(row,"stock_quantity", () -> integer(row.get("stock_quantity"))));
            ctx.at(row,"sku", () -> { variants.validateCreateVariantRequest(request); return true; });
            List<String> values = ctx.at(row,"option_values", () -> selected(row.get("option_values"),options));
            if (values!=null && !combinations.add(values)) ctx.error(row,"option_values","Tổ hợp biến thể bị trùng.");
            if (values!=null) result.add(new Variant(request, values, row.row()));
        }
        return List.copyOf(result);
    }
    private List<String> selected(String input, List<Option> options) {
        try {
            var node = json.readTree(input);
            if (node==null || !node.isObject()) throw new IllegalArgumentException();
            Map<String,String> entries = new HashMap<>();
            for (var property : node.properties()) {
                if (!property.getValue().isString() || entries.put(ProductOptionValidation.key(property.getKey()), ProductOptionValidation.key(property.getValue().asString()))!=null)
                    throw new IllegalArgumentException();
            }
            if (entries.size()!=options.size()) throw new IllegalArgumentException();
            List<String> result = new ArrayList<>();
            for (var option : options) {
                String key = entries.get(ProductOptionValidation.key(option.name()));
                result.add(option.values().stream().filter(v -> ProductOptionValidation.key(v).equals(key)).findFirst().orElseThrow(IllegalArgumentException::new));
            }
            return List.copyOf(result);
        } catch (Exception e) { throw new IllegalArgumentException("Cần JSON object tên thuộc tính → giá trị hợp lệ; đúng một giá trị mỗi thuộc tính, không trùng khóa. Không có thuộc tính dùng {}."); }
    }
    private List<Media> media(Context ctx, ExcelRow product, List<ExcelRow> rows, CreateProductRequest request) {
        List<Media> result = new ArrayList<>();
        for (var row : rows) {
            ProductMediaType type = ctx.at(row,"media_type", () -> {
                try { return ProductMediaType.valueOf(row.get("media_type")); }
                catch (Exception e) { throw new IllegalArgumentException("Chỉ hỗ trợ IMAGE hoặc VIDEO."); }
            });
            String url = ctx.at(row,"url", () -> url(row.get("url")));
            Short order = ctx.at(row,"sort_order", () -> order(row.get("sort_order")));
            Boolean thumbnail = ctx.at(row,"is_thumbnail", () -> {
                if (!Set.of("true","false").contains(row.get("is_thumbnail").toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Chỉ nhận true hoặc false.");
                return Boolean.valueOf(row.get("is_thumbnail"));
            });
            if (Boolean.TRUE.equals(thumbnail) && type!=ProductMediaType.IMAGE) ctx.error(row,"is_thumbnail","Ảnh đại diện phải là IMAGE.");
            if (type!=null && url!=null && order!=null && thumbnail!=null) result.add(new Media(type,url,order,thumbnail));
        }
        result.sort(Comparator.comparingInt(Media::order));
        long images = result.stream().filter(m -> m.type()==ProductMediaType.IMAGE).count();
        long videos = result.stream().filter(m -> m.type()==ProductMediaType.VIDEO).count();
        if (images<1 || images>10 || videos>3) ctx.error(product,"thumbnail_url","Cần 1..10 IMAGE và tối đa 3 VIDEO trong Media.");
        List<Media> marked = result.stream().filter(Media::thumbnail).toList();
        if (marked.size()>1) ctx.error(product,"thumbnail_url","Chỉ được đánh dấu một ảnh đại diện.");
        String explicit = product.get("thumbnail_url");
        if (!explicit.isBlank()) {
            ctx.at(product,"thumbnail_url", () -> url(explicit));
            if (result.stream().noneMatch(m -> m.type()==ProductMediaType.IMAGE && m.url().equals(explicit)) || marked.stream().anyMatch(m -> !m.url().equals(explicit)))
                ctx.error(product,"thumbnail_url","thumbnail_url phải khớp IMAGE trong Media và không xung đột is_thumbnail.");
        }
        String thumbnail = !explicit.isBlank() ? explicit : !marked.isEmpty() ? marked.getFirst().url() : result.stream().filter(m -> m.type()==ProductMediaType.IMAGE).map(Media::url).findFirst().orElse(null);
        request.setThumbnailUrl(thumbnail);
        return List.copyOf(result);
    }
    private String catalogName(String value, String label) {
        String name = ProductOptionValidation.required(value,label);
        if (name.length()>255) throw new IllegalArgumentException(label+" tối đa 255 ký tự.");
        if (name.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("Hãy nhập tên "+label.toLowerCase(Locale.ROOT)+", không nhập UUID.");
        return name;
    }
    private BigDecimal money(String value) {
        try { BigDecimal v=new BigDecimal(value); if(v.signum()<0 || v.scale()>2 || v.precision()-v.scale()>16) throw new IllegalArgumentException(); return v; }
        catch (Exception e) { throw new IllegalArgumentException("Giá phải không âm, tối đa 16 chữ số nguyên và 2 chữ số thập phân."); }
    }
    private int integer(String value) {
        try { if (!value.matches("[0-9]+")) throw new IllegalArgumentException(); return Integer.parseInt(value); }
        catch (Exception e) { throw new IllegalArgumentException("Cần số nguyên không âm tối đa 2147483647."); }
    }
    private short order(String value) {
        int v=integer(value); if (v>32767) throw new IllegalArgumentException("sort_order tối đa 32767."); return (short)v;
    }
    private String url(String value) {
        if (value.length()>4096 || value.chars().anyMatch(c -> c<=32 || c==127)) throw new IllegalArgumentException("URL không hợp lệ hoặc quá 4096 ký tự.");
        try {
            URI uri=URI.create(value);
            if (uri.getUserInfo()!=null || uri.getFragment()!=null) throw new IllegalArgumentException();
            if (value.startsWith("/uploads/") && !value.contains("\\") && !value.contains("..") && !value.contains("%")) return value;
            if (Set.of("http","https").contains(uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT)) && uri.getHost()!=null) return value;
        } catch (Exception ignored) {}
        throw new IllegalArgumentException("URL cần HTTP(S) tuyệt đối hoặc đường dẫn /uploads/ hợp lệ.");
    }
}
