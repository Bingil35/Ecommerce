package com.example.backend.service;

import com.example.backend.exception.BusinessException;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

/**
 * Bảo toàn dữ liệu giao dịch khi xóa sản phẩm/biến thể.
 *
 * Schema có các bảng tham chiếu sản phẩm/biến thể mà code chưa có entity:
 *   orderdetails.variant_id         -> productvariants   (lịch sử đơn hàng: PHẢI bảo toàn)
 *   productreviews.product_id/variant_id                 (đánh giá của khách: PHẢI bảo toàn)
 *   recentlyviewedproduct.product_id -> products         (nhật ký xem: an toàn để xóa)
 *
 * Không tạo entity mới và không đổi schema nên dùng SQL trực tiếp.
 * Mọi kiểm tra phải được gọi TRƯỚC khi xóa bất kỳ dữ liệu nào.
 */
@Component
public class ProductHistoryGuard {

    private static final String TABLE_ORDER_DETAILS = "orderdetails";
    private static final String TABLE_PRODUCT_REVIEWS = "productreviews";
    private static final String TABLE_RECENTLY_VIEWED = "recentlyviewedproduct";

    private final NamedParameterJdbcTemplate jdbc;

    public ProductHistoryGuard(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ném BusinessException nếu sản phẩm (hoặc bất kỳ biến thể nào của nó) đã có
     * đơn hàng hoặc đánh giá.
     */
    public void assertProductDeletable(
            UUID productId,
            String productName,
            Collection<UUID> variantIds
    ) {
        String label = productName == null || productName.isBlank()
                ? "Sản phẩm"
                : "Sản phẩm \"" + productName + "\"";

        if (hasOrders(variantIds)) {
            throw new BusinessException(
                    label + " đã phát sinh đơn hàng nên không thể xóa. "
                            + "Hãy chuyển sang INACTIVE để ngừng kinh doanh."
            );
        }

        if (hasReviews(productId, variantIds)) {
            throw new BusinessException(
                    label + " đã có đánh giá của khách hàng nên không thể xóa. "
                            + "Hãy chuyển sang INACTIVE để ngừng kinh doanh."
            );
        }
    }

    /** Ném BusinessException nếu biến thể đã có đơn hàng hoặc đánh giá. */
    public void assertVariantDeletable(UUID variantId, String sku) {

        String label = sku == null || sku.isBlank()
                ? "Biến thể"
                : "Biến thể \"" + sku + "\"";

        if (hasOrders(java.util.List.of(variantId))) {
            throw new BusinessException(
                    label + " đã phát sinh đơn hàng nên không thể xóa. "
                            + "Hãy chuyển biến thể sang INACTIVE."
            );
        }

        if (hasReviews(null, java.util.List.of(variantId))) {
            throw new BusinessException(
                    label + " đã có đánh giá của khách hàng nên không thể xóa. "
                            + "Hãy chuyển biến thể sang INACTIVE."
            );
        }
    }

    /** Nhật ký xem gần đây không phải lịch sử giao dịch; xóa để không vi phạm khóa ngoại. */
    public void deleteViewHistory(UUID productId) {
        jdbc.update(
                "delete from " + TABLE_RECENTLY_VIEWED + " where product_id = :productId",
                new MapSqlParameterSource("productId", productId)
        );
    }

    boolean hasOrders(Collection<UUID> variantIds) {

        if (variantIds == null || variantIds.isEmpty()) {
            return false;
        }

        Boolean exists = jdbc.queryForObject(
                "select exists (select 1 from " + TABLE_ORDER_DETAILS
                        + " where variant_id in (:variantIds))",
                new MapSqlParameterSource("variantIds", variantIds),
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }

    boolean hasReviews(UUID productId, Collection<UUID> variantIds) {

        boolean hasVariants = variantIds != null && !variantIds.isEmpty();

        if (productId == null && !hasVariants) {
            return false;
        }

        StringBuilder sql = new StringBuilder(
                "select exists (select 1 from " + TABLE_PRODUCT_REVIEWS + " where ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (productId != null) {
            sql.append("product_id = :productId");
            params.addValue("productId", productId);
        }

        if (hasVariants) {
            if (productId != null) {
                sql.append(" or ");
            }
            sql.append("variant_id in (:variantIds)");
            params.addValue("variantIds", variantIds);
        }

        sql.append(")");

        return Boolean.TRUE.equals(
                jdbc.queryForObject(sql.toString(), params, Boolean.class)
        );
    }
}
