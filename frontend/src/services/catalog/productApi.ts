import axiosClient from '../api/axiosClient';

export interface ApiEnvelope<T> { success: boolean; message: string; data: T; }
export interface ProductCardDto {
  id: string;
  shopId: string;
  categoryId?: string | null;
  brandId?: string | null;
  name: string;
  slug: string;
  description?: string | null;
  thumbnailUrl?: string | null;
  minPrice?: number | string | null;
  maxPrice?: number | string | null;
  price?: number | string | null;
  comparePrice?: number | string | null;
  tags?: (string | { id?: string; name?: string; label?: string })[] | null;
  stockQuantity?: number | null;
  featured?: boolean;
  ratingAverage?: number | string | null;
  ratingCount?: number | null;
}
export interface ProductDetailDto extends ProductCardDto {
  status?: string;
  shop?: { id: string; name: string; slug?: string; logoUrl?: string; bannerUrl?: string; ratingAverage?: number | string; ratingCount?: number; followersCount?: number } | null;
  category?: { id: string; name: string; slug?: string; imageUrl?: string } | null;
  media?: { id: string; type?: string; url: string; sortOrder?: number }[];
  options?: { id: string; name: string; sortOrder?: number; values: { id: string; value: string; sortOrder?: number }[] }[];
  variants?: { id: string; sku?: string; status?: string; price: number | string; comparePrice?: number | string | null; imageUrl?: string | null; stockQuantity?: number; selectedOptions?: { optionId: string; optionValueId: string; optionName: string; value: string }[] }[];
}
export interface CategoryDto { id: string; name: string; slug: string; imageUrl?: string | null; parentId?: string | null; status?: string; sortOrder?: number; }
export interface ProductQuery { q?: string; shopId?: string; categoryId?: string; minPrice?: number; maxPrice?: number; featured?: boolean; sortBy?: 'newest'|'price_asc'|'price_desc'|'rating_desc'|'name_asc'; }
export interface ProductPageDto<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
export interface ProductSearchQuery {
  keyword?: string;
  categoryId?: string;
  brandId?: string;
  minPrice?: number;
  maxPrice?: number;
  status?: string;
  rating?: number;
  page?: number;
  size?: number;
  sort?: 'newest' | 'price_asc' | 'price_desc' | 'name_asc';
}

const dataOf = <T,>(response: { data: ApiEnvelope<T> | T }): T => {
  const body = response.data as ApiEnvelope<T>;
  return body && typeof body === 'object' && 'data' in body ? body.data : response.data as T;
};

export const productApi = {
  async list(params: ProductQuery = {}) {
    const r = await axiosClient.get('/products', { params });
    return dataOf<ProductCardDto[]>(r);
  },
  async search(params: Required<Pick<ProductSearchQuery, 'keyword'>> & ProductSearchQuery) {
    const r = await axiosClient.get('/products/search', { params });
    return dataOf<ProductPageDto<ProductCardDto>>(r);
  },
  async filter(params: ProductSearchQuery = {}) {
    const r = await axiosClient.get('/products/filter', { params });
    return dataOf<ProductPageDto<ProductCardDto>>(r);
  },
  async detail(slug: string) {
    const r = await axiosClient.get(`/products/slug/${encodeURIComponent(slug)}`);
    return dataOf<ProductDetailDto>(r);
  },
  async related(slug: string, limit = 8) {
    const r = await axiosClient.get(`/products/slug/${encodeURIComponent(slug)}/related`, { params: { limit } });
    return dataOf<ProductCardDto[]>(r);
  },
};

export const categoryApi = {
  async list() {
    const r = await axiosClient.get('/categories');
    return dataOf<CategoryDto[]>(r);
  },
};

export const formatVnd = (value?: number | string | null) => value == null ? 'Liên hệ' : `${new Intl.NumberFormat('vi-VN').format(Number(value))}₫`;
export const productImage = (p: Pick<ProductCardDto, 'thumbnailUrl'>) => p.thumbnailUrl || 'https://placehold.co/600x600/F2F5FC/6575B8?text=Kala';
