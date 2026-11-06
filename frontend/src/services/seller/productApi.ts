import axiosClient from '../api/axiosClient';

type Envelope<T> = { success: boolean; message: string; data: T };
export type ProductStatus = 'PENDING' | 'REJECTED' | 'ACTIVE' | 'INACTIVE';
export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
export interface SellerProduct {
  id: string; name: string; sku: string | null; thumbnailUrl: string | null;
  categoryId: string | null; categoryName: string | null;
  description: string | null; status: ProductStatus;
  minPrice: number | null; maxPrice: number | null; totalStock: number | null;
  variantCount: number | null; createdAt: string | null;
  slug?: string | null; shopId?: string | null; brandId?: string | null; featured?: boolean | null;
  ratingAverage?: number | null; ratingCount?: number | null; updatedAt?: string | null;
}
export interface ProductPage { items: SellerProduct[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean }
export interface ProductStats { all: number; pending: number; rejected: number; selling: number; outOfStock: number; hidden: number; lowStock: number }
export interface ProductFilters { keyword?: string; categoryId?: string; status?: ProductStatus; stockStatus?: StockStatus; minPrice?: number; maxPrice?: number; createdFrom?: string; createdTo?: string; sort?: string; page?: number; size?: number }
export interface ProductPayload { name: string; description?: string; categoryId: string; thumbnailUrl?: string; featured?: boolean }
const root = '/shop-owner/products';
export const sellerProductApi = {
  async list(filters: ProductFilters) { const r = await axiosClient.get<Envelope<ProductPage>>(root, { params: filters }); return r.data.data; },
  async stats() { const r = await axiosClient.get<Envelope<ProductStats>>(`${root}/stats`); return r.data.data; },
  async get(id: string) { const r = await axiosClient.get<Envelope<SellerProduct>>(`${root}/${id}`); return r.data.data; },
  async create(payload: ProductPayload) { const r = await axiosClient.post<Envelope<SellerProduct>>(root, payload); return r.data.data; },
  async update(id: string, payload: Partial<ProductPayload>) { const r = await axiosClient.put<Envelope<SellerProduct>>(`${root}/${id}`, payload); return r.data.data; },
  async remove(id: string) { await axiosClient.delete(`${root}/${id}`); },
  async duplicate(id: string) { const r = await axiosClient.post<Envelope<SellerProduct>>(`${root}/${id}/duplicate`); return r.data.data; },
  async status(id: string, status: 'ACTIVE' | 'INACTIVE') { const r = await axiosClient.patch<Envelope<SellerProduct>>(`${root}/${id}/status`, { status }); return r.data.data; },
  async exportCsv(filters: ProductFilters) { const r = await axiosClient.get(`${root}/export`, { params: filters, responseType: 'blob' }); return r.data as Blob; },
  async importCsv(file: File) { const body = new FormData(); body.append('file', file); const r = await axiosClient.post<Envelope<{total:number;created:number}>>(`${root}/import`, body, { headers: { 'Content-Type': undefined }, timeout: 60000 }); return r.data.data; },
  async template() { const r = await axiosClient.get(`${root}/import-template`, { responseType: 'blob' }); return r.data as Blob; },
};
