import type { ProductCardDto } from '../catalog/productApi';
import type { WishlistItemDto } from './wishlistTypes';

export type WishlistSort =
  | 'newest'
  | 'price_asc'
  | 'price_desc'
  | 'rating_desc';

export const SORT_OPTIONS: { value: WishlistSort; label: string }[] = [
  { value: 'newest', label: 'Mới thêm gần đây' },
  { value: 'price_asc', label: 'Giá tăng dần' },
  { value: 'price_desc', label: 'Giá giảm dần' },
  { value: 'rating_desc', label: 'Đánh giá cao nhất' },
];

export const PAGE_SIZE_OPTIONS = [12, 24, 48] as const;
export const DEFAULT_PAGE_SIZE = 12;

export interface AdvancedFilters {
  minPrice: number | null;
  maxPrice: number | null;
  inStockOnly: boolean;
  onSaleOnly: boolean;
  /** Chỉ lấy sản phẩm có đánh giá trung bình từ mức này trở lên */
  minRating: number | null;
}

export const EMPTY_FILTERS: AdvancedFilters = {
  minPrice: null,
  maxPrice: null,
  inStockOnly: false,
  onSaleOnly: false,
  minRating: null,
};

/** Khóa danh mục dành cho sản phẩm không có categoryId. */
export const NO_CATEGORY = '__none__';
export const ALL_CATEGORIES = 'all';

export interface CategoryChip {
  id: string;
  name: string;
  count: number;
}

/* ---------- Số liệu của sản phẩm ---------- */

/** BigDecimal từ BE có thể về dạng số hoặc chuỗi. */
export const toNumber = (value: unknown): number | null => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export const priceOf = (product: ProductCardDto): number | null =>
  toNumber(product.minPrice) ?? toNumber(product.price);

export const maxPriceOf = (product: ProductCardDto): number | null =>
  toNumber(product.maxPrice) ?? priceOf(product);

export const comparePriceOf = (product: ProductCardDto): number | null => {
  const price = priceOf(product);
  const compare = toNumber(product.comparePrice);
  return price != null && compare != null && compare > price ? compare : null;
};

/** Chỉ tính khi có đủ giá hợp lệ và giá gốc cao hơn giá bán. */
export const discountPercentOf = (product: ProductCardDto): number | null => {
  const price = priceOf(product);
  const compare = comparePriceOf(product);
  if (price == null || compare == null) return null;
  const percent = Math.round((1 - price / compare) * 100);
  return percent > 0 ? percent : null;
};

export const ratingOf = (product: ProductCardDto): number =>
  toNumber(product.ratingAverage) ?? 0;

export const isOutOfStock = (product: ProductCardDto): boolean =>
  product.stockQuantity != null && product.stockQuantity <= 0;

/* ---------- Tìm kiếm (không phân biệt hoa/thường và dấu tiếng Việt) ---------- */

export const normalizeText = (value: string): string =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();

/** Mọi từ trong từ khóa phải xuất hiện trong tên sản phẩm. */
export const matchesKeyword = (
  product: ProductCardDto,
  normalizedKeyword: string
): boolean => {
  if (!normalizedKeyword) return true;
  const name = normalizeText(product.name ?? '');
  return normalizedKeyword.split(' ').every((word) => name.includes(word));
};

/* ---------- Bộ lọc nâng cao ---------- */

export const countActiveFilters = (filters: AdvancedFilters): number =>
  [
    filters.minPrice != null,
    filters.maxPrice != null,
    filters.inStockOnly,
    filters.onSaleOnly,
    filters.minRating != null,
  ].filter(Boolean).length;

export const matchesFilters = (
  product: ProductCardDto,
  filters: AdvancedFilters
): boolean => {
  const price = priceOf(product);
  const maxPrice = maxPriceOf(product);

  // Khoảng giá: giao nhau với khoảng [giá thấp nhất, giá cao nhất] của sản phẩm
  if (filters.minPrice != null && (maxPrice == null || maxPrice < filters.minPrice)) {
    return false;
  }
  if (filters.maxPrice != null && (price == null || price > filters.maxPrice)) {
    return false;
  }
  if (filters.inStockOnly && isOutOfStock(product)) return false;
  if (filters.onSaleOnly && discountPercentOf(product) == null) return false;
  if (filters.minRating != null && ratingOf(product) < filters.minRating) {
    return false;
  }
  return true;
};

/* ---------- Danh mục ---------- */

export const categoryKeyOf = (product: ProductCardDto): string =>
  product.categoryId || NO_CATEGORY;

/**
 * Chip lấy từ toàn bộ danh sách yêu thích (để chip không biến mất khi đang tìm kiếm);
 * số đếm tính trên các sản phẩm đang khớp từ khóa và bộ lọc.
 */
export const buildCategoryChips = (
  allItems: WishlistItemDto[],
  matchedItems: WishlistItemDto[],
  categoryNames: ReadonlyMap<string, string>
): CategoryChip[] => {
  const totals = new Map<string, number>();
  allItems.forEach((item) => {
    const key = categoryKeyOf(item.product);
    totals.set(key, (totals.get(key) ?? 0) + 1);
  });

  const matched = new Map<string, number>();
  matchedItems.forEach((item) => {
    const key = categoryKeyOf(item.product);
    matched.set(key, (matched.get(key) ?? 0) + 1);
  });

  return [...totals.entries()]
    .map(([id, total]) => ({
      id,
      total,
      name:
        id === NO_CATEGORY
          ? 'Khác'
          : categoryNames.get(id) ?? 'Danh mục khác',
      count: matched.get(id) ?? 0,
    }))
    .sort(
      (a, b) =>
        b.total - a.total || a.name.localeCompare(b.name, 'vi')
    )
    .map(({ id, name, count }) => ({ id, name, count }));
};

/* ---------- Sắp xếp ---------- */

/** Sắp xếp ổn định; "newest" giữ nguyên thứ tự BE trả về (mới thêm trước). */
export const sortItems = (
  items: WishlistItemDto[],
  sort: WishlistSort
): WishlistItemDto[] => {
  if (sort === 'newest') return items;

  const indexed = items.map((item, index) => ({ item, index }));
  const compare = (a: WishlistItemDto, b: WishlistItemDto): number => {
    if (sort === 'rating_desc') {
      return (
        ratingOf(b.product) - ratingOf(a.product) ||
        (b.product.ratingCount ?? 0) - (a.product.ratingCount ?? 0)
      );
    }
    const priceA = priceOf(a.product);
    const priceB = priceOf(b.product);
    // Sản phẩm chưa có giá luôn xuống cuối
    if (priceA == null && priceB == null) return 0;
    if (priceA == null) return 1;
    if (priceB == null) return -1;
    return sort === 'price_asc' ? priceA - priceB : priceB - priceA;
  };

  return indexed
    .sort((a, b) => compare(a.item, b.item) || a.index - b.index)
    .map(({ item }) => item);
};

/* ---------- Phân trang ---------- */

export type PageToken = number | 'ellipsis-start' | 'ellipsis-end';

/** Ví dụ: 1 2 3 4 5 … 8 | 1 … 4 5 6 … 8 | 1 … 4 5 6 7 8 */
export const buildPageList = (current: number, total: number): PageToken[] => {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, 'ellipsis-end', total];
  if (current >= total - 3) {
    return [1, 'ellipsis-start', total - 4, total - 3, total - 2, total - 1, total];
  }
  return [
    1,
    'ellipsis-start',
    current - 1,
    current,
    current + 1,
    'ellipsis-end',
    total,
  ];
};
