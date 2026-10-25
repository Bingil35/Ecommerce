import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle2, ChevronRight, CircleAlert, Heart, Info, Trash2 } from 'lucide-react';

import WishlistBenefits from '../../components/customer/wishlist/WishlistBenefits';
import WishlistFilters from '../../components/customer/wishlist/WishlistFilters';
import WishlistPagination from '../../components/customer/wishlist/WishlistPagination';
import WishlistProductCard from '../../components/customer/wishlist/WishlistProductCard';
import {
  ConfirmDialog,
  EmptyWishlist,
  NoWishlistResults,
  WishlistError,
  WishlistSkeleton,
} from '../../components/customer/wishlist/WishlistStates';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { useWishlist } from '../../context/WishlistContext';
import { getCartErrorMessage } from '../../services/cart/cartApi';
import {
  categoryApi,
  productApi,
  type ProductCardDto,
} from '../../services/catalog/productApi';
import {
  getHttpStatus,
  getWishlistErrorMessage,
  isAbortError,
  isNotFound,
  wishlistApi,
} from '../../services/wishlist/wishlistApi';
import type { WishlistItemDto } from '../../services/wishlist/wishlistTypes';
import {
  ALL_CATEGORIES,
  DEFAULT_PAGE_SIZE,
  EMPTY_FILTERS,
  buildCategoryChips,
  categoryKeyOf,
  countActiveFilters,
  matchesFilters,
  matchesKeyword,
  normalizeText,
  sortItems,
  type AdvancedFilters,
  type WishlistSort,
} from '../../services/wishlist/wishlistUtils';

type LoadStatus = 'loading' | 'ready' | 'error';
type ToastKind = 'success' | 'error' | 'info';

interface ToastState {
  id: number;
  kind: ToastKind;
  message: string;
}

/** Chỉ khách hàng đã đăng nhập mới vào được trang này (giống trang giỏ hàng). */
const WishlistPage = () => {
  const location = useLocation();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();

  if (isAuthLoading) return null;

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname,
          notice: 'Đăng nhập để xem danh sách yêu thích.',
        }}
      />
    );
  }

  return <WishlistContent />;
};

const WishlistContent = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { addToCart } = useCart();
  const { sync } = useWishlist();

  const [status, setStatus] = useState<LoadStatus>('loading');
  const [errorMessage, setErrorMessage] = useState('');
  const [items, setItems] = useState<WishlistItemDto[]>([]);
  const [serverTotal, setServerTotal] = useState(0);
  const [truncated, setTruncated] = useState(false);
  const [categoryNames, setCategoryNames] = useState<Map<string, string>>(new Map());

  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState(ALL_CATEGORIES);
  const [sort, setSort] = useState<WishlistSort>('newest');
  const [filters, setFilters] = useState<AdvancedFilters>(EMPTY_FILTERS);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState<number>(DEFAULT_PAGE_SIZE);

  const [removingIds, setRemovingIds] = useState<Set<string>>(new Set());
  const [cartBusyIds, setCartBusyIds] = useState<Set<string>>(new Set());
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isDeletingAll, setIsDeletingAll] = useState(false);
  const [toast, setToast] = useState<ToastState | null>(null);
  // Tăng lên để tải lại danh sách (nút "Thử lại", sau khi xóa hàng loạt)
  const [reloadToken, setReloadToken] = useState(0);

  // Chặn bấm lặp trước khi state kịp cập nhật
  const removeInFlight = useRef(new Set<string>());
  const cartInFlight = useRef(new Set<string>());
  const toastTimer = useRef<number | undefined>(undefined);
  const logoutRef = useRef(logout);
  useEffect(() => {
    logoutRef.current = logout;
  }, [logout]);

  const total = truncated ? serverTotal : items.length;

  const showToast = useCallback((kind: ToastKind, message: string) => {
    window.clearTimeout(toastTimer.current);
    setToast({ id: Date.now(), kind, message });
    toastTimer.current = window.setTimeout(() => setToast(null), 3500);
  }, []);

  useEffect(() => () => window.clearTimeout(toastTimer.current), []);

  /* ---------- Tải dữ liệu ---------- */

  useEffect(() => {
    const controller = new AbortController();

    const run = async () => {
      try {
        const result = await wishlistApi.listAll(controller.signal);
        if (controller.signal.aborted) return;
        setItems(result.items);
        setServerTotal(result.totalElements);
        setTruncated(result.truncated);
        setErrorMessage('');
        setStatus('ready');
      } catch (error) {
        if (isAbortError(error) || controller.signal.aborted) return;
        // Interceptor đã xóa token khi 401; đồng bộ AuthContext để route guard chuyển sang đăng nhập
        if (getHttpStatus(error) === 401) logoutRef.current();
        setErrorMessage(getWishlistErrorMessage(error));
        setStatus('error');
      }
    };

    void run();
    return () => controller.abort();
  }, [reloadToken]);

  const reload = useCallback(() => {
    setStatus('loading');
    setReloadToken((current) => current + 1);
  }, []);

  // Tên danh mục: BE chỉ trả categoryId trong thẻ sản phẩm nên cần danh sách danh mục công khai
  useEffect(() => {
    const controller = new AbortController();
    categoryApi
      .list()
      .then((list) => {
        if (!controller.signal.aborted) {
          setCategoryNames(new Map(list.map((c) => [c.id, c.name])));
        }
      })
      .catch(() => {
        /* Không có tên thì chip dùng nhãn mặc định, không chặn trang */
      });
    return () => controller.abort();
  }, []);

  // Badge trên Header luôn khớp với số đang hiển thị ở trang
  useEffect(() => {
    if (status === 'ready') {
      sync(
        items.map((item) => item.product.id),
        total
      );
    }
  }, [status, items, total, sync]);

  /* ---------- Dữ liệu hiển thị ---------- */

  // Khớp từ khóa + bộ lọc nâng cao (chưa áp danh mục) -> dùng để đếm số trên chip
  const matched = useMemo(() => {
    const normalized = normalizeText(keyword);
    return items.filter(
      (item) =>
        matchesKeyword(item.product, normalized) &&
        matchesFilters(item.product, filters)
    );
  }, [items, keyword, filters]);

  const chips = useMemo(
    () => buildCategoryChips(items, matched, categoryNames),
    [items, matched, categoryNames]
  );

  // Danh mục đang chọn không còn sản phẩm nào (đã xóa hết) -> coi như "Tất cả"
  const activeCategory = chips.some((chip) => chip.id === category)
    ? category
    : ALL_CATEGORIES;

  const visible = useMemo(() => {
    const inCategory =
      activeCategory === ALL_CATEGORIES
        ? matched
        : matched.filter((item) => categoryKeyOf(item.product) === activeCategory);
    return sortItems(inCategory, sort);
  }, [matched, activeCategory, sort]);

  const hasActiveFilters =
    keyword.trim() !== '' ||
    countActiveFilters(filters) > 0 ||
    activeCategory !== ALL_CATEGORIES;

  const totalPages = Math.max(1, Math.ceil(visible.length / pageSize));
  // Giữ trang trong giới hạn hợp lệ khi xóa bớt sản phẩm
  const currentPage = Math.min(page, totalPages);
  const pageItems = visible.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  /* ---------- Bộ lọc: luôn về trang 1 ---------- */

  const changeKeyword = (value: string) => {
    setKeyword(value);
    setPage(1);
  };
  const changeCategory = (value: string) => {
    setCategory(value);
    setPage(1);
  };
  const changeSort = (value: WishlistSort) => {
    setSort(value);
    setPage(1);
  };
  const changeFilters = (value: AdvancedFilters) => {
    setFilters(value);
    setPage(1);
  };
  const changePageSize = (value: number) => {
    setPageSize(value);
    setPage(1);
  };
  const resetFilters = () => {
    setKeyword('');
    setFilters(EMPTY_FILTERS);
    setCategory(ALL_CATEGORIES);
    setPage(1);
  };

  /* ---------- Xóa sản phẩm ---------- */

  const applyRemoved = useCallback((productIds: string[]) => {
    const ids = new Set(productIds);
    setItems((current) => current.filter((item) => !ids.has(item.product.id)));
    setServerTotal((current) => Math.max(0, current - ids.size));
  }, []);

  const handleRemove = async (product: ProductCardDto) => {
    if (removeInFlight.current.has(product.id)) return;
    removeInFlight.current.add(product.id);
    setRemovingIds((current) => new Set(current).add(product.id));

    try {
      await wishlistApi.remove(product.id);
      applyRemoved([product.id]);
      showToast('success', 'Đã xóa sản phẩm khỏi danh sách yêu thích.');
    } catch (error) {
      if (isNotFound(error)) {
        // Sản phẩm đã không còn trong danh sách (xóa ở thiết bị khác) -> đồng bộ lại giao diện
        applyRemoved([product.id]);
        showToast('info', 'Sản phẩm này không còn trong danh sách yêu thích.');
      } else {
        if (getHttpStatus(error) === 401) logoutRef.current();
        showToast('error', getWishlistErrorMessage(error));
      }
    } finally {
      removeInFlight.current.delete(product.id);
      setRemovingIds((current) => {
        const next = new Set(current);
        next.delete(product.id);
        return next;
      });
    }
  };

  const handleDeleteAll = async () => {
    if (isDeletingAll) return;
    setIsDeletingAll(true);

    try {
      const { removedIds, failedIds } = await wishlistApi.removeMany(
        items.map((item) => item.product.id)
      );
      applyRemoved(removedIds);
      setConfirmOpen(false);

      if (failedIds.length > 0) {
        showToast(
          'error',
          `Không thể xóa ${failedIds.length} sản phẩm. Vui lòng thử lại.`
        );
      } else {
        showToast('success', 'Đã xóa tất cả sản phẩm yêu thích.');
      }
      // Danh sách dài hơn giới hạn tải: tải lại phần còn lại
      if (truncated) reload();
    } finally {
      setIsDeletingAll(false);
    }
  };

  /* ---------- Thêm vào giỏ ---------- */

  const handleAddToCart = async (product: ProductCardDto) => {
    if (cartInFlight.current.has(product.id)) return;
    cartInFlight.current.add(product.id);
    setCartBusyIds((current) => new Set(current).add(product.id));

    try {
      // Giỏ hàng BE cần variantId, mà thẻ wishlist không có; lấy từ chi tiết sản phẩm
      const detail = await productApi.detail(product.slug);
      const variants = (detail.variants ?? []).filter(
        (variant) => !variant.status || variant.status === 'ACTIVE'
      );

      if (variants.length === 0) {
        showToast('error', 'Sản phẩm hiện chưa thể mua.');
        return;
      }

      // Nhiều phân loại -> để khách chọn ở trang chi tiết, không tự chọn thay
      if (variants.length > 1) {
        navigate(`/product/${product.slug || product.id}`);
        return;
      }

      const variant = variants[0];
      if (variant.stockQuantity != null && variant.stockQuantity <= 0) {
        showToast('error', 'Sản phẩm đã hết hàng.');
        return;
      }

      // CartContext tự báo lỗi (hết hàng, vượt tồn kho...) và cập nhật badge giỏ hàng khi thành công
      const added = await addToCart({
        productId: product.id,
        variantId: variant.id,
        quantity: 1,
      });
      if (added) showToast('success', 'Đã thêm sản phẩm vào giỏ hàng.');
    } catch (error) {
      showToast('error', getCartErrorMessage(error));
    } finally {
      cartInFlight.current.delete(product.id);
      setCartBusyIds((current) => {
        const next = new Set(current);
        next.delete(product.id);
        return next;
      });
    }
  };

  /* ---------- Render ---------- */

  const isEmptyWishlist = status === 'ready' && items.length === 0;

  return (
    <div className="mx-auto w-full max-w-[1200px] px-4 pb-10 pt-5 sm:pt-6">
      <nav
        aria-label="Đường dẫn"
        className="flex items-center gap-2 text-sm text-[#8A93A6]"
      >
        <Link to="/" className="hover:text-[#5367C9]">
          Trang chủ
        </Link>
        <ChevronRight size={14} aria-hidden="true" />
        <span aria-current="page" className="font-medium text-[#202642]">
          Sản phẩm yêu thích
        </span>
      </nav>

      <div className="mt-6 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="flex items-center gap-3 text-2xl font-bold text-[#202642] sm:text-[28px]">
            <Heart
              size={28}
              fill="currentColor"
              strokeWidth={1.5}
              className="shrink-0 text-[#E5334F]"
            />
            Sản phẩm yêu thích
          </h1>
          <p className="mt-2 text-sm text-[#7B8498]" aria-live="polite">
            {status === 'ready' ? `${total} sản phẩm` : '\u00A0'}
          </p>
        </div>

        <button
          type="button"
          disabled={status !== 'ready' || items.length === 0}
          onClick={() => setConfirmOpen(true)}
          className="flex h-11 shrink-0 items-center gap-2 rounded-full border border-[#FAD0D8] bg-[#FFF5F7] px-5 text-sm font-semibold text-[#E5334F] transition hover:bg-[#FFE9ED] disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Trash2 size={16} strokeWidth={1.9} />
          Xóa tất cả
        </button>
      </div>

      {status === 'loading' && (
        <div className="mt-6">
          <WishlistSkeleton />
        </div>
      )}

      {status === 'error' && (
        <div className="mt-6">
          <WishlistError message={errorMessage} onRetry={reload} />
        </div>
      )}

      {isEmptyWishlist && (
        <div className="mt-6">
          <EmptyWishlist />
        </div>
      )}

      {status === 'ready' && items.length > 0 && (
        <>
          <div className="mt-6">
            <WishlistFilters
              keyword={keyword}
              onKeywordChange={changeKeyword}
              sort={sort}
              onSortChange={changeSort}
              filters={filters}
              onFiltersChange={changeFilters}
              chips={chips}
              matchedTotal={matched.length}
              activeCategory={activeCategory}
              onCategoryChange={changeCategory}
            />
          </div>

          {truncated && (
            <p className="mt-4 rounded-xl border border-[#DCE5FA] bg-[#F4F7FF] px-4 py-2.5 text-sm text-[#5367C9]">
              Danh sách của bạn rất dài nên chỉ hiển thị {items.length} sản phẩm
              mới thêm gần đây nhất.
            </p>
          )}

          <div className="mt-5">
            {visible.length === 0 ? (
              <NoWishlistResults onReset={resetFilters} />
            ) : (
              <>
                {hasActiveFilters && (
                  <p className="mb-3 text-sm text-[#7B8498]">
                    {visible.length} kết quả phù hợp
                  </p>
                )}
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
                  {pageItems.map((item) => (
                    <WishlistProductCard
                      key={item.product.id}
                      product={item.product}
                      isRemoving={removingIds.has(item.product.id)}
                      isAddingToCart={cartBusyIds.has(item.product.id)}
                      onRemove={handleRemove}
                      onAddToCart={handleAddToCart}
                    />
                  ))}
                </div>
              </>
            )}
          </div>

          {visible.length > 0 && (
            <div className="mt-8">
              <WishlistPagination
                page={currentPage}
                totalPages={totalPages}
                pageSize={pageSize}
                totalItems={visible.length}
                onPageChange={setPage}
                onPageSizeChange={changePageSize}
              />
            </div>
          )}
        </>
      )}

      <div className="mt-10">
        <WishlistBenefits />
      </div>

      {confirmOpen && (
        <ConfirmDialog
          title="Xóa tất cả sản phẩm yêu thích?"
          description={`${total} sản phẩm sẽ bị xóa khỏi danh sách yêu thích của bạn. Thao tác này không thể hoàn tác.`}
          confirmLabel="Xóa tất cả"
          isBusy={isDeletingAll}
          onConfirm={() => void handleDeleteAll()}
          onCancel={() => setConfirmOpen(false)}
        />
      )}

      {toast && <Toast key={toast.id} toast={toast} />}
    </div>
  );
};

const TOAST_STYLES: Record<ToastKind, string> = {
  success: 'border-[#CFEBDD] text-[#1F7A4D]',
  error: 'border-[#F6D3D8] text-[#C73049]',
  info: 'border-[#DCE5FA] text-[#4559B7]',
};

const Toast = ({ toast }: { toast: ToastState }) => {
  const Icon =
    toast.kind === 'success' ? CheckCircle2 : toast.kind === 'error' ? CircleAlert : Info;

  return (
    <div
      role={toast.kind === 'error' ? 'alert' : 'status'}
      className={`fixed bottom-6 left-1/2 z-[70] flex max-w-[calc(100vw-2rem)] -translate-x-1/2 items-center gap-2.5 rounded-xl border bg-white px-4 py-3 text-sm font-medium shadow-[0_12px_32px_rgba(31,37,64,0.18)] ${TOAST_STYLES[toast.kind]}`}
    >
      <Icon size={18} className="shrink-0" />
      <span>{toast.message}</span>
    </div>
  );
};

export default WishlistPage;
