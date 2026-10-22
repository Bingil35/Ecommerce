import type { ShopVoucher } from '../voucher/voucherTypes';

/* ========== Dữ liệu BE trả về (giỏ hàng đã gom theo shop) ========== */

export interface CartItemDto {
  id: string;
  productId: string;
  productSlug: string;
  productName: string;
  variantId?: string | null;
  /** Tên phân loại BE ghép sẵn "Tên: Giá trị", ví dụ "Dung tích: 50ml"; null nếu variant chưa gắn option */
  variantName?: string | null;
  selectedOptions?: { optionName: string; value: string }[];
  sku?: string | null;
  /** Ảnh variant; BE đã tự lấy ảnh sản phẩm nếu variant không có ảnh */
  imageUrl?: string | null;
  unitPrice: number | string;
  /** Giá gốc, null nếu không có */
  comparePrice?: number | string | null;
  quantity: number;
  lineSubtotal?: number | string | null;
  availableStock?: number | null;
  available?: boolean;
}

export interface CartShopDto {
  subCartId: string;
  shopId: string;
  shopSlug: string;
  shopName: string;
  totalQuantity?: number | string;
  subtotal?: number | string;
  items: CartItemDto[];
}

export interface CartDto {
  cartId: string | null;
  shops: CartShopDto[];
  totalQuantity?: number | string;
  subtotal?: number | string;
}

export interface AddToCartPayload {
  productId: string;
  variantId?: string | null;
  quantity: number;
}

/* ========== Dữ liệu dùng cho giao diện ========== */

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productSlug: string;
  imageUrl?: string | null;
  price: number;
  comparePrice?: number | null;
  quantity: number;
  stockQuantity?: number;
  variantId?: string | null;
  variantSku?: string | null;
  variantName?: string | null;
  selectedOptions?: { optionName: string; value: string }[];
  shopId: string;
  shopName?: string;
  available?: boolean;
  lineSubtotal?: number;
}

export interface CartShop {
  /** subCartId của BE */
  id: string;
  shopId: string;
  shopSlug: string;
  shopName: string;
  totalQuantity: number;
  subtotal: number;
  items: CartItem[];
}

/** Kết quả tính tiền của 1 shop dựa trên các sản phẩm đang được tích chọn. */
export interface ShopSummary {
  shopId: string;
  selectedCount: number;
  selectedQuantity: number;
  /** Tổng tiền các sản phẩm đã chọn (chưa trừ voucher) */
  subtotal: number;
  /** Voucher shop đang chọn (có thể chưa đủ điều kiện) */
  voucher: ShopVoucher | null;
  /** Số tiền thực sự được giảm (0 nếu chưa đạt điều kiện) */
  discount: number;
  /** Số tiền còn thiếu để đạt đơn tối thiểu của voucher (0 nếu đã đủ) */
  missingForVoucher: number;
  /** Tổng tiền shop sau khi trừ voucher */
  total: number;
}

/** Kết quả tính tiền cả giỏ (các sản phẩm đã chọn) sau voucher shop và voucher tổng. */
export interface OrderSummary {
  selectedQuantity: number;
  /** Tổng tiền trước khi giảm */
  subtotal: number;
  /** Tổng giảm từ các voucher shop */
  shopDiscount: number;
  /** Tổng tiền sau khi trừ voucher shop (căn cứ để xét voucher tổng) */
  afterShopDiscount: number;
  /** Giảm từ voucher tổng (0 nếu chưa đủ điều kiện) */
  orderDiscount: number;
  /** Số tiền còn thiếu để voucher tổng đủ điều kiện */
  missingForOrderVoucher: number;
  /** Tổng giảm = shopDiscount + orderDiscount */
  totalDiscount: number;
  /** Số tiền phải trả (sau giảm) */
  total: number;
}

export interface CartContextValue {
  shops: CartShop[];
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  addToCart: (payload: AddToCartPayload) => Promise<boolean>;
  updateQuantity: (itemId: string, quantity: number) => Promise<boolean>;
  removeFromCart: (itemId: string) => Promise<boolean>;
}