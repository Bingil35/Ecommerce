import {
  categoryApi,
  productApi,
  type CategoryDto,
  type ProductCardDto,
  type ProductSearchQuery,
} from '../catalog/productApi';
import type { Category } from './productTypes';

export const PAGE_SIZE = 12;

export const SORT_KEYS = ['relevance', 'price-asc', 'price-desc', 'name-asc'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

export interface SearchFilters {
  q: string;
  category: string;
  brandIds: string[];
  shopAddresses: string[];
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  sort: SortKey;
  page: number;
}

export interface SearchResult {
  items: ProductCardDto[];
  total: number;
  totalPages: number;
  related: ProductCardDto[];
}

const toNumber = (value: string | null): number | undefined => {
  if (value === null || value === '') return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
};

export function parseFilters(params: URLSearchParams): SearchFilters {
  const sort = params.get('sort') as SortKey | null;
  return {
    q: params.get('q')?.trim() ?? '',
    category: params.get('category') ?? '',
    brandIds: params.getAll('brandId').filter(Boolean),
    shopAddresses: params.getAll('shopAddress').map((value) => value.trim()).filter(Boolean),
    minPrice: toNumber(params.get('minPrice')),
    maxPrice: toNumber(params.get('maxPrice')),
    minRating: toNumber(params.get('rating')),
    sort: sort && SORT_KEYS.includes(sort) ? sort : 'relevance',
    page: Math.max(1, Math.floor(toNumber(params.get('page')) ?? 1)),
  };
}

export function buildParams(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('q', filters.q);
  if (filters.category) params.set('category', filters.category);
  filters.brandIds.forEach((id) => params.append('brandId', id));
  filters.shopAddresses.forEach((address) => params.append('shopAddress', address));
  if (filters.minPrice !== undefined) params.set('minPrice', String(filters.minPrice));
  if (filters.maxPrice !== undefined) params.set('maxPrice', String(filters.maxPrice));
  if (filters.minRating !== undefined) params.set('rating', String(filters.minRating));
  if (filters.sort !== 'relevance') params.set('sort', filters.sort);
  if (filters.page > 1) params.set('page', String(filters.page));
  return params;
}

let categoryCache: Promise<CategoryDto[]> | null = null;
const loadCategories = (): Promise<CategoryDto[]> => {
  if (!categoryCache) {
    categoryCache = categoryApi.list().catch((error) => {
      categoryCache = null;
      throw error;
    });
  }
  return categoryCache;
};

const buildCategoryTree = (flat: CategoryDto[]): Category[] => {
  const sorted = [...flat].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  const nodes = new Map<string, Category>(sorted.map((c) => [c.id, { id: c.id, name: c.name, parentId: c.parentId }]));
  const roots: Category[] = [];
  for (const category of sorted) {
    const node = nodes.get(category.id)!;
    const parent = category.parentId ? nodes.get(category.parentId) : undefined;
    if (parent) (parent.children ??= []).push(node);
    else roots.push(node);
  }
  return roots;
};

export async function getCategories(): Promise<Category[]> {
  return buildCategoryTree(await loadCategories());
}

const SORT_TO_API: Record<SortKey, NonNullable<ProductSearchQuery['sort']>> = {
  relevance: 'newest',
  'price-asc': 'price_asc',
  'price-desc': 'price_desc',
  'name-asc': 'name_asc',
};

export async function searchProducts(filters: SearchFilters): Promise<SearchResult> {
  const common: ProductSearchQuery = {
    keyword: filters.q.trim() || undefined,
    categoryId: filters.category || undefined,
    minPrice: filters.minPrice,
    maxPrice: filters.maxPrice,
    rating: filters.minRating,
    status: 'ACTIVE',
    page: 0,
    size: PAGE_SIZE,
    sort: SORT_TO_API[filters.sort],
  };

  // Khi không dùng bộ lọc đa lựa chọn, giữ luồng tìm kiếm/phân trang của backend.
  const hasMultiFilters = filters.brandIds.length > 0 || filters.shopAddresses.length > 0;
  if (!hasMultiFilters) {
    const useSearchEndpoint = Boolean(filters.q.trim()) && !filters.category &&
      filters.minPrice === undefined && filters.maxPrice === undefined && filters.minRating === undefined;
    const response = useSearchEndpoint
      ? await productApi.search({ keyword: filters.q.trim(), page: Math.max(0, filters.page - 1), size: PAGE_SIZE, sort: SORT_TO_API[filters.sort] })
      : await productApi.filter({ ...common, page: Math.max(0, filters.page - 1) });

    let related: ProductCardDto[] = [];
    if (response.totalElements === 0) {
      try {
        const featured = await productApi.list({ featured: true, sortBy: 'newest' });
        const suggestions = featured.length ? featured : await productApi.list({ sortBy: 'newest' });
        related = suggestions.slice(0, 4);
      } catch { related = []; }
    }
    return { items: response.items ?? [], total: response.totalElements ?? 0, totalPages: Math.max(1, response.totalPages ?? 0), related };
  }

  // Semantics: OR giữa các thương hiệu, OR giữa các địa chỉ, AND giữa hai nhóm.
  // Mỗi cặp thương hiệu/địa chỉ được truy vấn riêng rồi hợp nhất và loại trùng theo ID.
  const brandValues: (string | undefined)[] = filters.brandIds.length ? filters.brandIds : [undefined];
  const addressValues: (string | undefined)[] = filters.shopAddresses.length ? filters.shopAddresses : [undefined];
  const merged = new Map<string, ProductCardDto>();
  let related: ProductCardDto[] = [];
  for (const brandId of brandValues) {
    for (const shopAddress of addressValues) {
      let page = 0;
      let totalPages = 1;
      do {
        const response = await productApi.filter({ ...common, brandId, shopAddress, page, size: PAGE_SIZE });
        for (const product of response.items ?? []) merged.set(product.id, product);
        totalPages = Math.max(1, response.totalPages ?? 1);
        page += 1;
      } while (page < totalPages);
    }
  }

  let allItems = Array.from(merged.values());
  if (filters.sort === 'price-asc') allItems.sort((a, b) => Number(a.price ?? a.minPrice ?? 0) - Number(b.price ?? b.minPrice ?? 0));
  else if (filters.sort === 'price-desc') allItems.sort((a, b) => Number(b.price ?? b.minPrice ?? 0) - Number(a.price ?? a.minPrice ?? 0));
  else if (filters.sort === 'name-asc') allItems.sort((a, b) => a.name.localeCompare(b.name, 'vi'));

  if (allItems.length === 0) {
    try {
      const featured = await productApi.list({ featured: true, sortBy: 'newest' });
      const suggestions = featured.length ? featured : await productApi.list({ sortBy: 'newest' });
      related = suggestions.slice(0, 4);
    } catch { related = []; }
  }
  const total = allItems.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const start = (Math.max(1, filters.page) - 1) * PAGE_SIZE;
  return { items: allItems.slice(start, start + PAGE_SIZE), total, totalPages, related };
}

export function findCategory(id: string, nodes: Category[]): Category | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;
    const found = node.children && findCategory(id, node.children);
    if (found) return found;
  }
  return undefined;
}
