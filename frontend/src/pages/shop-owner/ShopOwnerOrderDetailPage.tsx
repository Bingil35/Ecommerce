import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, MapPin, Package, Truck } from 'lucide-react';
import { checkoutApi, formatDateTime, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderStatus, ShopOwnerOrder } from '../../services/checkout/checkoutTypes';

const labels: Record<OrderStatus, string> = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang vận chuyển', DELIVERED: 'Hoàn thành', CANCELLED: 'Đã hủy' };

export default function ShopOwnerOrderDetailPage() {
  const { orderId = '' } = useParams();
  const [order, setOrder] = useState<ShopOwnerOrder | null>(null);
  const [error, setError] = useState('');
  useEffect(() => { let active = true; void checkoutApi.getShopOrder(orderId).then(value => { if (active) setOrder(value); }).catch(e => { if (active) setError(getApiError(e)); }); return () => { active = false; }; }, [orderId]);
  if (error) return <main className="seller-container py-10"><Link to="/shop-owner/orders" className="inline-flex items-center gap-2 text-indigo-600"><ArrowLeft size={17}/> Danh sách đơn hàng</Link><p className="mt-5 rounded-2xl bg-rose-50 p-5 text-rose-700">{error}</p></main>;
  if (!order) return <main className="seller-container py-16 text-center text-slate-500">Đang tải chi tiết đơn hàng...</main>;
  return <main className="seller-container !max-w-[1180px] py-8">
    <Link to="/shop-owner/orders" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-indigo-600"><ArrowLeft size={17}/> Danh sách đơn hàng</Link>
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-indigo-100 bg-gradient-to-r from-white to-indigo-50 p-6 sm:p-8">
      <div><p className="text-xs font-bold uppercase tracking-widest text-indigo-500">Chi tiết đơn hàng</p><h1 className="mt-2 text-2xl font-extrabold text-slate-900">{order.orderCode}</h1><p className="mt-2 text-sm text-slate-500">{order.customerName || 'Khách hàng'} · {formatDateTime(order.createdAt)}</p></div>
      <span className="rounded-full bg-indigo-100 px-4 py-2 text-sm font-bold text-indigo-700">{labels[order.status]}</span>
    </header>
    <div className="grid gap-5 lg:grid-cols-[minmax(0,1.5fr)_minmax(280px,.8fr)]">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7"><div className="mb-5 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-600"><Package size={19}/></span><h2 className="text-lg font-bold text-slate-900">Sản phẩm trong đơn</h2></div>
        <div className="divide-y divide-slate-100">{order.items.map(item => <article key={item.id} className="flex flex-wrap items-center justify-between gap-4 py-5"><div className="min-w-0"><h3 className="font-semibold text-slate-800">{item.productName}</h3><p className="mt-1 text-sm text-slate-500">{formatMoney(item.price)} × {item.quantity}</p></div><strong className="text-slate-900">{formatMoney(item.totalAmount)}</strong></article>)}</div>
        <div className="mt-2 space-y-3 border-t border-slate-200 pt-5 text-sm"><div className="flex justify-between text-slate-500"><span>Tạm tính</span><span>{formatMoney(order.subtotal)}</span></div><div className="flex justify-between text-slate-500"><span>Giảm giá</span><span>−{formatMoney(order.discountAmount)}</span></div><div className="flex justify-between border-t border-slate-100 pt-4 text-base font-extrabold text-slate-900"><span>Tổng thanh toán</span><span className="text-indigo-600">{formatMoney(order.totalAmount)}</span></div></div>
      </section>
      <aside className="space-y-5"><section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><h2 className="mb-4 font-bold text-slate-900">Thanh toán</h2><div className="space-y-3 text-sm"><div className="flex justify-between gap-3"><span className="text-slate-500">Phương thức</span><b className="text-slate-800">{order.paymentMethod || 'Chưa rõ'}</b></div><div className="flex justify-between gap-3"><span className="text-slate-500">Trạng thái</span><b className="text-slate-800">{order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'PENDING' ? 'Chờ thanh toán' : order.paymentStatus}</b></div></div></section><section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="mb-4 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-sky-50 text-sky-600"><MapPin size={19}/></span><h2 className="font-bold text-slate-900">Địa chỉ giao hàng</h2></div>{order.shippingAddress ? <div className="space-y-1 text-sm leading-6 text-slate-600"><b className="block text-slate-800">{order.shippingAddress.recipientName} · {order.shippingAddress.recipientPhone}</b><p>{order.shippingAddress.addressDetail}, {order.shippingAddress.ward}, {order.shippingAddress.recipientCity}</p></div> : <p className="text-sm text-slate-500">Chưa có địa chỉ giao hàng.</p>}</section>
        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="mb-5 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-violet-50 text-violet-600"><Truck size={19}/></span><h2 className="font-bold text-slate-900">Lịch sử xử lý</h2></div><ol className="space-y-4">{order.statusHistory.map((event, i) => <li key={`${event.createdAt}-${i}`} className="relative border-l-2 border-indigo-100 pb-1 pl-4 last:border-transparent"><span className="absolute -left-[6px] top-1 h-2.5 w-2.5 rounded-full bg-indigo-500 ring-4 ring-white"/><b className="text-sm text-slate-800">{labels[event.newStatus]}</b><p className="mt-1 text-xs text-slate-500">{formatDateTime(event.createdAt)}</p>{event.note && <p className="mt-1 text-sm text-slate-600">{event.note}</p>}</li>)}</ol>{order.statusHistory.length === 0 && <p className="text-sm text-slate-500">Chưa có lịch sử cập nhật.</p>}</section>
      </aside>
    </div>
  </main>;
}
