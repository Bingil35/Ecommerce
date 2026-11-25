import axiosClient from '../api/axiosClient';
import type { ApiResponse } from '../api/apiTypes';
import type {
  CreateShopRequest,
  ShopRegistrationResponse,
  ShopResponse,
} from './shopTypes';

export const shopApi = {
  async registrationStatus(): Promise<'PENDING' | 'ACTIVE' | 'CLOSED' | null> {
    const response = await axiosClient.get<ApiResponse<'PENDING' | 'ACTIVE' | 'CLOSED' | null>>('/customer/shop-registration/status');
    return response.data.data;
  },
  /** Khách hàng đăng ký mở gian hàng và chờ quản trị viên duyệt. */
  async register(
    payload: CreateShopRequest
  ): Promise<ShopRegistrationResponse> {
    const response = await axiosClient.post<
      ApiResponse<ShopRegistrationResponse>
    >('/customer/shop-registration', payload);

    return response.data.data;
  },

  /** Lấy gian hàng của chủ shop đang đăng nhập. */
  async getMyShop(): Promise<ShopResponse> {
    const response = await axiosClient.get<ApiResponse<ShopResponse>>(
      '/shop-owner/shop/me'
    );

    return response.data.data;
  },
  async updateMyShop(payload: CreateShopRequest): Promise<ShopResponse> {
    const response = await axiosClient.put<ApiResponse<ShopResponse>>('/shop-owner/shop/me', payload);
    return response.data.data;
  },
  async uploadImage(kind: 'logo' | 'banner', file: File): Promise<ShopResponse> {
    const body = new FormData();
    body.append('file', file);
    body.append('kind', kind);
    const response = await axiosClient.post<ApiResponse<ShopResponse>>('/shop-owner/shop/me/images', body, { headers: { 'Content-Type': undefined } });
    return response.data.data;
  },
};
