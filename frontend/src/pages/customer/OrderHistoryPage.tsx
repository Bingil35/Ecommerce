import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router-dom';
import { CalendarDays, ChevronLeft, ChevronRight, Package, Search } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderListItem, OrderStatus, PageResponse } from '../../services/checkout/checkoutTypes';

const statuses: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' }, { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' }, { value: 'SHIPPING', label: 'Đang vận chuyển' },
  { value: 'DELIVERED', label: 'Hoàn thành' }, { value: 'CANCELLED', label: 'Đã hủy' },
];
export default function OrderHistoryPage() {
  const { isAuthenticated, isLoading } = useAuth();
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState<PageResponse<OrderListItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [code, setCode] = useState(params.get('orderCode') ?? '');
  const [status, setStatus] = useState(params.get('status') ?? '');
  const [from, setFrom] = useState(params.get('from') ?? '');
  const [to, setTo] = useState(params.get('to') ?? '');
  const page = Number(params.get('page') ?? 0);
  const load = async () => {
    setLoading(true); setError('');
    try { setData(await checkoutApi.getOrders({ page, size: 8, status: (params.get('status') ?? '') as OrderStatus | '', orderCode: params.get('orderCode') ?? '', from: params.get('from') ? `${params.get('from')}T00:00:00+07:00` : undefined, to: params.get('to') ? `${params.get('to')}T23:59:59+07:00` : undefined })); }
    catch(e) { setError(getApiError(e, 'Không thể tải lịch sử đơn hàng.')); }
    finally { setLoading(false); }
  };
  useEffect(() => { if (isAuthenticated) void load(); }, [isAuthenticated, params]);
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: '/orders' }}/>;
  const filter = (e: FormEvent) => { e.preventDefault(); const next = new URLSearchParams(); if(status) next.set('status',status); if(code.trim()) next.set('orderCode',code.trim()); if(from) next.set('from',from); if(to) next.set('to',to); next.set('page','0'); setParams(next); };
  const goPage = (n:number) => { const next=new URLSearchParams(params); next.set('page',String(n)); setParams(next); };
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-8 sm:px-5"><div className="mx-auto max-w-[1050px]">
    <div className="mb-6"><p className="text-xs font-semibold uppercase tracking-[.15em] text-[#6576D8]">Tài khoản Kala</p><h1 className="mt-2 text-2xl font-bold text-[#252B40] sm:text-3xl">Đơn mua</h1><p className="mt-1 text-sm text-[#7A8498]">Theo dõi trạng thái, thanh toán và chi tiết các đơn hàng của bạn.</p></div>
    <form onSubmit={filter} className="mb-5 grid gap-3 rounded-2xl border border-[#E7EBF3] bg-white p-4 shadow-sm sm:grid-cols-2 lg:grid-cols-5">
      <label className="text-xs font-semibold text-[#697386] lg:col-span-2">Mã đơn hàng<div className="relative mt-1.5"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#9AA3B4]"/><input value={code} onChange={e=>setCode(e.target.value)} placeholder="Nhập mã đơn..." className="h-10 w-full rounded-lg border border-[#E2E8F2] pl-9 pr-3 text-sm outline-none focus:border-[#6576D8]"/></div></label>
      <label className="text-xs font-semibold text-[#697386]">Trạng thái<select value={status} onChange={e=>setStatus(e.target.value)} className="mt-1.5 h-10 w-full rounded-lg border border-[#E2E8F2] px-2 text-sm">{statuses.map(s=><option key={s.value} value={s.value}>{s.label}</option>)}</select></label>
      <label className="text-xs font-semibold text-[#697386]">Từ ngày<input type="date" value={from} onChange={e=>setFrom(e.target.value)} className="mt-1.5 h-10 w-full rounded-lg border border-[#E2E8F2] px-2 text-sm"/></label>
      <label className="text-xs font-semibold text-[#697386]">Đến ngày<input type="date" min={from || undefined} value={to} onChange={e=>setTo(e.target.value)} className="mt-1.5 h-10 w-full rounded-lg border border-[#E2E8F2] px-2 text-sm"/></label>
      <div className="flex gap-2 sm:col-span-2 lg:col-span-5"><button className="h-10 rounded-lg bg-[#596BD0] px-5 text-sm font-semibold text-white">Lọc đơn hàng</button><button type="button" onClick={()=>{setCode('');setStatus('');setFrom('');setTo('');setParams({page:'0'});}} className="h-10 rounded-lg border border-[#E2E8F2] px-4 text-sm font-medium text-[#697386]">Xóa bộ lọc</button></div>
    </form>
    {error && <div className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</div>}
    {loading ? <div className="rounded-2xl bg-white p-12 text-center text-sm text-[#697386]">Đang tải đơn hàng...</div> : data?.items.length ? <div className="space-y-3">{data.items.map(order=><article key={order.id} className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#EEF1F6] pb-3"><div><div className="flex items-center gap-2 text-sm font-bold text-[#2A344D]"><Package size={17} className="text-[#596BD0]"/>Đơn {order.orderCode}</div><p className="mt-1 flex items-center gap-1.5 text-xs text-[#818A9D]"><CalendarDays size={13}/>{formatDateTime(order.createdAt)}</p></div><span className="rounded-full bg-[#F2F4F8] px-3 py-1 text-xs font-semibold text-[#697386]">{order.subOrderCount} đơn shop</span></div><div className="mt-4 flex flex-wrap items-end justify-between gap-3"><div className="space-y-1 text-xs text-[#778197]"><p>Thanh toán: <strong className="text-[#46516A]">{order.paymentMethod ?? '—'} · {order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'PENDING' ? 'Chờ thanh toán' : order.paymentStatus}</strong></p><p>Tạm tính {formatMoney(order.subtotal)} · Giảm {formatMoney(order.discountAmount)}</p></div><div className="text-right"><p className="text-xs text-[#818A9D]">Tổng thanh toán</p><p className="text-lg font-bold text-[#596BD0]">{formatMoney(order.totalAmount)}</p></div></div><div className="mt-4 flex justify-end"><Link to={`/orders/${order.id}`} className="inline-flex h-9 items-center gap-1 rounded-lg border border-[#DCE3F4] px-4 text-xs font-semibold text-[#596BD0] hover:bg-[#F4F6FF]">Xem chi tiết <ChevronRight size={15}/></Link></div></article>)}</div> : <div className="rounded-2xl border border-[#E7EBF3] bg-white px-6 py-16 text-center"><Package size={30} className="mx-auto text-[#A7B0C1]"/><p className="mt-3 font-semibold text-[#35405A]">Chưa tìm thấy đơn hàng</p><p className="mt-1 text-sm text-[#7A8498]">Thử thay đổi bộ lọc hoặc tiếp tục mua sắm.</p><Link to="/products" className="mt-4 inline-flex rounded-xl bg-[#596BD0] px-5 py-2.5 text-sm font-semibold text-white">Khám phá sản phẩm</Link></div>}
    {data && data.totalPages > 1 && <div className="mt-5 flex items-center justify-between rounded-xl border border-[#E7EBF3] bg-white px-4 py-3"><span className="text-xs text-[#778197]">Trang {data.page+1}/{data.totalPages} · {data.totalElements} đơn hàng</span><div className="flex gap-2"><button disabled={data.page===0} onClick={()=>goPage(data.page-1)} className="rounded-lg border p-2 text-[#596BD0] disabled:opacity-30"><ChevronLeft size={17}/></button><button disabled={data.last} onClick={()=>goPage(data.page+1)} className="rounded-lg border p-2 text-[#596BD0] disabled:opacity-30"><ChevronRight size={17}/></button></div></div>}
  </div></main>;
}
