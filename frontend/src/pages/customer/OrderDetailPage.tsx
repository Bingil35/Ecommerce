import { useDialog, useToast } from '../../context/FeedbackContext';
import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Clock3, MapPin, Package, ReceiptText, Truck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderDetail, OrderStatus } from '../../services/checkout/checkoutTypes';
import { reviewApi } from '../../services/reviews/reviewApi';

const labels: Record<OrderStatus,string> = { PENDING:'Chờ xác nhận', CONFIRMED:'Đã xác nhận', SHIPPING:'Đang vận chuyển', DELIVERED:'Hoàn thành', CANCELLED:'Đã hủy' };
const colors: Record<OrderStatus,string> = { PENDING:'bg-amber-50 text-amber-700', CONFIRMED:'bg-blue-50 text-blue-700', SHIPPING:'bg-indigo-50 text-indigo-700', DELIVERED:'bg-emerald-50 text-emerald-700', CANCELLED:'bg-rose-50 text-rose-700' };
const vietnameseHistoryNote = (note: string) => ({
  'Order created.': 'Đơn hàng đã được tạo.',
  'Shop confirmed the order; shipment is in progress.': 'Cửa hàng đã xác nhận đơn và đang chuẩn bị giao hàng.',
  'Customer cancelled the order.': 'Khách hàng đã hủy đơn hàng.',
  'Customer confirmed receipt.': 'Khách hàng đã xác nhận nhận hàng.',
} as Record<string, string>)[note] ?? note;
export default function OrderDetailPage() {
  const toast=useToast();const {prompt}=useDialog();
  const { orderId = '' } = useParams();
  const location = useLocation();
  const isSubOrderRoute = location.pathname.includes('/sub-orders/');
  const { isAuthenticated, isLoading } = useAuth();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [reviewDrafts, setReviewDrafts] = useState<Record<string,{rating:number;content:string}>>({});
  const [reviewed, setReviewed] = useState<string[]>([]);
  const load = async () => { setLoading(true); try { setOrder(isSubOrderRoute ? await checkoutApi.getSubOrder(orderId) : await checkoutApi.getOrder(orderId)); setError(''); } catch(e) { setError(getApiError(e,'Không thể tải chi tiết đơn hàng.')); } finally { setLoading(false); } };
  useEffect(() => { if(isAuthenticated && orderId) void load(); }, [isAuthenticated, orderId, isSubOrderRoute]);
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{from:`/orders/${orderId}`}}/>;
  const cancel = (subOrderId:string)=>prompt({title:'Hủy đơn hàng?',message:'Bạn có chắc chắn muốn hủy đơn hàng này?',confirmText:'Hủy đơn hàng',cancelText:'Giữ đơn hàng',variant:'danger',input:{label:'Lý do hủy (không bắt buộc)',maxLength:500},loadingText:'Đang hủy đơn...',successMessage:'Đã hủy đơn hàng.',onConfirm:async reason=>{setBusyId(subOrderId);setError('');try{await checkoutApi.cancelSubOrder(subOrderId,reason);await load();}finally{setBusyId('');}}});
  const canConfirmCod = !!order && order.paymentMethod === 'COD' && order.paymentStatus === 'PENDING' &&
    order.subOrders.some(s=>s.status!=='CANCELLED') && order.subOrders.filter(s=>s.status!=='CANCELLED').every(s=>s.status==='DELIVERED');
  const confirmCod = async () => {
    if(!order) return;
    setBusyId(order.id); setError('');
    try { await checkoutApi.confirmCodReceived(order.id); toast.success('Đã xác nhận thanh toán COD.'); await load(); }
    catch(e) { setError(getApiError(e,'Chưa đủ điều kiện xác nhận COD.')); toast.error(getApiError(e)); }
    finally { setBusyId(''); }
  };
  const confirmReceived = async (subOrderId: string) => {
    setBusyId(subOrderId); setError('');
    try { await checkoutApi.confirmSubOrderReceived(subOrderId); toast.success('Đã xác nhận nhận hàng.'); await load(); }
    catch(e) { setError(getApiError(e, 'Không thể xác nhận đã nhận hàng.')); toast.error(getApiError(e)); }
    finally { setBusyId(''); }
  };
  const submitReview = async (subOrderId:string, productId:string, variantId:string|null) => {
    const draft=reviewDrafts[productId]||{rating:5,content:''}; setBusyId(`review-${productId}`); setError('');
    try { await reviewApi.create(productId,{...draft,subOrderId,variantId:variantId||undefined}); setReviewed(v=>[...v,productId]); toast.success('Đã gửi đánh giá sản phẩm.'); }
    catch(e) { setError(getApiError(e,'Không thể gửi đánh giá.')); toast.error(getApiError(e)); }
    finally { setBusyId(''); }
  };
  const visibleSubOrders = order ? (isSubOrderRoute ? order.subOrders.filter(sub => sub.id === orderId) : order.subOrders) : [];
  const displayedSubtotal = isSubOrderRoute ? visibleSubOrders.reduce((sum, sub) => sum + Number(sub.subtotal), 0) : Number(order?.subtotal ?? 0);
  const displayedDiscount = isSubOrderRoute ? visibleSubOrders.reduce((sum, sub) => sum + Number(sub.discountAmount), 0) : Number(order?.discountAmount ?? 0);
  const displayedTotal = isSubOrderRoute ? visibleSubOrders.reduce((sum, sub) => sum + Number(sub.totalAmount), 0) : Number(order?.totalAmount ?? 0);
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-8 sm:px-5"><div className="mx-auto max-w-[1000px]">
    <Link to="/orders" className="inline-flex items-center gap-2 text-sm font-medium text-[#596BD0] hover:underline"><ArrowLeft size={16}/>Quay lại đơn mua</Link>
    {loading ? <div className="mt-5 rounded-2xl bg-white p-12 text-center text-sm text-[#697386]">Đang tải chi tiết đơn hàng...</div> : order ? <>
      <div className="mt-4 flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-[#6576D8]">Chi tiết đơn hàng</p><h1 className="mt-1 text-2xl font-bold text-[#252B40]">{order.orderCode}</h1><p className="mt-1 text-xs text-[#7A8498]">Đặt lúc {formatDateTime(order.createdAt)}</p></div><span className={`rounded-full px-3 py-1.5 text-xs font-semibold ${order.paymentStatus==='PAID'?'bg-emerald-50 text-emerald-700':'bg-amber-50 text-amber-700'}`}>{order.paymentStatus==='PAID'?'Đã thanh toán':order.paymentStatus==='PENDING'?'Chờ thanh toán':order.paymentStatus}</span></div>
      {error && <div role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}
      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_310px]">
        <div className="space-y-4">{visibleSubOrders.map(sub=><section key={sub.id} className="overflow-hidden rounded-2xl border border-[#E7EBF3] bg-white shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#EEF1F6] bg-[#FBFCFF] px-5 py-4"><div><div className="flex items-center gap-2 text-sm font-bold text-[#303A52]"><Package size={17} className="text-[#596BD0]"/>{sub.shopName ?? 'Cửa hàng'}</div><p className="mt-1 text-[11px] text-[#8A93A6]">Mã đơn con: {sub.id}</p></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${colors[sub.status]}`}>{labels[sub.status]}</span></div>
          <div className="divide-y divide-[#EEF1F6]">{sub.items.map(item=><div key={item.id} className="px-5 py-3"><div className="flex items-start justify-between gap-4"><div className="flex min-w-0 gap-3"><img src={item.imageUrl || '/placeholder-product.png'} alt={item.productName} className="h-16 w-16 shrink-0 rounded-lg border border-[#EEF1F6] bg-[#F8FAFD] object-cover"/><div className="min-w-0"><p className="text-sm font-medium text-[#35405A]">{item.productName}</p>{item.variantOptions?.length > 0 && <p className="mt-1 text-xs text-[#7A8498]">Phân loại: {item.variantOptions.join(' · ')}</p>}<p className="mt-1 text-xs text-[#7A8498]">{formatMoney(item.price)} × {item.quantity}</p></div></div><div className="shrink-0 text-right text-sm font-semibold text-[#35405A]">{formatMoney(item.totalAmount)}<p className="mt-1 text-[11px] font-normal text-emerald-700">Giảm {formatMoney(item.discountAmount)}</p></div></div>
            {sub.status==='DELIVERED'&&item.productId&&(reviewed.includes(item.productId)?<p className="mt-3 text-xs font-semibold text-emerald-700">Đã gửi đánh giá sản phẩm này.</p>:<div className="mt-3 flex flex-col gap-2 rounded-lg bg-slate-50 p-3 sm:flex-row sm:items-end"><label className="text-xs font-semibold">Số sao<select className="mt-1 block rounded border bg-white px-2 py-2" value={reviewDrafts[item.productId]?.rating??5} onChange={e=>setReviewDrafts(d=>({...d,[item.productId!]:{rating:Number(e.target.value),content:d[item.productId!]?.content??''}}))}>{[5,4,3,2,1].map(n=><option key={n} value={n}>{n} sao</option>)}</select></label><textarea className="min-h-10 flex-1 rounded border p-2 text-xs" placeholder="Chia sẻ trải nghiệm của bạn" value={reviewDrafts[item.productId]?.content??''} onChange={e=>setReviewDrafts(d=>({...d,[item.productId!]:{rating:d[item.productId!]?.rating??5,content:e.target.value}}))}/><button disabled={busyId===`review-${item.productId}`} onClick={()=>void submitReview(sub.id,item.productId!,item.variantId)} className="rounded-lg bg-[#596BD0] px-3 py-2 text-xs font-semibold text-white disabled:opacity-50">{busyId===`review-${item.productId}`?'Đang gửi...':'Gửi đánh giá'}</button></div>)}
          </div>)}</div>
          {sub.shippingAddress && <div className="border-t border-[#EEF1F6] px-5 py-4"><div className="flex items-center gap-2 text-xs font-bold text-[#46516A]"><MapPin size={15}/>Địa chỉ nhận hàng (snapshot)</div><p className="mt-2 text-xs leading-5 text-[#68738A]">{sub.shippingAddress.recipientName} · {sub.shippingAddress.recipientPhone}<br/>{sub.shippingAddress.addressDetail}, {sub.shippingAddress.ward}, {sub.shippingAddress.recipientCity}</p></div>}
          <div className="border-t border-[#EEF1F6] px-5 py-4"><div className="flex items-center gap-2 text-xs font-bold text-[#46516A]"><Clock3 size={15}/>Lịch sử trạng thái</div>{sub.statusHistory.length ? <ol className="mt-3 space-y-3 [&>li:last-child>span:after]:hidden">{sub.statusHistory.map((h,i)=><li key={`${h.createdAt}-${i}`} className="relative flex gap-3"><span className="relative z-10 mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#6678D8] ring-2 ring-white after:absolute after:left-1/2 after:top-2 after:h-6 after:w-px after:-translate-x-1/2 after:bg-[#DCE2F1]"/><div className="min-w-0"><p className="text-xs font-semibold text-[#46516A]">{h.oldStatus ? `${labels[h.oldStatus]} → ` : ''}{labels[h.newStatus]}</p><p className="mt-0.5 text-[11px] text-[#8A93A6]">{formatDateTime(h.createdAt)}{h.note ? ` · ${vietnameseHistoryNote(h.note)}` : ''}</p></div></li>)}</ol> : <p className="mt-2 text-xs text-[#8A93A6]">Chưa có lịch sử cập nhật.</p>}</div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#EEF1F6] bg-[#FBFCFF] px-5 py-3"><div className="text-xs text-[#778197]">Tạm tính {formatMoney(sub.subtotal)} · Giảm {formatMoney(sub.discountAmount)}</div><div className="flex flex-wrap items-center gap-3"><div className="text-sm font-bold text-[#596BD0]">Thành tiền {formatMoney(sub.totalAmount)}</div>{sub.status==='SHIPPING'&&<button disabled={!!busyId} onClick={()=>void confirmReceived(sub.id)} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50">{busyId===sub.id?'Đang xác nhận...':<><CheckCircle2 size={15}/>Đã nhận được hàng</>}</button>}</div></div>
          {sub.status==='PENDING' && <div className="flex justify-end border-t border-[#EEF1F6] px-5 py-3"><button disabled={!!busyId} onClick={()=>void cancel(sub.id)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-50">{busyId===sub.id?'Đang hủy...':'Hủy đơn shop'}</button></div>}
        </section>)}</div>
        <aside className="h-fit rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm lg:sticky lg:top-24"><div className="flex items-center gap-2"><ReceiptText size={18} className="text-[#596BD0]"/><h2 className="font-bold text-[#303A52]">{isSubOrderRoute ? 'Tổng đơn cửa hàng' : 'Tổng thanh toán'}</h2></div><dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between text-[#778197]"><dt>Tạm tính</dt><dd>{formatMoney(displayedSubtotal)}</dd></div><div className="flex justify-between text-emerald-700"><dt>Tổng giảm giá</dt><dd>-{formatMoney(displayedDiscount)}</dd></div><div className="flex justify-between border-t border-[#EEF1F6] pt-3 font-bold text-[#303A52]"><dt>Thành tiền</dt><dd className="text-lg text-[#596BD0]">{formatMoney(displayedTotal)}</dd></div></dl><div className="mt-4 rounded-xl bg-[#F8FAFD] p-3 text-xs text-[#697386]"><p>Phương thức: <strong className="text-[#35405A]">{order.paymentMethod ?? '—'}</strong></p><p className="mt-1">Thanh toán: <strong className="text-[#35405A]">{order.paymentStatus==='PAID'?'Đã thanh toán':order.paymentStatus==='PENDING'?'Chờ thanh toán':order.paymentStatus}</strong></p></div>
          {canConfirmCod && <button onClick={()=>void confirmCod()} disabled={!!busyId} className="mt-4 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-[#596BD0] text-sm font-semibold text-white hover:bg-[#4859C2] disabled:opacity-50">{busyId===order.id?'Đang xác nhận...':<><CheckCircle2 size={16}/>Xác nhận đã nhận hàng & thanh toán</>}</button>}
          {order.paymentMethod==='COD' && order.paymentStatus==='PENDING' && !canConfirmCod && <p className="mt-3 flex gap-2 rounded-lg bg-amber-50 p-3 text-[11px] leading-5 text-amber-800"><Truck size={15} className="shrink-0"/>Chỉ có thể xác nhận COD sau khi tất cả đơn shop chưa hủy đã giao thành công. Shop sẽ ghi nhận đã thu tiền khi hoàn tất giao hàng.</p>}
        </aside>
      </div>
    </> : <div className="mt-5 rounded-2xl bg-white p-10 text-center text-sm text-[#697386]">{error || 'Không tìm thấy đơn hàng.'}</div>}
  </div></main>;
}
