import axiosClient from '../api/axiosClient';

export interface ShopOwnerDashboard {
  from: string; to: string;
  orderCount: number; deliveredOrders: number; pendingOrders: number;
  revenue: number | string; averageOrderValue: number | string;
  dailyPerformance: { date: string; orderCount: number; revenue: number | string }[];
  orderStatusBreakdown: { status: 'PENDING' | 'CONFIRMED' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED'; count: number }[];
  recentOrders: { id: string; orderCode: string; status: 'PENDING' | 'CONFIRMED' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED'; customerName: string | null; createdAt: string }[];
  attention: { lowStock: number; outOfStock: number; pendingProducts: number; unansweredReviews: number };
}
export interface ShopOwnerProductStats {
  items: { id: string; name: string; thumbnailUrl: string | null; slug: string; currentPrice: number | string | null;
    soldLifetime: number; sold30Days: number; revenueLifetime: number | string; revenue30Days: number | string;
    ratingAverage: number | string; ratingCount: number; salesPace: number | string }[];
  page: number; size: number; totalElements: number; totalPages: number; sort: string; shopName: string;
}
export const dashboardApi = {
  async get(from: string, to: string): Promise<ShopOwnerDashboard> {
    return (await axiosClient.get<{ data: ShopOwnerDashboard }>('/shop-owner/dashboard', { params: { from, to } })).data.data;
  },
  async getProductStats(params: { keyword?: string; sort?: string; page?: number; size?: number } = {}): Promise<ShopOwnerProductStats> {
    return (await axiosClient.get<{ data: ShopOwnerProductStats }>('/shop-owner/product-stats', { params })).data.data;
  },
};
