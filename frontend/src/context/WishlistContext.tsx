import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import { useAuth } from './AuthContext';
import {
  getWishlistErrorMessage,
  isAbortError,
  isNotFound,
  wishlistApi,
} from '../services/wishlist/wishlistApi';

interface WishlistContextValue {
  /** Số sản phẩm yêu thích đang hiển thị (badge ở Header) */
  count: number;
  /** Sản phẩm này đã nằm trong danh sách yêu thích của tài khoản hiện tại chưa */
  isWishlisted: (productId: string) => boolean;
  /** Đang gọi API thêm/xóa sản phẩm này */
  isPending: (productId: string) => boolean;
  /**
   * Thêm sản phẩm vào yêu thích, hoặc bỏ nếu đã có. Trả về trạng thái mới
   * (true = đang được yêu thích), hoặc null nếu thao tác thất bại / đang xử lý.
   */
  toggle: (productId: string) => Promise<boolean | null>;
  refresh: () => Promise<void>;
  /** Trang Wishlist đã tải xong danh sách: đồng bộ để mọi nơi hiển thị khớp nhau */
  sync: (productIds: string[], total: number) => void;
}

interface Snapshot {
  userId: string | null;
  ids: ReadonlySet<string>;
  count: number;
}

const EMPTY_IDS: ReadonlySet<string> = new Set();
const EMPTY: Snapshot = { userId: null, ids: EMPTY_IDS, count: 0 };

const WishlistContext = createContext<WishlistContextValue | undefined>(
  undefined
);

export const WishlistProvider = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();

  // Dữ liệu gắn với tài khoản đã tải nó, để đăng xuất/đổi tài khoản không hiện dữ liệu người cũ
  const [snapshot, setSnapshot] = useState<Snapshot>(EMPTY);
  const [pendingIds, setPendingIds] = useState<ReadonlySet<string>>(EMPTY_IDS);

  const userId = isAuthenticated ? (user?.id ?? null) : null;
  const userIdRef = useRef<string | null>(null);
  useEffect(() => {
    userIdRef.current = userId;
  }, [userId]);

  const current = userId !== null && snapshot.userId === userId ? snapshot : EMPTY;
  const currentRef = useRef(current);
  useEffect(() => {
    currentRef.current = current;
  }, [current]);

  // Bỏ qua kết quả của request cũ (đã đăng xuất / đã có dữ liệu chắc chắn hơn)
  const requestIdRef = useRef(0);
  const controllerRef = useRef<AbortController | null>(null);
  const inFlight = useRef(new Set<string>());

  const refresh = useCallback(async () => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    const requestId = ++requestIdRef.current;

    try {
      const result = await wishlistApi.listAll(controller.signal);
      if (requestId !== requestIdRef.current) return;
      setSnapshot({
        userId: userIdRef.current,
        ids: new Set(result.items.map((item) => item.product.id)),
        count: result.totalElements,
      });
    } catch (error) {
      if (isAbortError(error) || requestId !== requestIdRef.current) return;
      // Chỉ là thông tin phụ: lỗi (kể cả 403 với shop owner/admin) thì coi như chưa có gì
      setSnapshot({ userId: userIdRef.current, ids: EMPTY_IDS, count: 0 });
    }
  }, []);

  useEffect(() => {
    if (isAuthLoading) return;

    if (isAuthenticated) {
      void refresh();
    } else {
      controllerRef.current?.abort();
      requestIdRef.current++;
    }

    return () => controllerRef.current?.abort();
  }, [isAuthenticated, isAuthLoading, user?.id, refresh]);

  const sync = useCallback((productIds: string[], total: number) => {
    requestIdRef.current++; // một giá trị đã biết chắc thì thắng mọi request đang bay
    setSnapshot({
      userId: userIdRef.current,
      ids: new Set(productIds),
      count: Math.max(0, total),
    });
  }, []);

  const toggle = useCallback(async (productId: string) => {
    if (inFlight.current.has(productId)) return null;
    inFlight.current.add(productId);
    setPendingIds((ids) => new Set(ids).add(productId));

    const wasSaved = currentRef.current.ids.has(productId);
    const apply = (saved: boolean) => {
      requestIdRef.current++;
      const previous = currentRef.current;
      const ids = new Set(previous.ids);
      const had = ids.has(productId);
      if (saved) ids.add(productId);
      else ids.delete(productId);
      const delta = saved === had ? 0 : saved ? 1 : -1;
      const next: Snapshot = {
        userId: userIdRef.current,
        ids,
        count: Math.max(0, previous.count + delta),
      };
      // Cập nhật ref ngay để lần bấm kế tiếp thấy trạng thái mới, không chờ render
      currentRef.current = next;
      setSnapshot(next);
    };

    try {
      if (wasSaved) {
        await wishlistApi.remove(productId);
        apply(false);
        return false;
      }
      await wishlistApi.add(productId);
      apply(true);
      return true;
    } catch (error) {
      // Xóa một mục đã không còn trong danh sách (xóa ở thiết bị khác) -> khớp lại giao diện
      if (wasSaved && isNotFound(error)) {
        apply(false);
        return false;
      }
      window.alert(getWishlistErrorMessage(error));
      return null;
    } finally {
      inFlight.current.delete(productId);
      setPendingIds((ids) => {
        const next = new Set(ids);
        next.delete(productId);
        return next;
      });
    }
  }, []);

  const value = useMemo<WishlistContextValue>(
    () => ({
      count: current.count,
      isWishlisted: (productId) => current.ids.has(productId),
      isPending: (productId) => pendingIds.has(productId),
      toggle,
      refresh,
      sync,
    }),
    [current, pendingIds, toggle, refresh, sync]
  );

  return (
    <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>
  );
};

export const useWishlist = () => {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error('useWishlist must be used within WishlistProvider');
  }
  return context;
};
