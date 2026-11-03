import axios from 'axios';
import { checkoutApi } from '../checkout/checkoutApi';
import type { ShopVoucher } from './voucherTypes';

const mapCoupon = (coupon: Awaited<ReturnType<typeof checkoutApi.getCoupons>>[number]): ShopVoucher => ({
  id: coupon.id,
  shopId: coupon.shopId,
  code: coupon.code,
  title: coupon.discountType === 'PERCENTAGE'
    ? `Giảm ${coupon.discountValue}%${coupon.maximumDiscount ? ` tối đa ${Number(coupon.maximumDiscount).toLocaleString('vi-VN')}₫` : ''}`
    : `Giảm ${Number(coupon.discountValue).toLocaleString('vi-VN')}₫`,
  discountType: coupon.discountType === 'PERCENTAGE' ? 'PERCENT' : 'FIXED',
  discountValue: Number(coupon.discountValue),
  maxDiscount: coupon.maximumDiscount == null ? null : Number(coupon.maximumDiscount),
  minOrderValue: Number(coupon.minimumOrder ?? 0),
  expiresAt: coupon.endAt,
});

export const USE_MOCK_VOUCHERS = false;
export const IS_VOUCHER_READY = true;

export const voucherApi = {
  async getShopVouchers(shopId: string): Promise<ShopVoucher[]> {
    try {
      const coupons = await checkoutApi.getCoupons('SHOP');
      return coupons.filter((coupon) => coupon.shopId === shopId).map(mapCoupon);
    } catch (error) {
      if (axios.isAxiosError(error)) throw error;
      return [];
    }
  },
  async getOrderVouchers(): Promise<ShopVoucher[]> {
    const coupons = await checkoutApi.getCoupons('PLATFORM');
    return coupons.map(mapCoupon);
  },
};

export const calculateVoucherDiscount = (voucher: ShopVoucher, subtotal: number): number => {
  if (subtotal <= 0 || subtotal < voucher.minOrderValue) return 0;
  let discount = voucher.discountType === 'PERCENT'
    ? Math.floor((subtotal * voucher.discountValue) / 100)
    : voucher.discountValue;
  if (voucher.discountType === 'PERCENT' && voucher.maxDiscount != null && voucher.maxDiscount > 0) {
    discount = Math.min(discount, voucher.maxDiscount);
  }
  return Math.max(0, Math.min(discount, subtotal));
};
