import { calculateVoucherDiscount } from '../voucher/voucherApi';
import type { ShopVoucher } from '../voucher/voucherTypes';
import type {
  CartItem,
  CartShop,
  OrderSummary,
  ShopSummary,
} from './cartTypes';

/** Đường dẫn tới trang chủ shop (route `/shop/:slug`), dùng shopSlug BE trả về. */
export const getShopPath = (shop: Pick<CartShop, 'shopSlug'>): string =>
  `/shop/${shop.shopSlug}`;

/** Đường dẫn tới trang chi tiết sản phẩm theo slug, khớp route `/product/:slug`. */
export const getProductPath = (item: Pick<CartItem, 'productSlug'>): string =>
  `/product/${encodeURIComponent(item.productSlug)}`;

/** Sản phẩm hết hàng / ngừng bán thì không cho tích chọn thanh toán. */
export const isItemSelectable = (item: CartItem): boolean =>
  item.available !== false &&
  !(item.stockQuantity != null && item.stockQuantity <= 0);

export const getLineTotal = (item: CartItem): number =>
  item.price * item.quantity;

/** Tính tiền 1 shop từ các sản phẩm đang được tích chọn + voucher của shop. */
export const calcShopSummary = (
  shop: CartShop,
  selectedIds: Set<string>,
  voucher: ShopVoucher | null | undefined
): ShopSummary => {
  const selectedItems = shop.items.filter(
    (item) => selectedIds.has(item.id) && isItemSelectable(item)
  );

  const subtotal = selectedItems.reduce(
    (sum, item) => sum + getLineTotal(item),
    0
  );
  const selectedQuantity = selectedItems.reduce(
    (sum, item) => sum + item.quantity,
    0
  );

  const discount = voucher ? calculateVoucherDiscount(voucher, subtotal) : 0;
  const missingForVoucher =
    voucher && subtotal > 0 && subtotal < voucher.minOrderValue
      ? voucher.minOrderValue - subtotal
      : 0;

  return {
    shopId: shop.shopId,
    selectedCount: selectedItems.length,
    selectedQuantity,
    subtotal,
    voucher: voucher ?? null,
    discount,
    missingForVoucher,
    total: subtotal - discount,
  };
};

/** Tính tiền cả giỏ: voucher shop trước, sau đó voucher tổng trên số tiền còn lại. */
export const calcOrderSummary = (
  shopSummaries: ShopSummary[],
  orderVoucher: ShopVoucher | null | undefined
): OrderSummary => {
  const subtotal = shopSummaries.reduce((s, x) => s + x.subtotal, 0);
  const shopDiscount = shopSummaries.reduce((s, x) => s + x.discount, 0);
  const selectedQuantity = shopSummaries.reduce(
    (s, x) => s + x.selectedQuantity,
    0
  );
  const afterShopDiscount = subtotal - shopDiscount;

  const orderDiscount = orderVoucher
    ? calculateVoucherDiscount(orderVoucher, afterShopDiscount)
    : 0;
  const missingForOrderVoucher =
    orderVoucher &&
    afterShopDiscount > 0 &&
    afterShopDiscount < orderVoucher.minOrderValue
      ? orderVoucher.minOrderValue - afterShopDiscount
      : 0;

  const totalDiscount = shopDiscount + orderDiscount;

  return {
    selectedQuantity,
    subtotal,
    shopDiscount,
    afterShopDiscount,
    orderDiscount,
    missingForOrderVoucher,
    totalDiscount,
    total: subtotal - totalDiscount,
  };
};