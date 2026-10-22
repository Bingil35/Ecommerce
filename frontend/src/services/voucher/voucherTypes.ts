export type VoucherDiscountType = 'PERCENT' | 'FIXED';

export interface ShopVoucher {
  id: string;
  /** null = voucher tổng (áp dụng cho cả đơn) */
  shopId: string | null;
  code: string;
  title: string;
  discountType: VoucherDiscountType;
  /** PERCENT: số %, FIXED: số tiền (VND) */
  discountValue: number;
  /** Giảm tối đa (chỉ dùng cho PERCENT) */
  maxDiscount?: number | null;
  /** Giá trị đơn tối thiểu (tính trên các sản phẩm của shop được chọn) */
  minOrderValue: number;
  /** ISO date string */
  expiresAt?: string | null;
}