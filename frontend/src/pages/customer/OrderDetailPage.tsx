import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Clock3, MapPin, Package, ReceiptText, Truck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderDetail, OrderStatus } from '../../services/checkout/checkoutTypes';

const labels: Record<OrderStatus,string> = { PENDING:'Chờ xác nhận', CONFIRMED:'Đã xác nhận', SHIPPING:'Đang giao', DELIVERED:'Đã giao', CANCELLED:'Đã hủy' };
const colors: Record<OrderStatus,string> = { PENDING:'bg-amber-50 text-amber-700', CONFIRMED:'bg-blue-50 text-blue-700', SHIPPING:'bg-indigo-50 text-indigo-700', DELIVERED:'bg-emerald-50 text-emerald-700', CANCELLED:'bg-rose-50 text-rose-700' };
export default function OrderDetailPage() {
  const { orderId = '' } = useParams();
  const { isAuthenticated, isLoading } = useAuth();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const load = async () => { setLoading(true); try { setOrder(await checkoutApi.getOrder(orderId)); setError(''); } catch(e) { setError(getApiError(e,'Không thể tải chi tiết đơn hàng.')); } finally { setLoading(false); } };
  useEffect(() => { if(isAuthenticated && orderId) void load(); }, [isAuthenticated, orderId]);
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{from:`/orders/${orderId}`}}/>;
  const cancel = async (subOrderId:string) => {
    const input = window.prompt('Lý do hủy đơn (không bắt buộc):');
    if (input === null) return;
    const reason = input;
    setBusyId(subOrderId); setError('');
    try { await checkoutApi.cancelSubOrder(subOrderId,reason); await load(); }
    catch(e) { setError(getApiError(e,'Không thể hủy đơn hàng.')); }
    finally { setBusyId(''); }
  };
  const canConfirmCod = !!order && order.paymentMethod === 'COD' && order.paymentStatus === 'PENDING' &&
    order.subOrders.some(s=>s.status!=='CANCELLED') && order.subOrders.filter(s=>s.status!=='CANCELLED').every(s=>s.status==='DELIVERED');
  const confirmCod = async () => {
    if(!order) return;
    setBusyId(order.id); setError('');
    try { await checkoutApi.confirmCodReceived(order.id); await load(); }
    catch(e) { setError(getApiError(e,'Chưa đủ điều kiện xác nhận COD.')); }
    finally { setBusyId(''); }
  };
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-8 sm:px-5"><div className="mx-auto max-w-[1000px]">
    <Link to="/orders" className="inline-flex items-center gap-2 text-sm font-medium text-[#596BD0] hover:underline"><ArrowLeft size={16}/>Quay lại đơn mua</Link>
    {loading ? <div className="mt-5 rounded-2xl bg-white p-12 text-center text-sm text-[#697386]">Đang tải chi tiết đơn hàng...</div> : order ? <>
      <div className="mt-4 flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-[#6576D8]">Chi tiết đơn hàng</p><h1 className="mt-1 text-2xl font-bold text-[#252B40]">{order.orderCode}</h1><p className="mt-1 text-xs text-[#7A8498]">Đặt lúc {formatDateTime(order.createdAt)}</p></div><span className={`rounded-full px-3 py-1.5 text-xs font-semibold ${order.paymentStatus==='PAID'?'bg-emerald-50 text-emerald-700':'bg-amber-50 text-amber-700'}`}>{order.paymentStatus==='PAID'?'Đã thanh toán':order.paymentStatus==='PENDING'?'Chờ thanh toán':order.paymentStatus}</span></div>
      {error && <div role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}
      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_310px]">
        <div className="space-y-4">{order.subOrders.map(sub=><section key={sub.id} className="overflow-hidden rounded-2xl border border-[#E7EBF3] bg-white shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#EEF1F6] bg-[#FBFCFF] px-5 py-4"><div><div className="flex items-center gap-2 text-sm font-bold text-[#303A52]"><Package size={17} className="text-[#596BD0]"/>{sub.shopName ?? 'Cửa hàng'}</div><p className="mt-1 text-[11px] text-[#8A93A6]">Mã đơn con: {sub.id}</p></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${colors[sub.status]}`}>{labels[sub.status]}</span></div>
          <div className="divide-y divide-[#EEF1F6]">{sub.items.map(item=><div key={item.id} className="flex items-start justify-between gap-4 px-5 py-3"><div className="min-w-0"><p className="text-sm font-medium text-[#35405A]">{item.productName}</p><p className="mt-1 text-xs text-[#7A8498]">{formatMoney(item.price)} × {item.quantity}</p></div><div className="shrink-0 text-right text-sm font-semibold text-[#35405A]">{formatMoney(item.totalAmount)}<p className="mt-1 text-[11px] font-normal text-emerald-700">Giảm {formatMoney(item.discountAmount)}</p></div></div>)}</div>
          {sub.shippingAddress && <div className="border-t border-[#EEF1F6] px-5 py-4"><div className="flex items-center gap-2 text-xs font-bold text-[#46516A]"><MapPin size={15}/>Địa chỉ nhận hàng (snapshot)</div><p className="mt-2 text-xs leading-5 text-[#68738A]">{sub.shippingAddress.recipientName} · {sub.shippingAddress.recipientPhone}<br/>{sub.shippingAddress.addressDetail}, {sub.shippingAddress.ward}, {sub.shippingAddress.recipientCity}</p></div>}
          <div className="border-t border-[#EEF1F6] px-5 py-4"><div className="flex items-center gap-2 text-xs font-bold text-[#46516A]"><Clock3 size={15}/>Lịch sử trạng thái</div>{sub.statusHistory.length ? <ol className="mt-3 space-y-3">{sub.statusHistory.map((h,i)=><li key={`${h.createdAt}-${i}`} className="relative flex gap-3"><span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#6678D8]"/><div className="min-w-0"><p className="text-xs font-semibold text-[#46516A]">{h.oldStatus ? `${labels[h.oldStatus]} → ` : ''}{labels[h.newStatus]}</p><p className="mt-0.5 text-[11px] text-[#8A93A6]">{formatDateTime(h.createdAt)}{h.note ? ` · ${h.note}` : ''}</p></div></li>)}</ol> : <p className="mt-2 text-xs text-[#8A93A6]">Chưa có lịch sử cập nhật.</p>}</div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#EEF1F6] bg-[#FBFCFF] px-5 py-3"><div className="text-xs text-[#778197]">Tạm tính {formatMoney(sub.subtotal)} · Giảm {formatMoney(sub.discountAmount)}</div><div className="text-sm font-bold text-[#596BD0]">Thành tiền {formatMoney(sub.totalAmount)}</div></div>
          {sub.status==='PENDING' && <div className="flex justify-end border-t border-[#EEF1F6] px-5 py-3"><button disabled={!!busyId} onClick={()=>void cancel(sub.id)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-50">{busyId===sub.id?'Đang hủy...':'Hủy đơn shop'}</button></div>}
        </section>)}</div>
        <aside className="h-fit rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm lg:sticky lg:top-24"><div className="flex items-center gap-2"><ReceiptText size={18} className="text-[#596BD0]"/><h2 className="font-bold text-[#303A52]">Tổng thanh toán</h2></div><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between text-[#778197]"><dt>Tạm tính</dt><dd>{formatMoney(order.subtotal)}</dd></div><div className="flex justify-between text-emerald-700"><dt>Tổng giảm giá</dt><dd>-{formatMoney(order.discountAmount)}</dd></div><div className="flex justify-between border-t border-[#EEF1F6] pt-3 font-bold text-[#303A52]"><dt>Thành tiền</dt><dd className="text-lg text-[#596BD0]">{formatMoney(order.totalAmount)}</dd></div></dl><div className="mt-4 rounded-xl bg-[#F8FAFD] p-3 text-xs text-[#697386]"><p>Phương thức: <strong className="text-[#35405A]">{order.paymentMethod ?? '—'}</strong></p><p className="mt-1">Thanh toán: <strong className="text-[#35405A]">{order.paymentStatus==='PAID'?'Đã thanh toán':order.paymentStatus==='PENDING'?'Chờ thanh toán':order.paymentStatus}</strong></p></div>
          {canConfirmCod && <button onClick={()=>void confirmCod()} disabled={!!busyId} className="mt-4 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-[#596BD0] text-sm font-semibold text-white hover:bg-[#4859C2] disabled:opacity-50">{busyId===order.id?'Đang xác nhận...':<><CheckCircle2 size={16}/>Xác nhận đã nhận hàng & thanh toán</>}</button>}
          {order.paymentMethod==='COD' && order.paymentStatus==='PENDING' && !canConfirmCod && <p className="mt-3 flex gap-2 rounded-lg bg-amber-50 p-3 text-[11px] leading-5 text-amber-800"><Truck size={15} className="shrink-0"/>Chỉ có thể xác nhận COD sau khi tất cả đơn shop chưa hủy đã giao thành công. Shop sẽ ghi nhận đã thu tiền khi hoàn tất giao hàng.</p>}
        </aside>
      </div>
    </> : <div className="mt-5 rounded-2xl bg-white p-10 text-center text-sm text-[#697386]">{error || 'Không tìm thấy đơn hàng.'}</div>}
  </div></main>;
}
