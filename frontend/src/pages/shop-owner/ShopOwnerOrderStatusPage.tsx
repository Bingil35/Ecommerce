import { useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { ClipboardCheck, PackageCheck, Truck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, getApiError } from '../../services/checkout/checkoutApi';
import type { OrderStatus } from '../../services/checkout/checkoutTypes';

const steps: { status: OrderStatus; label: string; description: string; icon: typeof PackageCheck }[] = [
  { status: 'CONFIRMED', label: 'Xác nhận đơn', description: 'Tiếp nhận và xác nhận đơn PENDING.', icon: ClipboardCheck },
  { status: 'SHIPPING', label: 'Đang giao hàng', description: 'Chuyển đơn đã xác nhận sang trạng thái vận chuyển.', icon: Truck },
  { status: 'DELIVERED', label: 'Đã giao hàng', description: 'Hoàn tất giao hàng. Với COD, thao tác này ghi nhận shop đã thu tiền.', icon: PackageCheck },
];
export default function ShopOwnerOrderStatusPage() {
  const { isAuthenticated, isLoading, user } = useAuth();
  const [subOrderId, setSubOrderId] = useState('');
  const [status, setStatus] = useState<OrderStatus>('CONFIRMED');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: '/shop-owner/orders/status' }}/>;
  const roles = user?.roles ?? (user?.roleDefault ? [user.roleDefault] : []);
  if (roles.length && !roles.some(r => r.toUpperCase().includes('SHOP'))) return <div className="mx-auto max-w-2xl px-4 py-16 text-center text-sm text-rose-600">Tài khoản không có quyền truy cập khu vực cửa hàng.</div>;
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setError(''); setSuccess('');
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(subOrderId.trim())) { setError('Mã đơn shop phải là UUID hợp lệ.'); return; }
    setBusy(true);
    try { const result = await checkoutApi.updateShopSubOrderStatus(subOrderId.trim(), status, note); setSuccess(`Đã cập nhật trạng thái: ${result.status}. Backend đã ghi lịch sử trạng thái.`); setNote(''); }
    catch(e) { setError(getApiError(e, 'Không thể cập nhật trạng thái đơn hàng.')); }
    finally { setBusy(false); }
  };
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-4 py-10"><div className="mx-auto max-w-2xl">
    <p className="text-xs font-semibold uppercase tracking-[.15em] text-[#6576D8]">Khu vực cửa hàng</p><h1 className="mt-2 text-2xl font-bold text-[#252B40]">Cập nhật trạng thái đơn shop</h1><p className="mt-2 text-sm leading-6 text-[#778197]">Backend chỉ cho phép chuyển trạng thái tuần tự: PENDING → CONFIRMED → SHIPPING → DELIVERED. Quyền sở hữu đơn được xác minh từ tài khoản shop đang đăng nhập.</p>
    <form onSubmit={submit} className="mt-6 space-y-5 rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm sm:p-7">
      <label className="block text-xs font-semibold text-[#697386]">Mã SubOrder (UUID)<input value={subOrderId} onChange={e=>setSubOrderId(e.target.value)} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" className="mt-1.5 h-11 w-full rounded-xl border border-[#E2E8F2] px-3 text-sm outline-none focus:border-[#6576D8]" required/></label>
      <div><p className="mb-2 text-xs font-semibold text-[#697386]">Trạng thái muốn cập nhật</p><div className="space-y-2">{steps.map(step=>{const Icon=step.icon;return <label key={step.status} className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3 ${status===step.status?'border-[#6576D8] bg-[#F5F7FF]':'border-[#E8ECF3]'}`}><input type="radio" checked={status===step.status} onChange={()=>setStatus(step.status)} className="mt-1 accent-[#596BD0]"/><Icon size={19} className="mt-0.5 text-[#596BD0]"/><span><span className="block text-sm font-semibold text-[#34405A]">{step.label}</span><span className="mt-0.5 block text-xs text-[#7A8498]">{step.description}</span></span></label>;})}</div></div>
      <label className="block text-xs font-semibold text-[#697386]">Ghi chú (không bắt buộc)<textarea value={note} onChange={e=>setNote(e.target.value)} maxLength={500} rows={3} placeholder="Ghi chú cho lịch sử đơn hàng..." className="mt-1.5 w-full rounded-xl border border-[#E2E8F2] p-3 text-sm outline-none focus:border-[#6576D8]"/></label>
      {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {success && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}
      <button disabled={busy} className="h-11 w-full rounded-xl bg-[#596BD0] text-sm font-semibold text-white hover:bg-[#4859C2] disabled:opacity-50">{busy?'Đang cập nhật...':'Cập nhật trạng thái'}</button>
    </form>
    <p className="mt-4 text-xs leading-5 text-[#8A93A6]">Lưu ý: phiên bản backend hiện có API PATCH cập nhật trạng thái nhưng chưa có API GET danh sách/chi tiết đơn dành cho shop owner. Vì vậy màn hình này nhận mã SubOrder từ hệ thống quản lý đơn hiện có; backend sẽ từ chối mã không thuộc shop đang đăng nhập hoặc chuyển trạng thái sai thứ tự.</p>
  </div></main>;
}
