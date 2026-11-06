import { useState, type FormEvent } from 'react';
import { Navigate, Link } from 'react-router-dom';
import { ArrowLeft, ClipboardCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, getApiError } from '../../services/checkout/checkoutApi';

export default function ShopOwnerOrderStatusPage() {
  const { isAuthenticated, isLoading, user } = useAuth();
  const [subOrderId, setSubOrderId] = useState('');
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
    try { const result = await checkoutApi.updateShopSubOrderStatus(subOrderId.trim(), 'CONFIRMED', note); setSuccess(`Đã xác nhận đơn. Trạng thái hiện tại: ${result.status} (đang vận chuyển).`); setNote(''); }
    catch (e) { setError(getApiError(e, 'Không thể xác nhận đơn hàng.')); }
    finally { setBusy(false); }
  };
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-4 py-10"><div className="mx-auto max-w-2xl"><Link to="/shop-owner/orders" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-600"><ArrowLeft size={16}/> Danh sách đơn hàng</Link><p className="mt-6 text-xs font-semibold uppercase tracking-[.15em] text-[#6576D8]">Khu vực cửa hàng</p><h1 className="mt-2 text-2xl font-bold text-[#252B40]">Xác nhận đơn hàng</h1><p className="mt-2 text-sm leading-6 text-[#778197]">Chủ cửa hàng chỉ xác nhận đơn đang chờ. Sau khi xác nhận, đơn chuyển sang Đang vận chuyển; khách hàng sẽ xác nhận đã nhận được hàng.</p>
    <form onSubmit={submit} className="mt-6 space-y-5 rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm sm:p-7"><label className="block text-xs font-semibold text-[#697386]">Mã đơn shop (UUID)<input value={subOrderId} onChange={e => setSubOrderId(e.target.value)} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" className="mt-1.5 h-11 w-full rounded-xl border border-[#E2E8F2] px-3 text-sm" required/></label><label className="block text-xs font-semibold text-[#697386]">Ghi chú (không bắt buộc)<textarea value={note} onChange={e => setNote(e.target.value)} maxLength={500} rows={3} className="mt-1.5 w-full rounded-xl border border-[#E2E8F2] p-3 text-sm"/></label>{error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}{success && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}<button disabled={busy} className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-[#596BD0] text-sm font-semibold text-white disabled:opacity-50"><ClipboardCheck size={17}/>{busy ? 'Đang xác nhận...' : 'Xác nhận & chuyển vận chuyển'}</button></form>
  </div></main>;
}
