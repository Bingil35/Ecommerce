import type { ShopVoucher } from './voucherTypes';

/**
 * ⚠️ Chức năng voucher shop: LÀM Ở SCRUM KHÁC.
 *
 * Hiện tại BE/FE chưa có API voucher, nên đang dùng dữ liệu giả (mock) để giao
 * diện giỏ hàng và logic tính lại giá chạy được.
 * Khi làm voucher thật: đặt USE_MOCK_VOUCHERS = false và gọi API thật trong
 * getShopVouchers (axiosClient). Phần còn lại của giỏ hàng không cần sửa.
 */
export const USE_MOCK_VOUCHERS = true;

/**
 * false: bấm vào ô voucher chỉ hiện alert "làm voucher shop ở scrum khác".
 * Khi làm xong voucher ở scrum khác thì đổi thành true để mở modal chọn voucher.
 */
export const IS_VOUCHER_READY = false;

const buildMockVouchers = (shopId: string): ShopVoucher[] => [
  {
    id: `${shopId}-SHOP10`,
    shopId,
    code: 'SHOP10',
    title: 'Giảm 10% tối đa 50.000₫',
    discountType: 'PERCENT',
    discountValue: 10,
    maxDiscount: 50000,
    minOrderValue: 200000,
    expiresAt: '2026-12-31T23:59:59',
  },
  {
    id: `${shopId}-GIAM30K`,
    shopId,
    code: 'GIAM30K',
    title: 'Giảm 30.000₫',
    discountType: 'FIXED',
    discountValue: 30000,
    maxDiscount: null,
    minOrderValue: 150000,
    expiresAt: '2026-12-31T23:59:59',
  },
  {
    id: `${shopId}-BIGSALE100`,
    shopId,
    code: 'BIGSALE100',
    title: 'Giảm 100.000₫ cho đơn từ 1 triệu',
    discountType: 'FIXED',
    discountValue: 100000,
    maxDiscount: null,
    minOrderValue: 1000000,
    expiresAt: '2026-12-31T23:59:59',
  },
];

/** Voucher tổng: áp dụng cho cả đơn, tính trên tổng tiền sau khi đã trừ voucher shop. */
const buildMockOrderVouchers = (): ShopVoucher[] => [
  {
    id: 'ORDER-KALA5',
    shopId: null,
    code: 'KALA5',
    title: 'Giảm 5% tối đa 100.000₫',
    discountType: 'PERCENT',
    discountValue: 5,
    maxDiscount: 100000,
    minOrderValue: 300000,
    expiresAt: '2026-12-31T23:59:59',
  },
  {
    id: 'ORDER-KALA50K',
    shopId: null,
    code: 'KALA50K',
    title: 'Giảm 50.000₫ cho đơn từ 500.000₫',
    discountType: 'FIXED',
    discountValue: 50000,
    maxDiscount: null,
    minOrderValue: 500000,
    expiresAt: '2026-12-31T23:59:59',
  },
];

export const voucherApi = {
  async getShopVouchers(shopId: string): Promise<ShopVoucher[]> {
    if (USE_MOCK_VOUCHERS) {
      await new Promise((resolve) => setTimeout(resolve, 150));
      return buildMockVouchers(shopId);
    }

    // TODO (SCRUM khác): gọi API lấy voucher của shop
    return [];
  },

  async getOrderVouchers(): Promise<ShopVoucher[]> {
    if (USE_MOCK_VOUCHERS) {
      await new Promise((resolve) => setTimeout(resolve, 150));
      return buildMockOrderVouchers();
    }

    // TODO (SCRUM khác): gọi API lấy voucher tổng
    return [];
  },
};

/**
 * Tính số tiền được giảm của 1 voucher trên tổng tiền các sản phẩm đã chọn.
 * Trả về 0 nếu chưa đạt đơn tối thiểu.
 */
export const calculateVoucherDiscount = (
  voucher: ShopVoucher,
  subtotal: number
): number => {
  if (subtotal <= 0 || subtotal < voucher.minOrderValue) return 0;

  let discount =
    voucher.discountType === 'PERCENT'
      ? Math.floor((subtotal * voucher.discountValue) / 100)
      : voucher.discountValue;

  if (
    voucher.discountType === 'PERCENT' &&
    voucher.maxDiscount != null &&
    voucher.maxDiscount > 0
  ) {
    discount = Math.min(discount, voucher.maxDiscount);
  }

  // Không bao giờ giảm quá tổng tiền
  return Math.max(0, Math.min(discount, subtotal));
};