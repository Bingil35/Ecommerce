package com.example.backend.dto.request;

/**
 * Điều chỉnh tồn kho. Truyền ĐÚNG MỘT trong hai:
 *  - delta       : cộng/trừ (số âm để giảm), khác 0
 *  - newQuantity : đặt tồn kho thành số này (kiểm kê)
 * reason tùy chọn (lý do kiểm kê, hao hụt...), chỉ được ghi vào nhật ký ứng dụng.
 */
public record StockAdjustRequest(Integer delta, Integer newQuantity, String reason) {
}
