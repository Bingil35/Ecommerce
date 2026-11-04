export type PaymentMethod = 'COD' | 'VNPAY' | 'MOMO';
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface CustomerAddress {
  id: string; recipientName: string; recipientPhone: string;
  provinceCity: string; ward: string; addressDetail: string; isDefault: boolean;
}
export interface Coupon {
  id: string; code: string; scope: 'PLATFORM' | 'SHOP'; shopId: string | null; shopName: string | null;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number;
  minimumOrder: number | string | null; maximumDiscount: number | string | null;
  usageLimit: number | null; usedCount: number; startAt: string | null; endAt: string | null;
}
export interface CheckoutPreviewRequest {
  addressId: string; itemIds: string[]; platformCouponCode?: string | null;
  shopCouponCodes?: Record<string, string>;
}
export interface CheckoutPreview {
  addressId: string; recipientName: string; recipientPhone: string; recipientCity: string;
  ward: string; addressDetail: string;
  shops: { shopId: string; shopName: string; itemCount: number; subtotal: number | string;
    shopCouponDiscount: number | string; platformCouponDiscount: number | string;
    discount: number | string; totalAmount: number | string;
    items: { cartItemId: string; price: number | string; comparePrice: number | string | null;
      quantity: number; subtotal: number | string; shopCouponDiscount: number | string;
      platformCouponDiscount: number | string; totalAmount: number | string }[] }[];
  subtotal: number | string; shopDiscount: number | string; platformDiscount: number | string;
  totalDiscount: number | string; totalAmount: number | string; appliedCoupons: string[];
}
export interface PlaceOrderRequest extends CheckoutPreviewRequest { paymentMethod: PaymentMethod }
export interface OrderListItem {
  id: string; orderCode: string; paymentMethod: PaymentMethod | null; paymentStatus: PaymentStatus;
  subtotal: number | string; discountAmount: number | string; totalAmount: number | string;
  createdAt: string; subOrderCount: number;
}
export interface PageResponse<T> {
  items: T[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean;
}
export interface OrderDetail {
  id: string; orderCode: string; paymentMethod: PaymentMethod | null; paymentStatus: PaymentStatus;
  subtotal: number | string; discountAmount: number | string; totalAmount: number | string; createdAt: string;
  subOrders: {
    id: string; shopId: string | null; shopName: string | null; status: OrderStatus;
    subtotal: number | string; discountAmount: number | string; totalAmount: number | string;
    shippingAddress: { recipientName: string; recipientPhone: string; recipientCity: string; ward: string; addressDetail: string } | null;
    statusHistory: { oldStatus: OrderStatus | null; newStatus: OrderStatus; note: string | null; createdAt: string }[];
    items: { id: string; productId: string | null; variantId: string | null; productName: string; price: number | string; quantity: number; discountAmount: number | string; totalAmount: number | string }[];
  }[];
}
export interface PaymentConfirmation {
  orderId: string; orderCode: string; paymentStatus: PaymentStatus; confirmedAt: string; message: string;
}
export interface ShopOwnerOrder {
  id: string; orderCode: string; customerName: string; status: OrderStatus; paymentMethod: PaymentMethod | null; paymentStatus: PaymentStatus;
  subtotal: number | string; discountAmount: number | string; totalAmount: number | string; createdAt: string;
  shippingAddress: { recipientName: string; recipientPhone: string; recipientCity: string; ward: string; addressDetail: string } | null;
  statusHistory: { oldStatus: OrderStatus | null; newStatus: OrderStatus; note: string | null; createdAt: string }[];
  items: OrderDetail['subOrders'][number]['items'];
}

export interface PlaceOrderResult { id: string; orderCode: string; paymentMethod: PaymentMethod; paymentStatus: PaymentStatus; subtotal: number | string; discountAmount: number | string; totalAmount: number | string; createdAt: string; subOrders: unknown[]; }
