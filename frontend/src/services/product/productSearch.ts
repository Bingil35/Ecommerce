import {
  categoryApi,
  productApi,
  type CategoryDto,
  type ProductCardDto,
  type ProductQuery,
} from '../catalog/productApi';
import type { Category, Product } from './productTypes';

export const PAGE_SIZE = 9;

export const SORT_KEYS = [
  'relevance',
  'price-asc',
  'price-desc',
  'top-rated',
] as const;

export type SortKey = (typeof SORT_KEYS)[number];

export interface SearchFilters {
  q: string;
  category: string;
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
   URL <-> filters
===================================================== */

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
   DANH MỤC
   BE trả danh sách phẳng (có parentId) -> dựng thành cây.
===================================================== */

let categoryCache: Promise<CategoryDto[]> | null = null;

const loadCategories = (): Promise<CategoryDto[]> => {
  if (!categoryCache) {
    categoryCache = categoryApi.list().catch((error) => {
      categoryCache = null; // lần sau thử lại
      throw error;
    });
  }
  return categoryCache;
};

const buildCategoryTree = (flat: CategoryDto[]): Category[] => {
  const sorted = [...flat].sort(
    (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0),
  );
  const nodes = new Map<string, Category>(
    sorted.map((c) => [c.id, { id: c.id, name: c.name, parentId: c.parentId }]),
  );
  const roots: Category[] = [];

  for (const category of sorted) {
    const node = nodes.get(category.id)!;
    const parent = category.parentId ? nodes.get(category.parentId) : undefined;

    if (parent) {
      (parent.children ??= []).push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
};

export async function getCategories(): Promise<Category[]> {
  return buildCategoryTree(await loadCategories());
}

/** Chọn danh mục cha thì tính cả sản phẩm của các danh mục con. */
const collectCategoryIds = (flat: CategoryDto[], rootId: string): Set<string> => {
  const ids = new Set<string>([rootId]);
  let added = true;

  while (added) {
    added = false;
    for (const category of flat) {
      if (category.parentId && ids.has(category.parentId) && !ids.has(category.id)) {
        ids.add(category.id);
        added = true;
      }
    }
  }

  return ids;
};

/* =====================================================
   TÌM KIẾM
   BE chỉ so khớp từ khóa theo chuỗi liền, có phân biệt dấu
   ("ao thun" không ra "Áo thun"), và không có lọc theo
   đánh giá / còn hàng, không phân trang. Phần này xử lý ở FE
   trên danh sách sản phẩm đang bán mà BE trả về.
===================================================== */

const normalize = (value?: string | null): string =>
  (value ?? '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd');

const tokenize = (q: string): string[] =>
  normalize(q).split(/\s+/).filter(Boolean);

const splitWords = (text: string): string[] =>
  text.split(/[^a-z0-9]+/).filter(Boolean);

// Từ ngắn (< 4 ký tự) phải khớp ĐẦU một từ, để "ao" không khớp "thao".
// Từ dài thì cho khớp ở bất kỳ vị trí nào.
const matchesTokens = (text: string, tokens: string[]): boolean => {
  const words = splitWords(text);

  return tokens.every((token) =>
    token.length >= 4
      ? text.includes(token)
      : words.some((word) => word.startsWith(token)),
  );
};

const SORT_TO_API: Record<SortKey, ProductQuery['sortBy']> = {
  relevance: 'newest',
  'price-asc': 'price_asc',
  'price-desc': 'price_desc',
  'top-rated': 'rating_desc',
};

const toProduct = (dto: ProductCardDto): Product => {
  const price = dto.minPrice ?? dto.price;

  return {
    id: dto.id,
    slug: dto.slug,
    name: dto.name,
    categoryId: dto.categoryId,
    price: price == null ? null : Number(price),
    rating: Number(dto.ratingAverage ?? 0),
    reviewCount: dto.ratingCount ?? 0,
    inStock: (dto.stockQuantity ?? 0) > 0,
    imageUrl: dto.thumbnailUrl ?? undefined,
  };
};

// Tên khớp đủ các từ thì xếp trước kết quả chỉ khớp ở mô tả/slug.
const relevanceScore = (dto: ProductCardDto, tokens: string[]): number => {
  return matchesTokens(normalize(dto.name), tokens) ? 1 : 0;
};

export async function searchProducts(
  filters: SearchFilters,
): Promise<SearchResult> {
  const [list, categories] = await Promise.all([
    productApi.list({
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      sortBy: SORT_TO_API[filters.sort],
    }),
    filters.category ? loadCategories() : Promise.resolve([]),
  ]);

  const tokens = tokenize(filters.q);
  const categoryIds = filters.category
    ? collectCategoryIds(categories, filters.category)
    : null;

  let matched = list.filter((dto) => {
    if (categoryIds && (!dto.categoryId || !categoryIds.has(dto.categoryId))) {
      return false;
    }

    if (
      filters.minRating !== undefined &&
      Number(dto.ratingAverage ?? 0) < filters.minRating
    ) {
      return false;
    }

    if (filters.inStock && (dto.stockQuantity ?? 0) <= 0) return false;

    if (tokens.length) {
      const haystack = [
        normalize(dto.name),
        normalize(dto.slug).replace(/-/g, ' '),
        normalize(dto.description),
      ].join(' ');

      if (!matchesTokens(haystack, tokens)) return false;
    }

    return true;
  });

  if (tokens.length && filters.sort === 'relevance') {
    // Array.sort ổn định -> các sản phẩm cùng điểm giữ thứ tự mới nhất
    matched = [...matched].sort(
      (a, b) => relevanceScore(b, tokens) - relevanceScore(a, tokens),
    );
  }

  const total = matched.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const page = Math.min(filters.page, totalPages);
  const items = matched
    .slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
    .map(toProduct);

  // Không có kết quả -> gợi ý sản phẩm nổi bật.
  let related: Product[] = [];

  if (total === 0) {
    try {
      const featured = await productApi.list({ featured: true });
      const suggestions = featured.length
        ? featured
        : await productApi.list({});
      related = suggestions.slice(0, 4).map(toProduct);
    } catch {
      related = [];
    }
  }

  return { items, total, totalPages, related };
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
