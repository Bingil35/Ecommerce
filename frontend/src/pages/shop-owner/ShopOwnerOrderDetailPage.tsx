import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, MapPin, Package, Truck } from 'lucide-react';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderStatus, ShopOwnerOrder } from '../../services/checkout/checkoutTypes';
import { mediaUrl } from '../../services/api/mediaUrl';
import { orderStatusClasses, orderStatusLabels } from '../../services/checkout/orderStatusPresentation';

const historyLabel = (status: OrderStatus, isFirst: boolean) => isFirst && status === 'PENDING' ? 'Đặt hàng' : orderStatusLabels[status];
const vietnameseNote = (note: string) => ({
  'Order created.': 'Đơn hàng được tạo.',
  'Shop confirmed the order; shipment is in progress.': 'Cửa hàng đã xác nhận đơn và đang chuẩn bị giao hàng.',
  'Customer cancelled the order.': 'Khách hàng đã hủy đơn hàng.',
  'Customer confirmed receipt.': 'Khách hàng đã xác nhận nhận hàng.',
} as Record<string, string>)[note] ?? note;

export default function ShopOwnerOrderDetailPage() {
  const { orderId = '' } = useParams();
  const [order, setOrder] = useState<ShopOwnerOrder | null>(null);
  const [error, setError] = useState('');
  useEffect(() => { let active = true; void checkoutApi.getShopOrder(orderId).then(value => { if (active) setOrder(value); }).catch(e => { if (active) setError(getApiError(e)); }); return () => { active = false; }; }, [orderId]);
  if (error) return <main className="seller-container py-10"><Link to="/shop-owner/orders" className="inline-flex items-center gap-2 text-indigo-600"><ArrowLeft size={17}/> Danh sách đơn hàng</Link><p className="mt-5 rounded-2xl bg-rose-50 p-5 text-rose-700">{error}</p></main>;
  if (!order) return <main className="seller-container py-16 text-center text-slate-500">Đang tải chi tiết đơn hàng...</main>;
  return <main className="seller-container !max-w-[1180px] py-6 sm:py-8">
    <Link to="/shop-owner/orders" className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-indigo-600"><ArrowLeft size={17}/> Danh sách đơn hàng</Link>
    <div className="grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1.5fr)_minmax(280px,.8fr)]">
      <section className="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3 rounded-xl bg-slate-50 p-3 sm:p-4"><div className="min-w-0"><p className="text-xs font-medium uppercase tracking-wide text-indigo-600">Chi tiết đơn hàng</p><h1 className="mt-1 break-all text-lg font-semibold text-slate-900 sm:text-xl">{order.orderCode}</h1><p className="mt-1 text-sm font-normal text-slate-500">{formatDateTime(order.createdAt)}</p></div><span className={`shrink-0 rounded-full px-3 py-1.5 text-xs font-medium ${orderStatusClasses[order.status]}`}>{orderStatusLabels[order.status]}</span></div>
        <div className="mb-2 flex items-center gap-3"><span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-indigo-50 text-indigo-600"><Package size={17}/></span><div className="min-w-0"><h2 className="text-base font-medium text-slate-800">Sản phẩm trong đơn</h2><p className="text-xs text-slate-500">Các sản phẩm đã đặt</p></div></div>
        <div className="divide-y divide-slate-100">{order.items.map(item=><article key={item.id} className="flex min-w-0 flex-wrap items-start justify-between gap-3 py-4"><div className="flex min-w-0 flex-1 gap-3"><img src={item.imageUrl?mediaUrl(item.imageUrl):'/placeholder-product.png'} alt={item.productName} className="h-16 w-16 shrink-0 rounded-xl border border-slate-100 bg-slate-50 object-cover"/><div className="min-w-0"><h3 className="break-words text-sm font-medium text-slate-800">{item.productName}</h3>{item.variantOptions?.length>0&&<p className="mt-1 text-xs font-normal text-slate-500">Phân loại: {item.variantOptions.join(' · ')}</p>}<p className="mt-1 text-sm font-normal text-slate-500">{formatMoney(item.price)} × {item.quantity}</p></div></div><div className="ml-auto text-right text-sm font-medium text-slate-800">{formatMoney(item.totalAmount)}</div></article>)}</div>
        <div className="mt-2 space-y-3 border-t border-slate-200 pt-4 text-sm"><div className="flex justify-between text-slate-500"><span>Tạm tính</span><span>{formatMoney(order.subtotal)}</span></div><div className="flex justify-between text-slate-500"><span>Giảm giá</span><span>−{formatMoney(order.discountAmount)}</span></div><div className="flex justify-between border-t border-slate-100 pt-3 font-semibold text-slate-900"><span>Tổng thanh toán</span><span className="text-indigo-600">{formatMoney(order.totalAmount)}</span></div></div>
        <section className="mt-5 border-t border-slate-100 pt-4"><div className="mb-4 flex items-center gap-3"><span className="grid h-9 w-9 place-items-center rounded-xl bg-violet-50 text-violet-600"><Truck size={17}/></span><h2 className="font-medium text-slate-800">Lịch sử xử lý</h2></div>{order.statusHistory.length?<ol className="space-y-4">{order.statusHistory.map((event,i)=><li key={`${event.createdAt}-${i}`} className="relative border-l border-indigo-100 pb-1 pl-4 last:border-transparent"><span className="absolute -left-[5px] top-1 h-2 w-2 rounded-full bg-indigo-500 ring-4 ring-white"/><p className="text-sm font-medium text-slate-700">{event.oldStatus?`${orderStatusLabels[event.oldStatus]} → `:''}{historyLabel(event.newStatus,i===0)}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(event.createdAt)}</p>{event.note&&<p className="mt-1 text-sm font-normal text-slate-600">{vietnameseNote(event.note)}</p>}</li>)}</ol>:<p className="text-sm text-slate-500">Chưa có lịch sử cập nhật.</p>}</section>
      </section>
      <aside className="min-w-0 space-y-4"><section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"><h2 className="mb-4 font-medium text-slate-800">Thanh toán</h2><div className="space-y-3 text-sm"><div className="flex justify-between gap-3"><span className="text-slate-500">Phương thức</span><span className="text-right font-medium text-slate-700">{order.paymentMethod||'Chưa rõ'}</span></div><div className="flex justify-between gap-3"><span className="text-slate-500">Trạng thái</span><span className="text-right font-medium text-slate-700">{order.paymentStatus==='PAID'?'Đã thanh toán':order.paymentStatus==='PENDING'?'Chờ thanh toán':order.paymentStatus==='FAILED'?'Thanh toán thất bại':'Đã hoàn tiền'}</span></div></div></section>
        <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5"><div className="mb-3 flex items-center gap-3"><span className="grid h-9 w-9 place-items-center rounded-xl bg-sky-50 text-sky-600"><MapPin size={17}/></span><h2 className="font-medium text-slate-800">Địa chỉ giao hàng</h2></div>{order.shippingAddress?<div className="space-y-1 text-sm font-normal leading-6 text-slate-600"><p className="font-medium text-slate-700">{order.shippingAddress.recipientName} · {order.shippingAddress.recipientPhone}</p><p>{order.shippingAddress.addressDetail}, {order.shippingAddress.ward}, {order.shippingAddress.recipientCity}</p></div>:<p className="text-sm text-slate-500">Chưa có địa chỉ giao hàng.</p>}</section></aside>
    </div>
  </main>;
}
