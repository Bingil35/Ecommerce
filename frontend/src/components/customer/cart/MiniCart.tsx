import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Minus, Plus, ShoppingCart, Trash2 } from 'lucide-react';
import { useAuth } from '../../../context/AuthContext';
import { useCart } from '../../../context/CartContext';
import { mediaUrl } from '../../../services/api/mediaUrl';
import { formatVnd } from '../../../services/catalog/productApi';
import { getProductPath } from '../../../services/cart/cartUtils';

const MiniCart = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const { items, distinctItemCount, isLoading, error, refresh, updateQuantity, removeFromCart } = useCart();
  const [open, setOpen] = useState(false);
  const wrapper = useRef<HTMLDivElement>(null);
  const closeTimer = useRef<number | undefined>(undefined);

  const closeSoon = () => { window.clearTimeout(closeTimer.current); closeTimer.current = window.setTimeout(() => setOpen(false), 180); };
  const openNow = () => { if (location.pathname === '/cart') return; window.clearTimeout(closeTimer.current); setOpen(true); };
  useEffect(() => () => window.clearTimeout(closeTimer.current), []);
  useEffect(() => { setOpen(false); }, [location.pathname]);
  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => { if (!wrapper.current?.contains(event.target as Node)) setOpen(false); };
    const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false); };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => { document.removeEventListener('pointerdown', onPointerDown); document.removeEventListener('keydown', onKeyDown); };
  }, [open]);
  const goTo = (path: string) => { setOpen(false); navigate(path); };
  const latestItems = [...items].sort((a, b) => (Date.parse(b.createdAt || '') || 0) - (Date.parse(a.createdAt || '') || 0)).slice(0, 3);

  return <div ref={wrapper} className="relative" onPointerEnter={event => { if (event.pointerType === 'mouse') openNow(); }} onPointerLeave={event => { if (event.pointerType === 'mouse') closeSoon(); }}>
    <button type="button" aria-label="Đi đến giỏ hàng" aria-expanded={open} aria-controls="mini-cart-panel" onClick={() => goTo('/cart')} className="relative flex h-9 w-9 items-center justify-center rounded-full text-[#4B5563] transition-colors hover:bg-white hover:text-[#171B7A] sm:h-10 sm:w-10">
      <ShoppingCart size={20} strokeWidth={1.8}/>
      {distinctItemCount > 0 && <span className="absolute -right-0.5 -top-0.5 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-[#5B6FF5] px-1 text-[10px] font-semibold leading-none text-white ring-2 ring-white/70">{distinctItemCount > 99 ? '99+' : distinctItemCount}</span>}
    </button>
    {open && location.pathname !== '/cart' && <div id="mini-cart-panel" className="absolute right-0 top-full z-[70] w-[min(400px,calc(100vw-24px))] pt-2" aria-label="Giỏ hàng">
      <span aria-hidden="true" className="absolute right-3 top-[3px] z-10 h-3 w-3 rotate-45 border-l border-t border-[#E8EDF5] bg-white sm:right-3.5" />
      <div className="overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white shadow-[0_12px_40px_rgba(61,82,120,0.18)]">
        <div className="border-b border-[#EEF1F6] px-4 py-3 text-sm font-semibold text-[#252A3A]">Giỏ hàng của bạn</div>
        {!isAuthenticated ? <div className="px-5 py-8 text-center text-sm text-[#667085]"><p>Đăng nhập để xem giỏ hàng.</p><button type="button" onClick={() => { setOpen(false); navigate('/login', { state: { from: location.pathname, notice: 'Đăng nhập để sử dụng giỏ hàng.' } }); }} className="mt-4 rounded-full bg-[#5367C9] px-5 py-2 font-semibold text-white">Đăng nhập</button></div>
          : error ? <div role="alert" className="px-5 py-6 text-center text-sm text-[#B42318]"><p>{error}</p><button type="button" onClick={() => void refresh()} className="mt-2 text-[#5367C9] underline">Thử lại</button></div>
          : isLoading && items.length === 0 ? <p className="px-5 py-8 text-center text-sm text-[#667085]">Đang tải giỏ hàng...</p>
          : items.length === 0 ? <div className="px-5 py-8 text-center text-sm text-[#667085]"><p>Chưa có sản phẩm nào trong giỏ hàng.</p><button type="button" onClick={() => goTo('/products')} className="mt-4 rounded-full bg-[#5367C9] px-5 py-2 font-semibold text-white">Khám phá sản phẩm</button></div>
          : <><ul className="max-h-[min(55vh,420px)] overflow-y-auto overscroll-contain">{latestItems.map(item => {
              const variant = item.selectedOptions?.length ? item.selectedOptions.map(option => `${option.optionName}: ${option.value}`).join(' · ') : item.variantName;
              const cannotIncrease = item.available === false || (item.stockQuantity != null && item.quantity >= item.stockQuantity);
              return <li key={item.id} className="flex gap-3 border-b border-[#F0F2F7] px-4 py-3 last:border-b-0">
                <button type="button" onClick={() => goTo(getProductPath(item))} aria-label={`Xem ${item.productName}`} className="h-14 w-14 shrink-0 overflow-hidden rounded-lg bg-[#F3F6FC]">{item.imageUrl ? <img src={mediaUrl(item.imageUrl)} alt="" className="h-full w-full object-cover"/> : <ShoppingCart className="mx-auto text-[#A1A9BC]" size={22}/>}</button>
                <div className="min-w-0 flex-1"><div className="flex items-start gap-2"><button type="button" onClick={() => goTo(getProductPath(item))} className="min-w-0 flex-1 truncate text-left text-sm font-semibold text-[#252A3A] hover:text-[#5367C9]">{item.productName}</button><button type="button" disabled={isLoading} onClick={() => void removeFromCart(item.id)} aria-label={`Xóa ${item.productName}`} className="shrink-0 text-[#98A1B2] hover:text-[#DC5A5A] disabled:opacity-40"><Trash2 size={16}/></button></div>
                  {variant && <p className="mt-1 truncate text-xs text-[#98A1B2]">{variant}</p>}
                  {item.available === false && <p className="mt-1 text-xs text-[#DC5A5A]">Sản phẩm hiện không khả dụng</p>}
                  <div className="mt-2 flex items-center justify-between gap-2"><span className="text-sm font-semibold text-[#5367C9]">{formatVnd(item.price)}</span><div className="flex items-center rounded-lg border border-[#E8EDF5] text-[#48536A]"><button type="button" disabled={isLoading || item.quantity <= 1} onClick={() => void updateQuantity(item.id, item.quantity - 1)} aria-label={`Giảm số lượng ${item.productName}`} className="p-1.5 disabled:opacity-40"><Minus size={14}/></button><span className="min-w-7 text-center text-sm" aria-label={`Số lượng ${item.productName}`}>{item.quantity}</span><button type="button" disabled={isLoading || cannotIncrease} onClick={() => void updateQuantity(item.id, item.quantity + 1)} aria-label={`Tăng số lượng ${item.productName}`} className="p-1.5 disabled:opacity-40"><Plus size={14}/></button></div></div>
                </div></li>;
            })}</ul><div className="border-t border-[#EEF1F6] bg-[#F7FAFF] px-4 py-3"><button type="button" onClick={() => goTo('/cart')} className="w-full rounded-full bg-[#5367C9] px-5 py-2 text-sm font-semibold text-white hover:bg-[#4659B8]">Xem giỏ hàng</button></div></>}
      </div>
    </div>}
  </div>;
};

export default MiniCart;
