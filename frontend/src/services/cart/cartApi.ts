import axios from 'axios';

import axiosClient from '../api/axiosClient';
import type { ApiEnvelope } from '../catalog/productApi';
import type {
  AddToCartPayload,
  CartDto,
  CartShop,
} from './cartTypes';

const dataOf = (response: { data: ApiEnvelope<CartDto> }): CartDto =>
  response.data.data;

export const cartApi = {
  async get(): Promise<CartDto> {
    return dataOf(await axiosClient.get('/customer/cart'));
  },

  async addItem(payload: AddToCartPayload): Promise<CartDto> {
    return dataOf(await axiosClient.post('/customer/cart/items', payload));
  },

  async updateItem(itemId: string, quantity: number): Promise<CartDto> {
    return dataOf(
      await axiosClient.patch(`/customer/cart/items/${itemId}`, { quantity })
    );
  },

  async removeItem(itemId: string): Promise<CartDto> {
    return dataOf(await axiosClient.delete(`/customer/cart/items/${itemId}`));
  },
};

/** Đổi dữ liệu BE (đã gom theo shop) sang dữ liệu giao diện. */
export const toCartShops = (cart: CartDto): CartShop[] =>
  (cart.shops ?? []).map((shop) => ({
    id: shop.subCartId,
    shopId: shop.shopId,
    shopSlug: shop.shopSlug,
    shopName: shop.shopName,
    totalQuantity: Number(shop.totalQuantity ?? 0),
    subtotal: Number(shop.subtotal ?? 0),
    items: (shop.items ?? []).map((item) => ({
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      productSlug: item.productSlug,
      imageUrl: item.imageUrl,
      price: Number(item.unitPrice ?? 0),
      comparePrice: item.comparePrice != null ? Number(item.comparePrice) : null,
      quantity: item.quantity,
      stockQuantity: item.availableStock ?? undefined,
      variantId: item.variantId,
      variantSku: item.sku,
      variantName: item.variantName,
      selectedOptions: item.selectedOptions,
      shopId: shop.shopId,
      shopName: shop.shopName,
      available: item.available,
      lineSubtotal: Number(item.lineSubtotal ?? 0),
    })),
  }));

export const getCartErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message) return message;
    if (error.response?.status === 401) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Không kết nối được máy chủ.';
    }
  }
  return 'Không thể xử lý giỏ hàng. Vui lòng thử lại.';
};