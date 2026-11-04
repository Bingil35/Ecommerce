import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ClipboardList, MapPin, Package, Search, ShoppingBag } from 'lucide-react';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderStatus, ShopOwnerOrder } from '../../services/checkout/checkoutTypes';

const labels: Record<OrderStatus, string> = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang vận chuyển', DELIVERED: 'Hoàn thành', CANCELLED: 'Đã hủy' };
const nextStatus: Partial<Record<OrderStatus, OrderStatus>> = { PENDING: 'CONFIRMED' };
const statusStyles: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-50 text-amber-700 ring-amber-200', CONFIRMED: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  SHIPPING: 'bg-sky-50 text-sky-700 ring-sky-200', DELIVERED: 'bg-emerald-50 text-emerald-700 ring-emerald-200', CANCELLED: 'bg-slate-100 text-slate-600 ring-slate-200',
};

export default function ShopOwnerOrdersPage() {
  const [items, setItems] = useState<ShopOwnerOrder[]>([]);
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [notes, setNotes] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    try { setError(''); const result = await checkoutApi.getShopOrders({ page, size: 10, status, keyword: query }); setItems(result.items); setPages(result.totalPages); }
    catch (e) { setError(getApiError(e)); }
  }, [page, status, query]);
  useEffect(() => { void load(); }, [load]);

  const advance = async (order: ShopOwnerOrder) => {
    const next = nextStatus[order.status]; if (!next) return;
    try { setBusy(order.id); await checkoutApi.updateShopSubOrderStatus(order.id, next, notes[order.id] || ''); setNotes(current => ({ ...current, [order.id]: '' })); await load(); }
    catch (e) { setError(getApiError(e)); }
    finally { setBusy(null); }
  };

  return <div className="seller-container shop-management !max-w-[1440px]">
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
      <div className="flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-[#5367C9]"><ClipboardList size={23}/></span><div><h1>Quản lý đơn hàng</h1><p className="mt-1">Theo dõi đơn và cập nhật trạng thái xử lý.</p></div></div>
      <div className="flex items-center gap-2 rounded-xl border border-[#e4e8f1] bg-white px-4 py-3 shadow-sm"><span className="grid h-9 w-9 place-items-center rounded-xl bg-violet-50 text-violet-600"><ShoppingBag size={18}/></span><div><span className="block text-[11px] font-semibold uppercase tracking-wide text-slate-500">Đơn trên trang</span><strong className="text-lg text-[#121b3b]">{items.length}</strong></div></div>
    </header>

    <form className="mb-5 flex flex-wrap items-center gap-3 rounded-2xl border bg-white p-3.5" onSubmit={e => { e.preventDefault(); setPage(0); setQuery(keyword.trim()); }}>
      <label className="flex min-w-56 flex-1 items-center gap-2 rounded-xl border px-3 text-slate-400"><Search size={17}/><input className="w-full border-0 py-2.5 text-sm outline-none focus:!border-0 focus:!shadow-none" placeholder="Tìm theo mã đơn hoặc tên khách" value={keyword} onChange={e => setKeyword(e.target.value)}/></label>
      <select className="min-w-44 px-3 py-2.5 text-sm" value={status} onChange={e => { setStatus(e.target.value as OrderStatus | ''); setPage(0); }}><option value="">Tất cả trạng thái</option>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
      <button className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-bold text-white"><Search size={16}/>Tìm đơn</button>
    </form>

    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}

    <div className="overflow-x-auto bg-white">
      <table className="w-full min-w-[1040px] text-left text-sm">
        <thead><tr><th className="px-5 py-4">Đơn hàng</th><th className="px-4 py-4">Khách hàng</th><th className="px-4 py-4">Sản phẩm</th><th className="px-4 py-4">Giao tới</th><th className="px-4 py-4">Tổng tiền</th><th className="px-4 py-4">Trạng thái</th><th className="px-5 py-4 text-right">Cập nhật</th></tr></thead>
        <tbody>{items.map(order => {
          const next = nextStatus[order.status];
          return <tr key={order.id} className="border-t border-slate-100 align-top">
            <td className="px-5 py-4"><Link to={`/shop-owner/orders/${order.id}`} className="group block"><b className="block font-bold text-[#17213f] group-hover:text-indigo-600">{order.orderCode}</b><span className="mt-1 block text-xs text-slate-500">{formatDateTime(order.createdAt)}</span><span className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-indigo-600">Xem chi tiết <ArrowRight size={13}/></span></Link></td>
            <td className="px-4 py-4"><span className="font-semibold text-slate-700">{order.customerName || 'Khách hàng'}</span></td>
            <td className="px-4 py-4"><div className="flex items-start gap-2"><Package size={16} className="mt-0.5 shrink-0 text-[#5367C9]"/><div><b>{order.items.length} mặt hàng</b><span className="mt-1 block max-w-48 truncate text-xs text-slate-500">{order.items.map(line => line.productName).join(', ')}</span></div></div></td>
            <td className="max-w-56 px-4 py-4"><div className="flex items-start gap-1.5 text-xs leading-5 text-slate-600"><MapPin size={14} className="mt-0.5 shrink-0 text-slate-400"/><span>{order.shippingAddress ? `${order.shippingAddress.recipientName} · ${order.shippingAddress.addressDetail}, ${order.shippingAddress.ward}, ${order.shippingAddress.recipientCity}` : 'Chưa có địa chỉ'}</span></div></td>
            <td className="whitespace-nowrap px-4 py-4 font-bold text-[#17213f]">{formatMoney(order.totalAmount)}</td>
            <td className="px-4 py-4"><span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${statusStyles[order.status]}`}>{labels[order.status]}</span></td>
            <td className="min-w-56 px-5 py-3 text-right">{next ? <div className="flex flex-col items-end gap-2"><input aria-label="Ghi chú trạng thái" className="w-full px-2.5 py-1.5 text-xs" placeholder="Ghi chú (không bắt buộc)" value={notes[order.id] || ''} onChange={e => setNotes(value => ({ ...value, [order.id]: e.target.value }))}/><button disabled={busy === order.id} onClick={() => void advance(order)} className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-50">{busy === order.id ? 'Đang lưu...' : 'Xác nhận & chuyển vận chuyển'}<ArrowRight size={14}/></button></div> : <span className="text-xs text-slate-400">{order.status === 'SHIPPING' ? 'Chờ khách xác nhận đã nhận hàng' : 'Không cần thao tác'}</span>}</td>
          </tr>;
        })}{items.length === 0 && <tr><td colSpan={7} className="shop-empty">Không có đơn hàng phù hợp với bộ lọc.</td></tr>}</tbody>
      </table>
    </div>

    <footer className="mt-4 flex items-center justify-between rounded-xl border border-[#e4e8f1] bg-white px-4 py-3 text-sm text-slate-500"><span>Trang {pages === 0 ? 0 : page + 1} / {pages}</span><div className="flex gap-2"><button className="rounded-lg border px-3 py-2 disabled:opacity-40" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Trước</button><button className="rounded-lg border px-3 py-2 disabled:opacity-40" disabled={page + 1 >= pages} onClick={() => setPage(value => value + 1)}>Sau</button></div></footer>
  </div>;
}
