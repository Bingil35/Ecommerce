import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';

import ProductCard, { formatPrice } from '../../components/ProductCard';
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
  { id: 'top-rated', label: 'Đánh giá cao nhất' },
];

const RATING_OPTIONS = [4, 3, 2];

const cardClass =
  'rounded-2xl bg-white border border-[#E5EBF6]';

export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const filters = useMemo(() => parseFilters(params), [params]);

  const [keyword, setKeyword] = useState(filters.q);
  const [minInput, setMinInput] = useState(String(filters.minPrice ?? ''));
  const [maxInput, setMaxInput] = useState(String(filters.maxPrice ?? ''));

  const [result, setResult] = useState<SearchResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [categories, setCategories] = useState<Category[]>([]);

  // Đồng bộ các ô nhập khi URL đổi (bấm Back, xóa bộ lọc...).
  useEffect(() => {
    setKeyword(filters.q);
    setMinInput(String(filters.minPrice ?? ''));
    setMaxInput(String(filters.maxPrice ?? ''));
  }, [filters.q, filters.minPrice, filters.maxPrice]);

  // Tải danh mục một lần khi mở trang.
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

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    update({ q: keyword.trim() });
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

  if (filters.category) {
    activeChips.push({
      key: 'category',
      label: findCategory(filters.category, categories)?.name ?? filters.category,
      remove: () => update({ category: '' }),
    });
  }

  if (filters.minPrice !== undefined || filters.maxPrice !== undefined) {
    const from = filters.minPrice !== undefined ? formatPrice(filters.minPrice) : '0đ';
    const to = filters.maxPrice !== undefined ? formatPrice(filters.maxPrice) : 'trở lên';

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

  if (filters.inStock) {
    activeChips.push({
      key: 'inStock',
      label: 'Còn hàng',
      remove: () => update({ inStock: false }),
    });
  }

  const inputClass =
    'h-11 w-full rounded-xl border border-[#D7E0F2] bg-white px-3 text-sm text-[#172033] outline-none transition placeholder:text-[#9AA8C6] focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10';

  return (
    <div className="min-h-screen bg-[#F7F9FC] px-4 py-6 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-6xl">
        {/* ================= SEARCH BAR ================= */}

        <form
          onSubmit={handleSearch}
          role="search"
          className={`${cardClass} p-5 sm:p-6`}
        >
          <h1 className="text-[26px] font-bold tracking-tight text-[#29266F]">
            Tìm kiếm sản phẩm
          </h1>

          <div className="mt-4 flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <label htmlFor="keyword" className="sr-only">
                Từ khóa
              </label>

              <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#52638F]">
                <Search className="h-[22px] w-[22px]" />
              </span>
              <input
                id="keyword"
                type="text"
                value={keyword}
                placeholder="Nhập tên sản phẩm cần tìm"
                autoComplete="off"
                onChange={(event) => setKeyword(event.target.value)}
                className="h-[56px] w-full rounded-xl border border-[#D7E0F2] bg-white pl-14 pr-4 text-[15px] text-[#172033] outline-none transition placeholder:text-[#9AA8C6] focus:border-[#5B8DEF] focus:ring-4 focus:ring-[#5B8DEF]/10"
              />
            </div>

            <button
              type="submit"
              className="flex h-[56px] items-center justify-center rounded-full bg-[#4F7CFF] px-10 text-[15px] font-semibold text-white shadow-[0_8px_20px_rgba(79,124,255,0.2)] transition hover:bg-[#3D63D8] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#5B8DEF]/30"
            >
              Tìm kiếm
            </button>
          </div>
        </form>

        <div className="mt-6 grid gap-6 lg:grid-cols-[280px_1fr]">
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
                      onSelect={(id) =>
                        update({ category: id === filters.category ? '' : id })
                      }
                    />
                  ))}
                </ul>
              </fieldset>
            )}

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

            {/* Trạng thái */}
            <label className="flex cursor-pointer items-center gap-2.5 border-t border-[#E1E7F2] pt-6 text-sm text-[#667085]">
              <input
                type="checkbox"
                checked={filters.inStock}
                onChange={(event) => update({ inStock: event.target.checked })}
                className="h-5 w-5 cursor-pointer accent-[#4F7CFF]"
              />
              Chỉ hiện sản phẩm còn hàng
            </label>
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
                  className={`mt-5 grid grid-cols-2 gap-4 md:grid-cols-3 ${
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
  onSelect: (id: string) => void;
}

function CategoryItem({ node, depth, activeId, onSelect }: CategoryItemProps) {
  const active = activeId === node.id;

  return (
    <li>
      <button
        type="button"
        aria-pressed={active}
        onClick={() => onSelect(node.id)}
        style={{ paddingLeft: 12 + depth * 16 }}
        className={`w-full rounded-xl py-2 pr-3 text-left text-sm transition ${
          active
            ? 'bg-[#EEF4FF] font-semibold text-[#29266F]'
            : depth === 0
              ? 'font-medium text-[#52638F] hover:bg-[#F8FAFE]'
              : 'text-[#667085] hover:bg-[#F8FAFE]'
        }`}
      >
        {node.name}
      </button>

      {node.children && (
        <ul className="space-y-1">
          {node.children.map((child) => (
            <CategoryItem
              key={child.id}
              node={child}
              depth={depth + 1}
              activeId={activeId}
              onSelect={onSelect}
            />
          ))}
        </ul>
      )}
    </li>
  );
}