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
  brandId: string;
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
    brandId: params.get('brandId') ?? '',
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
  if (filters.brandId) params.set('brandId', filters.brandId);
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
  const page = Math.max(0, filters.page - 1); // Backend phân trang từ 0, UI từ 1.
  const common: ProductSearchQuery = {
    keyword: filters.q.trim() || undefined,
    categoryId: filters.category || undefined,
    brandId: filters.brandId || undefined,
    minPrice: filters.minPrice,
    maxPrice: filters.maxPrice,
    rating: filters.minRating,
    status: 'ACTIVE',
    page,
    size: PAGE_SIZE,
    sort: SORT_TO_API[filters.sort],
  };

  // Search endpoint yêu cầu keyword; khi có bộ lọc hoặc không có từ khóa thì dùng filter endpoint.
  const useSearchEndpoint = Boolean(filters.q.trim()) &&
    !filters.category && !filters.brandId && filters.minPrice === undefined &&
    filters.maxPrice === undefined && filters.minRating === undefined;

  const response = useSearchEndpoint
    ? await productApi.search({ keyword: filters.q.trim(), page, size: PAGE_SIZE, sort: SORT_TO_API[filters.sort] })
    : await productApi.filter(common);

  let related: ProductCardDto[] = [];
  if (response.totalElements === 0) {
    try {
      const featured = await productApi.list({ featured: true, sortBy: 'newest' });
      const suggestions = featured.length ? featured : await productApi.list({ sortBy: 'newest' });
      related = suggestions.slice(0, 4);
    } catch {
      related = [];
    }
  }

  return {
    items: response.items ?? [],
    total: response.totalElements ?? 0,
    totalPages: Math.max(1, response.totalPages ?? 0),
    related,
  };
}

export function findCategory(id: string, nodes: Category[]): Category | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;
    const found = node.children && findCategory(id, node.children);
    if (found) return found;
  }
  return undefined;
}
