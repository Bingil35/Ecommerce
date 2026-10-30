import { useEffect, useId, useRef, useState, type ReactNode } from 'react';
import {
  ChevronDown,
  LayoutGrid,
  Search,
  SlidersHorizontal,
  Tag,
  X,
} from 'lucide-react';

import {
  ALL_CATEGORIES,
  EMPTY_FILTERS,
  SORT_OPTIONS,
  countActiveFilters,
  type AdvancedFilters,
  type CategoryChip,
  type WishlistSort,
} from '../../../services/wishlist/wishlistUtils';

interface WishlistFiltersProps {
  keyword: string;
  onKeywordChange: (value: string) => void;
  sort: WishlistSort;
  onSortChange: (value: WishlistSort) => void;
  filters: AdvancedFilters;
  onFiltersChange: (value: AdvancedFilters) => void;
  chips: CategoryChip[];
  /** Số sản phẩm khớp từ khóa + bộ lọc, hiển thị ở chip "Tất cả" */
  matchedTotal: number;
  activeCategory: string;
  onCategoryChange: (id: string) => void;
}

const controlBase =
  'h-11 rounded-xl border border-[#E1E7F2] bg-white text-sm text-[#202642] shadow-[0_2px_10px_rgba(72,93,140,0.03)]';

const RATING_OPTIONS = [
  { value: 4.5, label: 'Từ 4.5 sao' },
  { value: 4, label: 'Từ 4 sao' },
  { value: 3, label: 'Từ 3 sao' },
] as const;

const WishlistFilters = ({
  keyword,
  onKeywordChange,
  sort,
  onSortChange,
  filters,
  onFiltersChange,
  chips,
  matchedTotal,
  activeCategory,
  onCategoryChange,
}: WishlistFiltersProps) => {
  const activeFilterCount = countActiveFilters(filters);

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 md:flex-row">
        <div className="relative min-w-0 flex-1">
          <Search
            size={18}
            strokeWidth={1.8}
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#9AA2B1]"
          />
          <input
            type="text"
            value={keyword}
            onChange={(event) => onKeywordChange(event.target.value)}
            placeholder="Tìm trong danh sách yêu thích..."
            aria-label="Tìm trong danh sách yêu thích"
            className={`${controlBase} w-full pl-11 pr-10 outline-none placeholder:text-[#9AA2B1] focus:border-[#9FB2EA] focus:ring-2 focus:ring-[#DCE5F5]`}
          />
          {keyword && (
            <button
              type="button"
              aria-label="Xóa từ khóa tìm kiếm"
              onClick={() => onKeywordChange('')}
              className="absolute right-3 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-[#9AA2B1] hover:bg-[#F3F6FC] hover:text-[#4C5570]"
            >
              <X size={15} />
            </button>
          )}
        </div>

        <div className="flex gap-3">
          <label className="relative min-w-0 flex-1 md:flex-none">
            <span className="sr-only">Sắp xếp</span>
            <select
              value={sort}
              onChange={(event) => onSortChange(event.target.value as WishlistSort)}
              className={`${controlBase} w-full cursor-pointer appearance-none pl-4 pr-10 font-medium outline-none focus:border-[#9FB2EA] focus:ring-2 focus:ring-[#DCE5F5] md:w-[250px]`}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  Sắp xếp: {option.label}
                </option>
              ))}
            </select>
            <ChevronDown
              size={16}
              className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[#7B8498]"
            />
          </label>

          <AdvancedFilterPopover
            filters={filters}
            activeCount={activeFilterCount}
            onApply={onFiltersChange}
          />
        </div>
      </div>

      {/* Category chips */}
      <div
        role="tablist"
        aria-label="Danh mục sản phẩm yêu thích"
        className="-mx-4 flex gap-2.5 overflow-x-auto px-4 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden sm:mx-0 sm:px-0"
      >
        <CategoryChipButton
          icon={<LayoutGrid size={16} strokeWidth={1.8} />}
          label="Tất cả"
          count={matchedTotal}
          active={activeCategory === ALL_CATEGORIES}
          onClick={() => onCategoryChange(ALL_CATEGORIES)}
        />
        {chips.map((chip) => (
          <CategoryChipButton
            key={chip.id}
            icon={<Tag size={16} strokeWidth={1.8} />}
            label={chip.name}
            count={chip.count}
            active={activeCategory === chip.id}
            onClick={() => onCategoryChange(chip.id)}
          />
        ))}
      </div>
    </div>
  );
};

interface CategoryChipButtonProps {
  icon: ReactNode;
  label: string;
  count: number;
  active: boolean;
  onClick: () => void;
}

const CategoryChipButton = ({
  icon,
  label,
  count,
  active,
  onClick,
}: CategoryChipButtonProps) => (
  <button
    type="button"
    role="tab"
    aria-selected={active}
    aria-label={`${label} (${count})`}
    onClick={onClick}
    className={`flex h-11 shrink-0 items-center gap-2 rounded-xl border px-4 text-sm font-medium transition ${
      active
        ? 'border-[#5367C9] bg-[#5367C9] text-white shadow-[0_6px_16px_rgba(83,103,201,0.25)]'
        : count === 0
          ? 'border-[#E8EDF5] bg-white text-[#A4ACBD] hover:border-[#C7D2F5]'
          : 'border-[#E1E7F2] bg-white text-[#3B4560] hover:border-[#B7C4F2] hover:text-[#5367C9]'
    }`}
  >
    {icon}
    <span className="max-w-[140px] truncate">{label}</span>
    <span className={active ? 'text-white/90' : 'text-inherit'}>({count})</span>
  </button>
);

/* ---------------- Bộ lọc nâng cao ---------------- */

interface AdvancedFilterPopoverProps {
  filters: AdvancedFilters;
  activeCount: number;
  onApply: (filters: AdvancedFilters) => void;
}

const AdvancedFilterPopover = ({
  filters,
  activeCount,
  onApply,
}: AdvancedFilterPopoverProps) => {
  const panelId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<AdvancedFilters>(filters);
  const [minText, setMinText] = useState('');
  const [maxText, setMaxText] = useState('');
  const [error, setError] = useState('');

  const openPanel = () => {
    setDraft(filters);
    setMinText(filters.minPrice != null ? String(filters.minPrice) : '');
    setMaxText(filters.maxPrice != null ? String(filters.maxPrice) : '');
    setError('');
    setOpen(true);
  };

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };

    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const parsePrice = (text: string): number | null | 'invalid' => {
    if (!text.trim()) return null;
    const value = Number(text);
    return Number.isFinite(value) && value >= 0 ? value : 'invalid';
  };

  const apply = () => {
    const minPrice = parsePrice(minText);
    const maxPrice = parsePrice(maxText);

    if (minPrice === 'invalid' || maxPrice === 'invalid') {
      setError('Giá không hợp lệ.');
      return;
    }
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
      setError('Giá tối thiểu không được lớn hơn giá tối đa.');
      return;
    }

    onApply({ ...draft, minPrice, maxPrice });
    setOpen(false);
  };

  const reset = () => {
    onApply(EMPTY_FILTERS);
    setOpen(false);
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => (open ? setOpen(false) : openPanel())}
        className={`${controlBase} relative flex items-center gap-2 px-5 font-medium transition hover:border-[#B7C4F2] hover:text-[#5367C9]`}
      >
        <SlidersHorizontal size={17} strokeWidth={1.8} />
        Bộ lọc
        {activeCount > 0 && (
          <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-[#5367C9] px-1 text-[11px] font-semibold text-white">
            {activeCount}
          </span>
        )}
      </button>

      {open && (
        <div
          id={panelId}
          role="dialog"
          aria-label="Bộ lọc nâng cao"
          className="absolute right-0 top-[calc(100%+8px)] z-30 w-[min(320px,calc(100vw-2rem))] rounded-2xl border border-[#E1E7F2] bg-white p-4 shadow-[0_16px_40px_rgba(61,82,120,0.16)]"
        >
          <fieldset>
            <legend className="mb-2 text-sm font-semibold text-[#202642]">
              Khoảng giá (₫)
            </legend>
            <div className="flex items-center gap-2">
              <input
                type="number"
                inputMode="numeric"
                min={0}
                value={minText}
                onChange={(event) => setMinText(event.target.value)}
                placeholder="Từ"
                aria-label="Giá tối thiểu"
                className="h-10 w-full min-w-0 rounded-lg border border-[#E1E7F2] px-3 text-sm outline-none focus:border-[#9FB2EA]"
              />
              <span className="text-[#9AA2B1]">–</span>
              <input
                type="number"
                inputMode="numeric"
                min={0}
                value={maxText}
                onChange={(event) => setMaxText(event.target.value)}
                placeholder="Đến"
                aria-label="Giá tối đa"
                className="h-10 w-full min-w-0 rounded-lg border border-[#E1E7F2] px-3 text-sm outline-none focus:border-[#9FB2EA]"
              />
            </div>
          </fieldset>

          <fieldset className="mt-4">
            <legend className="mb-2 text-sm font-semibold text-[#202642]">
              Đánh giá
            </legend>
            <div className="flex flex-wrap gap-2">
              {RATING_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={draft.minRating === option.value}
                  onClick={() =>
                    setDraft((current) => ({
                      ...current,
                      minRating: current.minRating === option.value ? null : option.value,
                    }))
                  }
                  className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition ${
                    draft.minRating === option.value
                      ? 'border-[#5367C9] bg-[#EEF1FD] text-[#3F4FC4]'
                      : 'border-[#E1E7F2] text-[#4C5570] hover:border-[#B7C4F2]'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </fieldset>

          <div className="mt-4 space-y-2.5">
            <label className="flex cursor-pointer items-center gap-2.5 text-sm text-[#343D55]">
              <input
                type="checkbox"
                checked={draft.inStockOnly}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, inStockOnly: event.target.checked }))
                }
                className="h-4 w-4 accent-[#5367C9]"
              />
              Chỉ sản phẩm còn hàng
            </label>
            <label className="flex cursor-pointer items-center gap-2.5 text-sm text-[#343D55]">
              <input
                type="checkbox"
                checked={draft.onSaleOnly}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, onSaleOnly: event.target.checked }))
                }
                className="h-4 w-4 accent-[#5367C9]"
              />
              Chỉ sản phẩm đang giảm giá
            </label>
          </div>

          {error && (
            <p role="alert" className="mt-3 text-xs text-[#DC5A5A]">
              {error}
            </p>
          )}

          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={reset}
              className="h-10 flex-1 rounded-lg border border-[#E1E7F2] text-sm font-semibold text-[#4C5570] hover:bg-[#F7F9FD]"
            >
              Đặt lại
            </button>
            <button
              type="button"
              onClick={apply}
              className="h-10 flex-1 rounded-lg bg-[#5367C9] text-sm font-semibold text-white hover:bg-[#4659B8]"
            >
              Áp dụng
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default WishlistFilters;
