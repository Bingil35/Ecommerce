import axiosClient from '../api/axiosClient';
import type { Category, Product } from './productTypes';

export const PAGE_SIZE = 9;

export const SORT_KEYS = [
  'relevance',
  'price-asc',
  'price-desc',
  'best-selling',
  'top-rated',
] as const;

export type SortKey = (typeof SORT_KEYS)[number];

export interface SearchFilters {
  q: string;
  category: string;
  brands: string[];
  tags: string[];
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  inStock: boolean;
  sort: SortKey;
  page: number;
}

export interface SearchResult {
  items: Product[];
  total: number;
  totalPages: number;
  related: Product[]; // gợi ý khi không có kết quả
}

/* =====================================================
   URL <-> filters (giữ nguyên, dùng cho thanh địa chỉ)
===================================================== */

const toNumber = (value: string | null): number | undefined => {
  if (value === null || value === '') return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
};

const toList = (value: string | null): string[] =>
  value ? value.split(',').filter(Boolean) : [];

export function parseFilters(params: URLSearchParams): SearchFilters {
  const sort = params.get('sort') as SortKey | null;

  return {
    q: params.get('q')?.trim() ?? '',
    category: params.get('category') ?? '',
    brands: toList(params.get('brand')),
    tags: toList(params.get('tag')),
    minPrice: toNumber(params.get('minPrice')),
    maxPrice: toNumber(params.get('maxPrice')),
    minRating: toNumber(params.get('rating')),
    inStock: params.get('inStock') === '1',
    sort: sort && SORT_KEYS.includes(sort) ? sort : 'relevance',
    page: Math.max(1, Math.floor(toNumber(params.get('page')) ?? 1)),
  };
}

export function buildParams(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams();

  if (filters.q) params.set('q', filters.q);
  if (filters.category) params.set('category', filters.category);
  if (filters.brands.length) params.set('brand', filters.brands.join(','));
  if (filters.tags.length) params.set('tag', filters.tags.join(','));
  if (filters.minPrice !== undefined)
    params.set('minPrice', String(filters.minPrice));
  if (filters.maxPrice !== undefined)
    params.set('maxPrice', String(filters.maxPrice));
  if (filters.minRating !== undefined)
    params.set('rating', String(filters.minRating));
  if (filters.inStock) params.set('inStock', '1');
  if (filters.sort !== 'relevance') params.set('sort', filters.sort);
  if (filters.page > 1) params.set('page', String(filters.page));

  return params;
}

/* =====================================================
   GỌI BACKEND
   Nếu backend đặt tên khác, chỉ cần sửa 3 chỗ dưới đây.
===================================================== */

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// Dạng dữ liệu phân trang backend trả về (GIẢ ĐỊNH - sửa cho khớp).
interface ApiPage {
  items: Product[];
  total: number;
  totalPages: number;
}

// (1) Tên tham số gửi lên backend.
function toApiParams(filters: SearchFilters, size = PAGE_SIZE) {
  return {
    keyword: filters.q || undefined,
    category: filters.category || undefined,
    brand: filters.brands.length ? filters.brands.join(',') : undefined,
    tag: filters.tags.length ? filters.tags.join(',') : undefined,
    minPrice: filters.minPrice,
    maxPrice: filters.maxPrice,
    minRating: filters.minRating,
    inStock: filters.inStock || undefined,
    sort: filters.sort === 'relevance' ? undefined : filters.sort,
    page: filters.page - 1, // đổi thành filters.page nếu backend đánh số từ 1
    size,
  };
}

// (2) Đổi tên field từ JSON của backend sang Product.
function mapProduct(raw: Product): Product {
  return raw; // ví dụ: { ...raw, price: raw.minPrice, imageUrl: raw.thumbnail }
}

// (3) Đọc phần phân trang từ response.
async function fetchPage(filters: SearchFilters, size = PAGE_SIZE) {
  const res = await axiosClient.get<ApiResponse<ApiPage>>('/products', {
    params: toApiParams(filters, size),
  });

  const page = res.data.data;

  return {
    items: page.items.map(mapProduct),
    total: page.total,
    totalPages: Math.max(1, page.totalPages),
  };
}

export async function searchProducts(
  filters: SearchFilters,
): Promise<SearchResult> {
  const page = await fetchPage(filters);

  // Không có kết quả -> lấy sản phẩm bán chạy để gợi ý.
  let related: Product[] = [];

  if (page.total === 0) {
    const suggestion = await fetchPage(
      {
        q: '',
        category: '',
        brands: [],
        tags: [],
        inStock: false,
        sort: 'best-selling',
        page: 1,
      },
      4,
    );
    related = suggestion.items;
  }

  return { ...page, related };
}

/* =====================================================
   DỮ LIỆU CHO BỘ LỌC (danh mục, thương hiệu, thẻ)
===================================================== */

export async function getCategories(): Promise<Category[]> {
  const res = await axiosClient.get<ApiResponse<Category[]>>('/categories');
  return res.data.data;
}

export async function getBrands(): Promise<string[]> {
  const res = await axiosClient.get<ApiResponse<string[]>>('/brands');
  return res.data.data;
}

export async function getTags(): Promise<string[]> {
  const res = await axiosClient.get<ApiResponse<string[]>>('/tags');
  return res.data.data;
}

/* =====================================================
   TIỆN ÍCH
===================================================== */

export function findCategory(
  id: string,
  nodes: Category[],
): Category | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;

    const found = node.children && findCategory(id, node.children);
    if (found) return found;
  }

  return undefined;
}