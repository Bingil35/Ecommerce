package com.example.backend.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;
import static com.example.backend.service.ProductExcelSchema.*;

@Component
public class ProductExcelWorkbook {
    private static final long MAX_EXPANDED_BYTES = 32L * 1024 * 1024;
    public static final class FormatException extends IllegalArgumentException {
        private final Issue issue;
        FormatException(String sheet, int row, String column, String message) {
            super(message); issue = new Issue("",sheet,row,column,message);
        }
        public Issue issue() { return issue; }
    }

    public byte[] write(Map<String, List<List<String>>> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (var entry : COLUMNS.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                Row header = sheet.createRow(0);
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                for (int i = 0; i < entry.getValue().size(); i++) {
                    Cell cell = header.createCell(i, CellType.STRING);
                    cell.setCellValue(entry.getValue().get(i)); cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, Math.min(70, i == 0 ? 38 : 28) * 256);
                    sheet.setDefaultColumnStyle(i, textStyle);
                    if (required(entry.getKey(), entry.getValue().get(i))) {
                        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                        anchor.setCol1(i); anchor.setCol2(i+3); anchor.setRow1(0); anchor.setRow2(3);
                        Comment comment = drawing.createCellComment(anchor);
                        comment.setAuthor("Kala");
                        comment.setString(workbook.getCreationHelper().createRichTextString("Bắt buộc. Xem quy tắc trong sheet Guide."));
                        cell.setCellComment(comment);
                    }
                }
                sheet.createFreezePane(0, 1);
                int rowIndex = 1;
                List<List<String>> rows = entry.getKey().equals("Guide") ? guide() : data.getOrDefault(entry.getKey(), List.of());
                for (var values : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < values.size(); i++) {
                        // Explicit STRING prevents formula injection, including leading =, +, -, @.
                        Cell cell = row.createCell(i, CellType.STRING);
                        cell.setCellValue(values.get(i) == null ? "" : values.get(i));
                    }
                }
                if (!entry.getKey().equals("Guide")) sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0,rowIndex-1), 0, entry.getValue().size()-1));
            }
            workbook.write(output); return output.toByteArray();
        } catch (IOException e) { throw new IllegalArgumentException("Không thể tạo file Excel."); }
    }

    private boolean required(String sheet, String column) {
        return switch (sheet) {
            case "Products" -> Set.of("product_code","name","category_name").contains(column);
            case "Options", "Media" -> true;
            case "Variants" -> !column.equals("compare_price");
            default -> false;
        };
    }

    private List<List<String>> guide() {
        return List.of(
            List.of("schema_version", VERSION),
            List.of("required", "Products: product_code, name, category_name. brand_name tùy chọn. Variants: product_code, sku, option_values, price, stock_quantity. Media: product_code, media_type, url, sort_order, is_thumbnail. Options: tất cả cột."),
            List.of("catalog_names", "Nhập tên danh mục và thương hiệu, không nhập UUID. Backend chuẩn hóa Unicode NFC, khoảng trắng, hoa/thường và giữ dấu; tên phải xác định duy nhất. Danh mục phải ACTIVE. Không tạo danh mục hoặc thương hiệu mới."),
            List.of("version", "KALA_PRODUCT_V3: Variants có đúng 6 cột: product_code, sku, option_values, price, compare_price, stock_quantity. File V1/V2 không được hỗ trợ; tải mẫu V3 mới."),
            List.of("variant_identity", "variant_id được hệ thống Kala tự động tạo khi lưu biến thể. Chủ cửa hàng chỉ cần nhập SKU, tổ hợp thuộc tính, giá và tồn kho. Không thêm cột ID biến thể vào Excel; SKU là mã nghiệp vụ, không phải khóa chính."),
            List.of("create_only", "Để trống product_id khi tạo mới. File export giữ product_id hiện có: không thể nhập tạo mới trước khi chuyển sang mẫu tạo mới và kiểm tra lại dữ liệu. Chưa hỗ trợ cập nhật sản phẩm."),
            List.of("product_code", "Mã tham chiếu duy nhất trong file, không phải SKU. Các sheet phải tham chiếu đúng mã."),
            List.of("options", "NFC, gộp khoảng trắng, so sánh không phân biệt hoa/thường, giữ dấu. Nhóm cùng option_name; mỗi giá trị chỉ một dòng. Thứ tự option theo lần xuất hiện đầu tiên; giá trị theo sort_order rồi thứ tự dòng."),
            List.of("option_values", "JSON object tên thuộc tính → giá trị, ví dụ {\"Màu sắc\":\"Đen\",\"RAM\":\"16GB\"}. Không có thuộc tính: {}. Mỗi biến thể phải chọn đúng một giá trị của mỗi thuộc tính."),
            List.of("sku", "Bắt buộc, tối đa 255 ký tự Latin/số/_/-, bắt đầu bằng chữ hoặc số; duy nhất không phân biệt hoa/thường trong từng sản phẩm (theo backend hiện tại)."),
            List.of("numbers", "Giá không âm, tối đa 16 chữ số nguyên và 2 thập phân. compare_price để trống hoặc không âm. stock_quantity là số nguyên từ 0 đến 2147483647. sort_order: 0..32767."),
            List.of("media", "Tối thiểu 1 và tối đa 10 IMAGE, tối đa 3 VIDEO. URL HTTP(S) tuyệt đối hoặc đường dẫn media nội bộ /uploads/; không tải URL về server."),
            List.of("thumbnail", "Tối đa một IMAGE is_thumbnail=true. Nếu thumbnail_url có giá trị thì phải khớp IMAGE trong Media và ảnh được đánh dấu. Nếu bỏ trống cả hai, dùng IMAGE đầu theo sort_order."),
            List.of("limits", "Chỉ .xlsx; tối đa 5 MiB, 100 sản phẩm, 200 biến thể/sản phẩm, 10 thuộc tính, 30000 dòng tổng, 200000 ô. Đúng 5 sheet. Không công thức, macro hoặc liên kết workbook ngoài."),
            List.of("example_product", "Products: [trống], LAPTOP01, Laptop Dell Inspiron 15, Laptop, Dell, Mô tả, https://example.com/laptop.jpg (danh mục Laptop và thương hiệu Dell phải có sẵn)"),
            List.of("example_option", "Options: LAPTOP01, Màu sắc, Đen, 0"),
            List.of("example_variant", "Variants: LAPTOP01, LAPTOP-DEN, {\"Màu sắc\":\"Đen\"}, 18000000, [trống], 20"),
            List.of("example_media", "Media: LAPTOP01, IMAGE, https://example.com/laptop.jpg, 0, true"),
            List.of("errors", "ID khác rỗng: chỉ hỗ trợ tạo mới. Tên danh mục/thương hiệu: phải tồn tại và không trùng; không nhập UUID. Danh mục phải ACTIVE. JSON: dùng dấu ngoặc kép. SKU/tổ hợp trùng: sửa dòng trùng. Không thêm cột shop_id/status."),
            List.of("confirm", "Preview không ghi database. Chỉ confirm khi không có lỗi; token 15 phút, một lần dùng, gắn tài khoản/cửa hàng/nội dung. Import toàn bộ hoặc rollback toàn bộ; sản phẩm PENDING chờ duyệt.")
        );
    }

    public Map<String, List<ExcelRow>> read(byte[] bytes) {
        inspectZip(bytes);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getWorkbookType() != org.apache.poi.xssf.usermodel.XSSFWorkbookType.XLSX ||
                workbook.getNumberOfSheets() != COLUMNS.size() || !workbook.getExternalLinksTable().isEmpty())
                throw new IllegalArgumentException("File phải có đúng 5 sheet và không có liên kết workbook ngoài.");
            checkVersion(workbook);
            Map<String, List<ExcelRow>> result = new LinkedHashMap<>();
            int rows = 0, cells = 0;
            for (var entry : COLUMNS.entrySet()) {
                Sheet sheet = workbook.getSheet(entry.getKey());
                if (sheet == null || sheet.getRow(0) == null) throw new FormatException(entry.getKey(),1,"","Thiếu sheet/header " + entry.getKey());
                if (sheet.getLastRowNum() > MAX_ROWS || sheet.getRow(0).getLastCellNum() != entry.getValue().size())
                    throw new FormatException(entry.getKey(),1,"","Sai số cột hoặc vượt giới hạn dòng: " + entry.getKey());
                List<ExcelRow> values = new ArrayList<>();
                for (Row row : sheet) {
                    if (++rows > MAX_ROWS) throw new IllegalArgumentException("Vượt giới hạn 30000 dòng.");
                    if (row.getLastCellNum() > entry.getValue().size()) throw new FormatException(entry.getKey(),row.getRowNum()+1,"","Có cột ngoài schema.");
                    Map<String, String> value = new LinkedHashMap<>();
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        if (++cells > MAX_CELLS) throw new IllegalArgumentException("Vượt giới hạn 200000 ô.");
                        String text = value(row.getCell(i), entry.getKey(), row.getRowNum()+1, entry.getValue().get(i));
                        if (row.getRowNum() == 0 && !text.equals(entry.getValue().get(i)))
                            throw new FormatException(entry.getKey(),1,entry.getValue().get(i),"Sai tên hoặc thứ tự header. Vui lòng tải mẫu "+VERSION+"; sheet Variants có đúng 6 cột.");
                        value.put(entry.getValue().get(i), text);
                    }
                    if (row.getRowNum() != 0 && value.values().stream().anyMatch(v -> !v.isBlank()))
                        values.add(new ExcelRow(entry.getKey(),row.getRowNum()+1,Collections.unmodifiableMap(value)));
                }
                result.put(entry.getKey(), List.copyOf(values));
            }
            List<ExcelRow> guide = result.get("Guide");
            if (guide.stream().filter(r -> r.get("key").equals("schema_version")).count() != 1 ||
                guide.stream().noneMatch(r -> r.get("key").equals("schema_version") && r.get("value").equals(VERSION)))
                throw new FormatException("Guide",2,"value","Schema không hợp lệ. Yêu cầu " + VERSION);
            return result;
        } catch (IllegalArgumentException e) { throw e; }
          catch (Exception e) { throw new IllegalArgumentException("File không phải workbook .xlsx hợp lệ hoặc vượt giới hạn an toàn."); }
    }

    private void checkVersion(XSSFWorkbook workbook) {
        Sheet guide=workbook.getSheet("Guide");
        if (guide==null || guide.getLastRowNum()>MAX_ROWS)
            throw new FormatException("Guide",1,"schema_version","Thiếu Guide hoặc vượt giới hạn. Hãy tải mẫu mới "+VERSION+".");
        int found=0;
        for (Row row : guide) {
            if (row.getRowNum()==0) continue;
            if (value(row.getCell(0),"Guide",row.getRowNum()+1,"key").equals("schema_version")) {
                found++;
                String version=value(row.getCell(1),"Guide",row.getRowNum()+1,"value");
                if (!VERSION.equals(version)) {
                    String message=Set.of("KALA_PRODUCT_V1","KALA_PRODUCT_V2").contains(version)
                            ? "File Excel đang sử dụng phiên bản cũ. Vui lòng tải mẫu "+VERSION+"."
                            : "Phiên bản Excel không được hỗ trợ. Vui lòng tải mẫu "+VERSION+".";
                    throw new FormatException("Guide",row.getRowNum()+1,"value",message);
                }
            }
        }
        if (found!=1) throw new FormatException("Guide",2,"value","Cần đúng một schema_version = "+VERSION+". Hãy tải mẫu mới.");
    }

    private String value(Cell cell, String sheet, int row, String column) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case BLANK -> "";
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> throw new FormatException(sheet,row,column,"Không chấp nhận công thức hoặc ô lỗi.");
        };
    }

    private void inspectZip(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("File rỗng hoặc vượt 5 MiB.");
        long total = 0; int count = 0; boolean contentTypes = false;
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry; byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (++count > 256 || !names.add(name) || name.contains("..") || name.endsWith(".bin") || name.contains("externallinks/") || name.contains("activex/") || name.contains("embeddings/"))
                    throw new IllegalArgumentException("Workbook chứa nội dung không được hỗ trợ.");
                if (name.equals("[content_types].xml")) contentTypes = true;
                int size;
                while ((size = zip.read(buffer)) != -1) {
                    total += size;
                    if (total > MAX_EXPANDED_BYTES) throw new IllegalArgumentException("Workbook giải nén vượt 32 MiB.");
                }
            }
            if (!contentTypes) throw new IllegalArgumentException("File không phải OOXML .xlsx.");
        } catch (IOException e) { throw new IllegalArgumentException("Không đọc được cấu trúc .xlsx."); }
    }
}
