import axiosClient from '../api/axiosClient';
import type { ApiResponse } from '../api/apiTypes';
import type {
  CreateShopRequest,
  ShopRegistrationResponse,
  ShopResponse,
} from './shopTypes';

export const shopApi = {
  /** Khách hàng đăng ký mở gian hàng. Backend cấp thêm role SHOP_OWNER và trả token mới. */
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
};
