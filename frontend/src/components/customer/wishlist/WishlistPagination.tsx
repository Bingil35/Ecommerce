import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';

import {
  PAGE_SIZE_OPTIONS,
  buildPageList,
} from '../../../services/wishlist/wishlistUtils';

interface WishlistPaginationProps {
  page: number;
  totalPages: number;
  pageSize: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}

const buttonBase =
  'flex h-9 min-w-9 items-center justify-center rounded-lg px-2 text-sm font-medium transition';

const WishlistPagination = ({
  page,
  totalPages,
  pageSize,
  totalItems,
  onPageChange,
  onPageSizeChange,
}: WishlistPaginationProps) => {
  const tokens = buildPageList(page, totalPages);

  return (
    <div className="relative flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
      {totalPages > 1 && (
        <nav
          aria-label="Phân trang"
          className="flex items-center gap-1 rounded-xl border border-[#E1E7F2] bg-white p-1 shadow-[0_2px_10px_rgba(72,93,140,0.04)]"
        >
          <button
            type="button"
            aria-label="Trang trước"
            disabled={page <= 1}
            onClick={() => onPageChange(page - 1)}
            className={`${buttonBase} text-[#4C5570] hover:bg-[#F3F6FC] disabled:cursor-not-allowed disabled:text-[#C3CAD9] disabled:hover:bg-transparent`}
          >
            <ChevronLeft size={18} />
          </button>

          {tokens.map((token) =>
            typeof token === 'number' ? (
              <button
                key={token}
                type="button"
                aria-label={`Trang ${token}`}
                aria-current={token === page ? 'page' : undefined}
                onClick={() => onPageChange(token)}
                className={`${buttonBase} ${
                  token === page
                    ? 'bg-[#5367C9] text-white shadow-sm'
                    : 'text-[#4C5570] hover:bg-[#F3F6FC]'
                }`}
              >
                {token}
              </button>
            ) : (
              <span
                key={token}
                aria-hidden="true"
                className="flex h-9 min-w-7 items-center justify-center text-sm text-[#9AA2B1]"
              >
                …
              </span>
            )
          )}

          <button
            type="button"
            aria-label="Trang sau"
            disabled={page >= totalPages}
            onClick={() => onPageChange(page + 1)}
            className={`${buttonBase} text-[#4C5570] hover:bg-[#F3F6FC] disabled:cursor-not-allowed disabled:text-[#C3CAD9] disabled:hover:bg-transparent`}
          >
            <ChevronRight size={18} />
          </button>
        </nav>
      )}

      {totalItems > PAGE_SIZE_OPTIONS[0] && (
        <label className="flex items-center gap-2 rounded-xl border border-[#E1E7F2] bg-white py-1 pl-3 pr-1 text-sm text-[#4C5570] sm:absolute sm:right-0">
          Hiển thị
          <span className="relative">
            <select
              value={pageSize}
              onChange={(event) => onPageSizeChange(Number(event.target.value))}
              className="h-8 cursor-pointer appearance-none rounded-lg bg-transparent pl-2 pr-7 text-sm font-medium text-[#202642] outline-none focus-visible:ring-2 focus-visible:ring-[#C7D2F5]"
            >
              {PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size} / trang
                </option>
              ))}
            </select>
            <ChevronDown
              size={15}
              className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-[#7B8498]"
            />
          </span>
        </label>
      )}
    </div>
  );
};

export default WishlistPagination;
