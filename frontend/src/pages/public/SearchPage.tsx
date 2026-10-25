import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, ChevronDown, ChevronRight } from 'lucide-react';

import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import { brandApi, shopApi, formatVnd, type BrandDto, type ShopAddressDto } from '../../services/catalog/productApi';
import type { Category } from '../../services/product/productTypes';
import {
  buildParams,
  findCategory,
  getCategories,
  parseFilters,
  searchProducts,
  type SearchFilters,
  type SearchResult,
  type SortKey,
} from '../../services/product/productSearch';

const SORT_OPTIONS: { id: SortKey; label: string }[] = [
  { id: 'relevance', label: 'Liên quan nhất' },
  { id: 'price-asc', label: 'Giá thấp đến cao' },
  { id: 'price-desc', label: 'Giá cao đến thấp' },
  { id: 'name-asc', label: 'Tên sản phẩm A–Z' },
];

const RATING_OPTIONS = [4, 3, 2];

const cardClass =
  'rounded-2xl bg-white border border-[#E5EBF6]';

export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(params), [params]);

  const [minInput, setMinInput] = useState(String(filters.minPrice ?? ''));
  const [maxInput, setMaxInput] = useState(String(filters.maxPrice ?? ''));
  const [expandedBrands, setExpandedBrands] = useState(false);
  const [expandedAddresses, setExpandedAddresses] = useState(false);
  const [shops, setShops] = useState<ShopAddressDto[]>([]);
  const [expandedCategoryIds, setExpandedCategoryIds] = useState<Set<string>>(() => new Set());

  const [result, setResult] = useState<SearchResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<BrandDto[]>([]);

  // Đồng bộ các ô giá khi URL đổi (Back, xóa bộ lọc...). Từ khóa được quản lý tại Header.
  useEffect(() => {
    setMinInput(String(filters.minPrice ?? ''));
    setMaxInput(String(filters.maxPrice ?? ''));
  }, [filters.minPrice, filters.maxPrice]);

  // Tải danh sách thương hiệu từ API Brands.
  useEffect(() => {
    brandApi.list()
      .then((items) => setBrands(items.slice().sort((a, b) => a.name.localeCompare(b.name, 'vi'))))
      .catch(() => setBrands([]));
  }, []);

  useEffect(() => {
    shopApi.list()
      .then((items) => setShops(items.filter((shop) => shop.address?.trim()).sort((a, b) => (a.address ?? '').localeCompare(b.address ?? '', 'vi'))))
      .catch(() => setShops([]));
  }, []);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => {
        // Không tải được danh mục thì trang vẫn tìm kiếm được.
      });
  }, []);

  // Gọi backend mỗi khi bộ lọc đổi.
  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setErrorMessage('');

    searchProducts(filters)
      .then((data) => {
        if (!cancelled) setResult(data);
      })
      .catch((error) => {
        if (cancelled) return;
        setResult(null);
        setErrorMessage(
          error.response?.data?.message ||
            'Không tải được danh sách sản phẩm. Vui lòng thử lại.',
        );
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [filters]);

  // Đổi bộ lọc thì quay về trang 1, trừ khi đang đổi trang.
  const update = (patch: Partial<SearchFilters>) => {
    setParams(buildParams({ ...filters, page: 1, ...patch }));
  };

  const applyPrice = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    let min = minInput === '' ? undefined : Math.max(0, Number(minInput));
    let max = maxInput === '' ? undefined : Math.max(0, Number(maxInput));

    if (min !== undefined && max !== undefined && min > max) {
      [min, max] = [max, min];
    }

    update({ minPrice: min, maxPrice: max });
  };

  const clearAll = () => setParams(new URLSearchParams());

  /* ---------- Chip các bộ lọc đang áp dụng ---------- */

  const activeChips: { key: string; label: string; remove: () => void }[] = [];

  if (filters.q) {
    activeChips.push({
      key: 'q',
      label: `Từ khóa: ${filters.q}`,
      remove: () => update({ q: '' }),
    });
  }

  if (filters.brandIds.length) {
    filters.brandIds.forEach((id) => activeChips.push({
      key: `brandId-${id}`,
      label: `Thương hiệu: ${brands.find((brand) => brand.id === id)?.name ?? id}`,
      remove: () => update({ brandIds: filters.brandIds.filter((selected) => selected !== id) }),
    }));
  }

  if (filters.category) {
    activeChips.push({
      key: 'category',
      label: findCategory(filters.category, categories)?.name ?? filters.category,
      remove: () => update({ category: '' }),
    });
  }

  if (filters.shopAddresses.length) {
    filters.shopAddresses.forEach((address) => activeChips.push({
      key: `shopAddress-${address}`,
      label: `Địa chỉ shop: ${address}`,
      remove: () => update({ shopAddresses: filters.shopAddresses.filter((selected) => selected !== address) }),
    }));
  }

  if (filters.minPrice !== undefined || filters.maxPrice !== undefined) {
    const from = filters.minPrice !== undefined ? formatVnd(filters.minPrice) : '0đ';
    const to = filters.maxPrice !== undefined ? formatVnd(filters.maxPrice) : 'trở lên';

    activeChips.push({
      key: 'price',
      label: `Giá: ${from} - ${to}`,
      remove: () => update({ minPrice: undefined, maxPrice: undefined }),
    });
  }

  if (filters.minRating !== undefined) {
    activeChips.push({
      key: 'rating',
      label: `Từ ${filters.minRating} sao`,
      remove: () => update({ minRating: undefined }),
    });
  }


  const inputClass =
    'h-11 w-full rounded-xl border border-[#D7E0F2] bg-white px-3 text-sm text-[#172033] outline-none transition placeholder:text-[#9AA8C6] focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10';

  return (
    <div className="min-h-screen bg-[#F7F9FC] px-4 py-6 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-6xl">
        <div className="mb-5">
          <h1 className="text-2xl font-bold tracking-tight text-[#29266F]">{filters.q ? `Kết quả tìm kiếm: “${filters.q}”` : 'Khám phá sản phẩm'}</h1>
          <p className="mt-1 text-sm text-[#667085]">Sử dụng thanh tìm kiếm trên Header để tìm sản phẩm.</p>
        </div>

        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* ================= FILTERS ================= */}

          <aside className={`${cardClass} h-fit space-y-6 p-6`}>
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-[#29266F]">Bộ lọc</h2>

              {activeChips.length > 0 && (
                <button
                  type="button"
                  onClick={clearAll}
                  className="text-sm font-medium text-[#3975E8] hover:underline"
                >
                  Xóa tất cả
                </button>
              )}
            </div>

            {/* Danh mục (cây cha - con) */}
            {categories.length > 0 && (
              <fieldset>
                <legend className="text-sm font-medium text-[#52638F]">
                  Danh mục
                </legend>

                <ul className="mt-3 space-y-1">
                  {categories.map((node) => (
                    <CategoryItem
                      key={node.id}
                      node={node}
                      depth={0}
                      activeId={filters.category}
                      expandedIds={expandedCategoryIds}
                      onToggle={(id) => setExpandedCategoryIds((current) => { const next = new Set(current); next.has(id) ? next.delete(id) : next.add(id); return next; })}
                      onSelect={(id) => update({ category: id === filters.category ? '' : id })}
                    />
                  ))}
                </ul>
              </fieldset>
            )}

            {/* Thương hiệu: checkbox nhiều lựa chọn, có thu gọn/xem thêm. */}
            <fieldset className="border-t border-[#E1E7F2] pt-6">
              <legend className="text-sm font-medium text-[#52638F]">Thương hiệu</legend>
              <div className="mt-3 space-y-3">
                {(expandedBrands ? brands : brands.slice(0, 5)).map((brand) => (
                  <label key={brand.id} className="flex cursor-pointer items-center gap-2.5 text-sm text-[#667085]">
                    <input type="checkbox" checked={filters.brandIds.includes(brand.id)}
                      onChange={(event) => update({ brandIds: event.target.checked ? [...filters.brandIds, brand.id] : filters.brandIds.filter((id) => id !== brand.id) })}
                      className="h-4 w-4 rounded accent-[#4F7CFF]" />
                    <span>{brand.name}</span>
                  </label>
                ))}
                {brands.length > 5 && <button type="button" onClick={() => setExpandedBrands((value) => !value)} className="text-sm font-medium text-[#3975E8] hover:underline">{expandedBrands ? 'Thu gọn' : `Xem thêm (${brands.length - 5})`}</button>}
                {brands.length === 0 && <p className="text-sm text-[#98A2B3]">Chưa có dữ liệu thương hiệu.</p>}
              </div>
            </fieldset>

            {/* Địa chỉ shop: các giá trị lấy từ danh sách shop. */}
            <fieldset className="border-t border-[#E1E7F2] pt-6">
              <legend className="text-sm font-medium text-[#52638F]">Địa chỉ cửa hàng</legend>
              <div className="mt-3 space-y-3">
                {(expandedAddresses ? shops : shops.slice(0, 5)).map((shop) => {
                  const address = shop.address!.trim();
                  return <label key={shop.id} className="flex cursor-pointer items-start gap-2.5 text-sm text-[#667085]">
                    <input type="checkbox" checked={filters.shopAddresses.includes(address)}
                      onChange={(event) => update({ shopAddresses: event.target.checked ? [...filters.shopAddresses, address] : filters.shopAddresses.filter((value) => value !== address) })}
                      className="mt-0.5 h-4 w-4 rounded accent-[#4F7CFF]" />
                    <span>{address}</span>
                  </label>;
                })}
                {shops.length > 5 && <button type="button" onClick={() => setExpandedAddresses((value) => !value)} className="text-sm font-medium text-[#3975E8] hover:underline">{expandedAddresses ? 'Thu gọn' : `Xem thêm (${shops.length - 5})`}</button>}
                {shops.length === 0 && <p className="text-sm text-[#98A2B3]">Chưa tải được danh sách địa chỉ shop.</p>}
              </div>
            </fieldset>

            {/* Khoảng giá */}
            <form onSubmit={applyPrice} className="border-t border-[#E1E7F2] pt-6">
              <p className="text-sm font-medium text-[#52638F]">Khoảng giá (đ)</p>

              <div className="mt-3 flex items-center gap-2">
                <input
                  type="number"
                  min={0}
                  inputMode="numeric"
                  value={minInput}
                  onChange={(event) => setMinInput(event.target.value)}
                  placeholder="Từ"
                  aria-label="Giá thấp nhất"
                  className={inputClass}
                />

                <span className="text-[#9AA8C6]">-</span>

                <input
                  type="number"
                  min={0}
                  inputMode="numeric"
                  value={maxInput}
                  onChange={(event) => setMaxInput(event.target.value)}
                  placeholder="Đến"
                  aria-label="Giá cao nhất"
                  className={inputClass}
                />
              </div>

              <button
                type="submit"
                className="mt-3 h-11 w-full rounded-full border border-[#4F7CFF] text-sm font-semibold text-[#4F7CFF] transition hover:bg-[#EEF4FF]"
              >
                Áp dụng
              </button>
            </form>

            {/* Đánh giá */}
            <fieldset className="border-t border-[#E1E7F2] pt-6">
              <legend className="text-sm font-medium text-[#52638F]">
                Đánh giá
              </legend>

              <div className="mt-3 space-y-3">
                {RATING_OPTIONS.map((stars) => (
                  <label
                    key={stars}
                    className="flex cursor-pointer items-center gap-2.5 text-sm text-[#667085]"
                  >
                    <input
                      type="radio"
                      name="rating"
                      checked={filters.minRating === stars}
                      onChange={() => update({ minRating: stars })}
                      className="h-5 w-5 cursor-pointer accent-[#4F7CFF]"
                    />
                    Từ {stars} sao trở lên
                  </label>
                ))}
              </div>
            </fieldset>

          </aside>

          {/* ================= RESULTS ================= */}

          <section aria-live="polite" className="min-w-0">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-[15px] text-[#667085]">
                {isLoading
                  ? 'Đang tìm kiếm...'
                  : result
                    ? `${result.total} sản phẩm`
                    : ''}
              </p>

              <div className="flex items-center gap-3">
                <label htmlFor="sort" className="text-sm text-[#667085]">
                  Sắp xếp
                </label>

                <select
                  id="sort"
                  value={filters.sort}
                  onChange={(event) =>
                    update({ sort: event.target.value as SortKey })
                  }
                  className="h-11 rounded-full border border-[#D7E0F2] bg-white px-4 text-sm text-[#172033] outline-none transition focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10"
                >
                  {SORT_OPTIONS.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {activeChips.length > 0 && (
              <ul className="mt-4 flex flex-wrap gap-2">
                {activeChips.map((chip) => (
                  <li key={chip.key}>
                    <button
                      type="button"
                      onClick={chip.remove}
                      aria-label={`Bỏ lọc ${chip.label}`}
                      className="flex items-center gap-2 rounded-full bg-[#EEF4FF] px-3.5 py-1.5 text-sm text-[#29266F] transition hover:bg-[#DFE9FF]"
                    >
                      {chip.label}
                      <span aria-hidden="true" className="text-[#52638F]">
                        ×
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {errorMessage && (
              <div className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-600">
                {errorMessage}
              </div>
            )}

            {result && result.items.length > 0 && (
              <>
                <div
                  className={`mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4 ${
                    isLoading ? 'opacity-60' : ''
                  }`}
                >
                  {result.items.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                {result.totalPages > 1 && (
                  <nav
                    aria-label="Phân trang"
                    className="mt-8 flex flex-wrap justify-center gap-2"
                  >
                    {Array.from(
                      { length: result.totalPages },
                      (_, index) => index + 1,
                    ).map((page) => (
                      <button
                        key={page}
                        type="button"
                        aria-current={page === filters.page ? 'page' : undefined}
                        onClick={() => update({ page })}
                        className={`h-10 min-w-10 rounded-full border px-3 text-sm transition ${
                          page === filters.page
                            ? 'border-[#4F7CFF] bg-[#4F7CFF] font-semibold text-white'
                            : 'border-[#D7E0F2] bg-white text-[#52638F] hover:bg-[#F8FAFE]'
                        }`}
                      >
                        {page}
                      </button>
                    ))}
                  </nav>
                )}
              </>
            )}

            {result && result.total === 0 && !isLoading && (
              <>
                <div className={`${cardClass} mt-5 px-6 py-14 text-center`}>
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#EEF4FF] text-[#4F7CFF]">
                    <Search className="h-8 w-8" />
                  </div>

                  <h3 className="mt-5 text-lg font-semibold text-[#29266F]">
                    Không tìm thấy sản phẩm phù hợp
                  </h3>

                  <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-[#667085]">
                    Hãy thử từ khóa ngắn hơn hoặc bỏ bớt bộ lọc.
                  </p>

                  <button
                    type="button"
                    onClick={clearAll}
                    className="mt-6 h-[48px] rounded-full bg-[#4F7CFF] px-8 text-sm font-semibold text-white transition hover:bg-[#3D63D8]"
                  >
                    Xóa tất cả bộ lọc
                  </button>
                </div>

                {result.related.length > 0 && (
                  <div className="mt-8">
                    <h3 className="text-base font-semibold text-[#29266F]">
                      Có thể bạn quan tâm
                    </h3>

                    <div className="mt-4 grid grid-cols-2 gap-4 md:grid-cols-4">
                      {result.related.map((product) => (
                        <ProductCard key={product.id} product={product} />
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}

/* =====================================================
   CATEGORY TREE ITEM
===================================================== */

interface CategoryItemProps {
  node: Category;
  depth: number;
  activeId: string;
  expandedIds: Set<string>;
  onToggle: (id: string) => void;
  onSelect: (id: string) => void;
}

function CategoryItem({ node, depth, activeId, expandedIds, onToggle, onSelect }: CategoryItemProps) {
  const active = activeId === node.id;
  const hasChildren = Boolean(node.children?.length);
  const expanded = expandedIds.has(node.id);

  return (
    <li>
      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-pressed={active}
          onClick={() => onSelect(node.id)}
          style={{ paddingLeft: 12 + depth * 16 }}
          className={`min-w-0 flex-1 rounded-xl py-2 pr-2 text-left text-sm transition ${active ? 'bg-[#EEF4FF] font-semibold text-[#29266F]' : depth === 0 ? 'font-medium text-[#52638F] hover:bg-[#F8FAFE]' : 'text-[#667085] hover:bg-[#F8FAFE]'}`}
        >
          {node.name}
        </button>
        {hasChildren && <button type="button" aria-label={`${expanded ? 'Thu gọn' : 'Mở rộng'} danh mục ${node.name}`} aria-expanded={expanded} onClick={() => onToggle(node.id)} className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-[#667085] hover:bg-[#F0F4FA]">
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </button>}
      </div>
      {hasChildren && expanded && <ul className="space-y-1">{node.children!.map((child) => <CategoryItem key={child.id} node={child} depth={depth + 1} activeId={activeId} expandedIds={expandedIds} onToggle={onToggle} onSelect={onSelect} />)}</ul>}
    </li>
  );
}
