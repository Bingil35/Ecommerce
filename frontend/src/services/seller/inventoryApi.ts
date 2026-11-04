import axiosClient from '../api/axiosClient';

export interface InventoryVariant {
  variantId: string; productId: string; productName: string; sku: string | null;
  quantity: number; reservedQuantity: number; soldQuantity: number; availableQuantity: number;
  lowStockThreshold: number | null; effectiveLowStockThreshold: number; lowStock: boolean;
}
type Envelope<T> = { data: T };
export const inventoryApi = {
  async listShop(): Promise<InventoryVariant[]> { return (await axiosClient.get<Envelope<InventoryVariant[]>>('/shop-owner/inventory')).data.data; },
  async listProduct(productId: string): Promise<InventoryVariant[]> { return (await axiosClient.get<Envelope<InventoryVariant[]>>(`/shop-owner/products/${productId}/inventory`)).data.data; },
  async stockIn(variantId: string, quantity: number, note?: string): Promise<InventoryVariant> {
    return (await axiosClient.post<Envelope<InventoryVariant>>(`/shop-owner/variants/${variantId}/inventory/stock-in`, { quantity, note })).data.data;
  },
};
