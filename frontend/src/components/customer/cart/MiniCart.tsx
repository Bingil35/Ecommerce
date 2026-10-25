import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ShoppingCart } from 'lucide-react';

import { useAuth } from '../../../context/AuthContext';
import { useCart } from '../../../context/CartContext';
import { formatVnd } from '../../../services/catalog/productApi';
import { getProductPath } from '../../../services/cart/cartUtils';
import type { CartItem } from '../../../services/cart/cartTypes';

/** Số sản phẩm mới thêm hiển thị trong dropdown */
const MAX_ITEMS = 5;
/** Sản phẩm thêm quá 7 ngày không còn tính là "mới thêm" */
const RECENT_TTL = 7 * 24 * 60 * 60 * 1000;

interface RecentEntry {
  id: string;
  at: number;
}

const storageKey = (userId?: string | number | null) =>
  `kala:recent-cart:${userId ?? 'guest'}`;

const readRecent = (key: string): RecentEntry[] => {
  try {
    const parsed = JSON.parse(localStorage.getItem(key) ?? '[]');
    const now = Date.now();
    return Array.isArray(parsed)
      ? parsed.filter(
          (e): e is RecentEntry =>
            typeof e?.id === 'string' && typeof e?.at === 'number' && now - e.at < RECENT_TTL
        )
      : [];
  } catch {
    return [];
  }
};

const writeRecent = (key: string, entries: RecentEntry[]) => {
  try {
    localStorage.setItem(key, JSON.stringify(entries));
  } catch {
    // Bỏ qua nếu trình duyệt chặn localStorage
  }
};
/** Trễ khi chuột rời đi, để kịp di chuột từ icon xuống dropdown */
const CLOSE_DELAY = 180;
const PANEL_WIDTH = 400;
const SCREEN_GAP = 12;

const PLACEHOLDER = 'https://placehold.co/600x600/F2F5FC/6575B8?text=Kala';

const getVariantLabel = (item: CartItem): string => {
  if (item.selectedOptions && item.selectedOptions.length > 0) {
    return item.selectedOptions.map((opt) => opt.value).join(' • ');
  }
  return item.variantName ?? '';
};

interface PanelPosition {
  top: number;
  right: number;
  width: number;
  /** Khoảng cách từ mép phải panel tới tâm mũi tên */
  caretRight: number;
}

/**
 * Icon giỏ hàng + dropdown "Sản phẩm mới thêm".
 * - Rê chuột vào icon -> xổ dropdown; rời chuột -> đóng.
 * - Bấm icon không chuyển trang (trên cảm ứng thì bật/tắt dropdown).
 * - Chỉ bấm "Xem giỏ hàng" mới vào trang giỏ hàng.
 */
const MiniCart = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user } = useAuth();
  const { items, itemCount, isLoading } = useCart();

  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<PanelPosition>({
    top: 0,
    right: SCREEN_GAP,
    width: PANEL_WIDTH,
    caretRight: 24,
  });

  const wrapperRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const closeTimer = useRef<number | undefined>(undefined);

  /* ===== Theo dõi sản phẩm mới thêm =====
     Backend chưa trả thời điểm thêm, nên so sánh giỏ hàng trước/sau mỗi thao tác:
     sản phẩm mới xuất hiện hoặc tăng số lượng -> đưa lên đầu danh sách "mới thêm". */
  const userKey = storageKey(user?.id);
  const [recent, setRecent] = useState<RecentEntry[]>([]);
  const baseline = useRef<Map<string, number> | null>(null);
  const sawLoading = useRef(false);

  // Nạp lại danh sách đã lưu khi đổi tài khoản
  useEffect(() => {
    setRecent(isAuthenticated ? readRecent(userKey) : []);
  }, [userKey, isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      baseline.current = null;
      sawLoading.current = false;
      return;
    }
    // Đang tải giỏ hàng từ server: dữ liệu tải về không tính là "mới thêm"
    if (isLoading) {
      baseline.current = null;
      sawLoading.current = true;
      return;
    }

    const current = new Map(items.map((item) => [item.id, item.quantity]));

    if (baseline.current === null) {
      if (sawLoading.current) {
        baseline.current = current;
        sawLoading.current = false;
      }
      return;
    }

    const addedIds = items
      .filter((item) => {
        const before = baseline.current!.get(item.id);
        return before === undefined || item.quantity > before;
      })
      .map((item) => item.id);

    baseline.current = current;
    if (addedIds.length === 0) return;

    setRecent((previous) => {
      const now = Date.now();
      const next = [
        ...addedIds.map((id) => ({ id, at: now })),
        ...previous.filter((entry) => !addedIds.includes(entry.id)),
      ]
        .filter((entry) => current.has(entry.id))
        .slice(0, 20);
      writeRecent(userKey, next);
      return next;
    });
  }, [items, isLoading, isAuthenticated, userKey]);
  const pointerType = useRef<string>('mouse');

  const updatePosition = useCallback(() => {
    const el = wrapperRef.current;
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const width = Math.min(PANEL_WIDTH, window.innerWidth - SCREEN_GAP * 2);
    // Canh mép phải panel theo icon, nhưng không để tràn khỏi màn hình
    const right = Math.max(
      SCREEN_GAP,
      Math.min(window.innerWidth - rect.right, window.innerWidth - width - SCREEN_GAP)
    );
    const iconCenterFromRight = window.innerWidth - (rect.left + rect.width / 2);
    const caretRight = Math.min(Math.max(iconCenterFromRight - right - 6, 14), width - 26);

    setPosition({ top: rect.bottom + 10, right, width, caretRight });
  }, []);

  const cancelClose = () => window.clearTimeout(closeTimer.current);

  const openNow = useCallback(() => {
    window.clearTimeout(closeTimer.current);
    updatePosition();
    setOpen(true);
  }, [updatePosition]);

  const scheduleClose = () => {
    window.clearTimeout(closeTimer.current);
    closeTimer.current = window.setTimeout(() => setOpen(false), CLOSE_DELAY);
  };

  // Chỉ phản ứng hover với chuột (cảm ứng dùng click)
  const handlePointerEnter = (event: React.PointerEvent) => {
    pointerType.current = event.pointerType;
    if (event.pointerType === 'mouse' && isAuthenticated) openNow();
  };
  const handlePointerLeave = (event: React.PointerEvent) => {
    if (event.pointerType === 'mouse') scheduleClose();
  };

  const handleTriggerClick = (event: React.MouseEvent) => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: { from: location.pathname, notice: 'Đăng nhập để sử dụng giỏ hàng.' },
      });
      return;
    }
    // Chuột: dropdown đã mở nhờ hover, bấm icon không làm gì
    if (pointerType.current === 'mouse' && event.detail > 0) return;
    if (open) setOpen(false);
    else openNow();
  };

  // Đóng khi đổi trang
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  // Đóng khi đăng xuất
  useEffect(() => {
    if (!isAuthenticated) setOpen(false);
  }, [isAuthenticated]);

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (wrapperRef.current?.contains(target) || panelRef.current?.contains(target)) return;
      setOpen(false);
    };
    const onViewportChange = () => setOpen(false);

    window.addEventListener('keydown', onKeyDown);
    document.addEventListener('pointerdown', onPointerDown);
    window.addEventListener('scroll', onViewportChange, { passive: true });
    window.addEventListener('resize', onViewportChange);

    return () => {
      window.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('pointerdown', onPointerDown);
      window.removeEventListener('scroll', onViewportChange);
      window.removeEventListener('resize', onViewportChange);
    };
  }, [open]);

  useEffect(() => () => window.clearTimeout(closeTimer.current), []);

  // Chỉ lấy những sản phẩm mới thêm gần đây (và vẫn còn trong giỏ), mới nhất lên đầu
  const recentItems = useMemo(() => {
    const byId = new Map(items.map((item) => [item.id, item]));
    return [...recent]
      .sort((a, b) => b.at - a.at)
      .map((entry) => byId.get(entry.id))
      .filter((item): item is CartItem => item !== undefined)
      .slice(0, MAX_ITEMS);
  }, [recent, items]);
  const remaining = Math.max(items.length - recentItems.length, 0);

  const goTo = (path: string) => {
    setOpen(false);
    navigate(path);
  };

  return (
    <>
      <div
        ref={wrapperRef}
        className="relative"
        onPointerEnter={handlePointerEnter}
        onPointerLeave={handlePointerLeave}
      >
        <button
          type="button"
          aria-label="Giỏ hàng"
          aria-haspopup="true"
          aria-expanded={open}
          onClick={handleTriggerClick}
          className={`flex h-9 w-9 items-center justify-center rounded-full transition-colors duration-200 hover:bg-white hover:text-[#171B7A] sm:h-10 sm:w-10 ${
            open ? 'bg-white text-[#171B7A]' : 'text-[#4B5563]'
          }`}
        >
          <ShoppingCart size={20} strokeWidth={1.8} />
        </button>

        {itemCount > 0 && (
          <span className="pointer-events-none absolute -right-0.5 -top-0.5 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-[#5B6FF5] px-1 text-[10px] font-semibold leading-none text-white ring-2 ring-white/70">
            {itemCount > 99 ? '99+' : itemCount}
          </span>
        )}
      </div>

      {createPortal(
        <AnimatePresence>
          {open && (
            <motion.div
              ref={panelRef}
              role="dialog"
              aria-label="Sản phẩm mới thêm vào giỏ hàng"
              className="fixed z-[70]"
              style={{ top: position.top, right: position.right, width: position.width }}
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.16, ease: 'easeOut' }}
              onPointerEnter={(event) => {
                if (event.pointerType === 'mouse') cancelClose();
              }}
              onPointerLeave={handlePointerLeave}
            >
              {/* Vùng đệm vô hình nối icon với panel để chuột không "rơi" khỏi hover */}
              <div className="absolute inset-x-0 -top-3 h-3" />

              {/* Mũi tên chỉ lên icon */}
              <span
                className="absolute -top-1.5 h-3 w-3 rotate-45 border-l border-t border-[#E8EDF5] bg-white"
                style={{ right: position.caretRight }}
              />

              <div className="relative overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white shadow-[0_12px_40px_rgba(61,82,120,0.18)]">
                {items.length === 0 ? (
                  <div className="flex flex-col items-center px-6 py-10 text-center">
                    <span className="mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-[#F3F6FC]">
                      <ShoppingCart size={30} strokeWidth={1.4} className="text-[#5367C9]" />
                    </span>
                    <p className="text-sm font-semibold text-[#252A3A]">
                      Chưa có sản phẩm nào
                    </p>
                    <p className="mt-1 text-xs text-[#667085]">
                      Hãy thêm sản phẩm bạn thích vào giỏ hàng nhé!
                    </p>
                    <button
                      type="button"
                      onClick={() => goTo('/products')}
                      className="mt-4 h-9 rounded-full bg-[#5367C9] px-5 text-sm font-semibold text-white transition-colors hover:bg-[#4659B8]"
                    >
                      Khám phá sản phẩm
                    </button>
                  </div>
                ) : (
                  <>
                    <p className="px-4 pb-1 pt-3.5 text-sm text-[#98A1B2]">
                      Sản phẩm mới thêm
                    </p>

                    {recentItems.length === 0 && (
                      <p className="px-4 py-6 text-center text-sm text-[#98A1B2]">
                        Chưa có sản phẩm nào thêm gần đây
                      </p>
                    )}

                    <ul>
                      {recentItems.map((item) => {
                        const variant = getVariantLabel(item);
                        return (
                          <li key={item.id}>
                            <button
                              type="button"
                              onClick={() => goTo(getProductPath(item))}
                              className="flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors hover:bg-[#F7FAFF]"
                            >
                              <img
                                src={item.imageUrl || PLACEHOLDER}
                                alt=""
                                className="h-10 w-10 shrink-0 rounded-lg border border-[#EEF1F6] bg-[#F3F6FC] object-cover"
                              />
                              <div className="min-w-0 flex-1">
                                <p className="truncate text-sm text-[#252A3A]">
                                  {item.productName}
                                </p>
                                <p className="truncate text-xs text-[#98A1B2]">
                                  {variant ? `${variant} · ` : ''}SL: {item.quantity}
                                </p>
                              </div>
                              <span className="ml-2 shrink-0 text-sm font-semibold text-[#5367C9]">
                                {formatVnd(item.price)}
                              </span>
                            </button>
                          </li>
                        );
                      })}
                    </ul>

                    <div className="flex items-center justify-between gap-3 border-t border-[#EEF1F6] bg-[#F7FAFF] px-4 py-3">
                      <span className="text-xs text-[#667085]">
                        {recentItems.length > 0 && remaining > 0
                          ? `${remaining} sản phẩm khác trong giỏ`
                          : `${items.length} sản phẩm trong giỏ`}
                      </span>
                      <button
                        type="button"
                        onClick={() => goTo('/cart')}
                        className="h-9 shrink-0 rounded-full bg-[#5367C9] px-5 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(83,103,201,0.25)] transition-colors hover:bg-[#4659B8]"
                      >
                        Xem giỏ hàng
                      </button>
                    </div>
                  </>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>,
        document.body
      )}
    </>
  );
};

export default MiniCart;