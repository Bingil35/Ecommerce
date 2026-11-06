import axiosClient from '../api/axiosClient';
import type { ProductStats } from './productApi';

export interface ShopOwnerDashboard {
  products: ProductStats;
  totalOrders: number; pendingOrders: number; shippingOrders: number; deliveredOrders: number;
  revenue: number | string; ratingAverage: number | string; reviewCount: number;
  dailyPerformance: { date: string; deliveredOrders: number; revenue: number | string }[];
}
export const dashboardApi = {
  async get(days: 7 | 30 = 7): Promise<ShopOwnerDashboard> {
    return (await axiosClient.get<{ data: ShopOwnerDashboard }>('/shop-owner/dashboard', { params: { days } })).data.data;
  },
};
