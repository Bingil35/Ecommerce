import axiosClient from '../api/axiosClient';
import type { ProductStats } from './productApi';

export interface ShopOwnerDashboard {
  products: ProductStats;
  totalOrders: number; pendingOrders: number; shippingOrders: number; deliveredOrders: number;
  revenue: number | string; ratingAverage: number | string; reviewCount: number;
  dailyPerformance: { date: string; deliveredOrders: number; revenue: number | string }[];
}
export interface ShopOwnerProductStats {
  items: { id: string; name: string; thumbnailUrl: string | null; slug: string; currentPrice: number | string | null;
    soldLifetime: number; sold30Days: number; revenueLifetime: number | string; revenue30Days: number | string;
    ratingAverage: number | string; ratingCount: number; salesPace: number | string }[];
  page: number; size: number; totalElements: number; totalPages: number; sort: string; shopName: string;
}
export const dashboardApi = {
  async get(days: 7 | 30 = 7): Promise<ShopOwnerDashboard> {
    return (await axiosClient.get<{ data: ShopOwnerDashboard }>('/shop-owner/dashboard', { params: { days } })).data.data;
  },
  async getProductStats(params: { keyword?: string; sort?: string; page?: number; size?: number } = {}): Promise<ShopOwnerProductStats> {
    return (await axiosClient.get<{ data: ShopOwnerProductStats }>('/shop-owner/product-stats', { params })).data.data;
  },
};
