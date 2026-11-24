import { LoadingButton } from '../../components/common/LoadingState';
import { useToast, getFeedbackError } from '../../context/FeedbackContext';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { BadgePercent, CalendarDays, Clock3, Pencil, Plus, Search, TicketPercent, X } from 'lucide-react';
import axiosClient from '../../services/api/axiosClient';
import { getApiError } from '../../services/checkout/checkoutApi';

type Coupon = { id: string; code: string; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; minimumOrder: number | null; maximumDiscount: number | null; usageLimit: number | null; usedCount: number; startAt: string | null; endAt: string | null; status: 'ACTIVE' | 'INACTIVE' | 'EXPIRED' };
type Payload = Omit<Coupon, 'id' | 'usedCount'>;
type Envelope<T> = { data: T };
const empty: Payload = { code: '', discountType: 'PERCENTAGE', discountValue: 10, minimumOrder: null, maximumDiscount: null, usageLimit: null, startAt: null, endAt: null, status: 'ACTIVE' };
const money = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
const date = (value: string | null) => value ? new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Không giới hạn';

export default function SellerPromotionsPage() {
  const toast=useToast();
  const [items, setItems] = useState<Coupon[]>([]);
  const [form, setForm] = useState<Payload>(empty);
  const [editing, setEditing] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formVisible, setFormVisible] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [statusFilter, setStatusFilter] = useState<'ALL' | Coupon['status']>('ALL');
  const [codeQuery, setCodeQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<'' | Coupon['discountType']>('');
  const [minimumValue, setMinimumValue] = useState('');
  const [maximumValue, setMaximumValue] = useState('');
  const [periodStart, setPeriodStart] = useState('');
  const [periodEnd, setPeriodEnd] = useState('');
  const closeTimer = useRef<number | null>(null);
  const load = useCallback(async () => { try { setItems((await axiosClient.get<Envelope<Coupon[]>>('/shop-owner/coupons')).data.data); setError(''); } catch (e) { toast.error(getFeedbackError(e));  setError(getApiError(e)); } }, []);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => () => { if (closeTimer.current) window.clearTimeout(closeTimer.current); }, []);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setError('');
    try { if (editing) await axiosClient.put(`/shop-owner/coupons/${editing}`, form); else await axiosClient.post('/shop-owner/coupons', form); toast.success('Đã lưu chương trình khuyến mãi.'); await load(); cancel(); }
    catch (e) { toast.error(getFeedbackError(e));  setError(getApiError(e)); } finally { setBusy(false); }
  };
  const openCreate = () => { if (closeTimer.current) window.clearTimeout(closeTimer.current); setEditing(null); setForm(empty); setShowForm(true); requestAnimationFrame(() => setFormVisible(true)); };
  const edit = (item: Coupon) => { if (closeTimer.current) window.clearTimeout(closeTimer.current); setEditing(item.id); setForm({ code: item.code, discountType: item.discountType, discountValue: item.discountValue, minimumOrder: item.minimumOrder, maximumDiscount: item.maximumDiscount, usageLimit: item.usageLimit, startAt: item.startAt, endAt: item.endAt, status: item.status }); setShowForm(true); requestAnimationFrame(() => setFormVisible(true)); };
  const cancel = () => { setFormVisible(false); if (closeTimer.current) window.clearTimeout(closeTimer.current); closeTimer.current = window.setTimeout(() => { setEditing(null); setForm(empty); setShowForm(false); closeTimer.current = null; }, 220); };
  const input = (key: keyof Payload, label: string, type = 'text') => <label className="grid gap-1.5 text-xs font-medium text-slate-600">{label}<input required={key === 'code' || key === 'discountValue'} type={type} value={(form[key] as string | number | null) ?? ''} onChange={e => setForm(current => ({ ...current, [key]: type === 'number' ? (e.target.value === '' ? null : Number(e.target.value)) : e.target.value }))} className="mt-0.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400" /></label>;
  const statusCounts = { ALL: items.length, ACTIVE: items.filter(item => item.status === 'ACTIVE').length, INACTIVE: items.filter(item => item.status === 'INACTIVE').length, EXPIRED: items.filter(item => item.status === 'EXPIRED').length };
  const filteredItems = items.filter(item => {
    if (statusFilter !== 'ALL' && item.status !== statusFilter) return false;
    if (codeQuery && !item.code.toLowerCase().includes(codeQuery.trim().toLowerCase())) return false;
    if (typeFilter && item.discountType !== typeFilter) return false;
    if (minimumValue && item.discountValue < Number(minimumValue)) return false;
    if (maximumValue && item.discountValue > Number(maximumValue)) return false;
    if (periodStart && item.endAt && new Date(item.endAt) < new Date(`${periodStart}T00:00:00`)) return false;
    if (periodEnd && item.startAt && new Date(item.startAt) > new Date(`${periodEnd}T23:59:59`)) return false;
    return true;
  });
  const editorForm = <form onSubmit={e => void submit(e)} className="rounded-2xl bg-white p-5 sm:p-6">
    <div className="mb-5 flex items-center justify-between gap-3 border-b border-slate-100 pb-4"><div className="flex min-w-0 items-center gap-3"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-indigo-50 text-[#5367C9]"><BadgePercent size={20}/></span><div><h2 className="text-base font-semibold text-[#17213f]">{editing ? 'Chỉnh sửa khuyến mãi' : 'Thông tin ưu đãi mới'}</h2><p className="mt-1 text-xs text-slate-500">Thiết lập mã, mức giảm và thời gian áp dụng.</p></div></div><button type="button" aria-label="Đóng biểu mẫu" onClick={cancel} className="grid h-9 w-9 shrink-0 place-items-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100"><X size={19}/></button></div>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{input('code', 'Mã khuyến mãi')}<label className="grid gap-1.5 text-xs font-medium text-slate-600">Hình thức giảm<select value={form.discountType} onChange={e => setForm(current => ({ ...current, discountType: e.target.value as Payload['discountType'] }))} className="mt-0.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"><option value="PERCENTAGE">Giảm theo phần trăm</option><option value="FIXED_AMOUNT">Giảm số tiền cố định</option></select></label>{input('discountValue', form.discountType === 'PERCENTAGE' ? 'Phần trăm giảm' : 'Số tiền giảm', 'number')}{input('minimumOrder', 'Đơn hàng tối thiểu (₫)', 'number')}{form.discountType === 'PERCENTAGE' && input('maximumDiscount', 'Mức giảm tối đa (₫)', 'number')}{input('usageLimit', 'Giới hạn lượt dùng', 'number')}<label className="grid gap-1.5 text-xs font-medium text-slate-600">Trạng thái<select value={form.status} onChange={e => setForm(current => ({ ...current, status: e.target.value as Payload['status'] }))} className="mt-0.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Tạm dừng</option></select></label></div>
    <div className="mt-4 grid gap-4 sm:grid-cols-2">{(['startAt', 'endAt'] as const).map((key, index) => <label key={key} className="grid gap-1.5 text-xs font-medium text-slate-600">{index === 0 ? 'Ngày bắt đầu' : 'Ngày kết thúc'}<input type="datetime-local" value={form[key]?.slice(0, 16) || ''} onChange={e => setForm(current => ({ ...current, [key]: e.target.value ? new Date(e.target.value).toISOString() : null }))} className="mt-0.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400" /></label>)}</div>
    <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4"><button type="button" onClick={cancel} className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-600">Hủy</button><LoadingButton type="submit" disabled={busy} className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-50" loading={busy} loadingText="Đang lưu...">{editing ? 'Lưu thay đổi' : 'Tạo mã ưu đãi'}</LoadingButton></div>
  </form>;

  return <div className="seller-container shop-management !max-w-[1280px]">
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
      <div className="flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-violet-50 text-violet-600"><TicketPercent size={24}/></span><div><h1 style={{ fontWeight: 650 }}>Khuyến mãi</h1><p className="mt-1">Tạo ưu đãi để khách hàng quay lại cửa hàng.</p></div></div>
      <button onClick={() => { if (showForm) cancel(); else openCreate(); }} className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-bold text-white"><Plus size={17}/>{showForm ? 'Đóng biểu mẫu' : 'Tạo khuyến mãi'}</button>
    </header>

    <section className="mb-5 rounded-2xl border border-[#e4e8f1] bg-white p-4 shadow-sm sm:p-5">
      <div className="mb-4 flex gap-1 overflow-x-auto border-b border-slate-100">{([['ALL', 'Tất cả'], ['ACTIVE', 'Đang hoạt động'], ['INACTIVE', 'Đã tạm dừng'], ['EXPIRED', 'Đã hết hạn']] as const).map(([key, label]) => <button key={key} type="button" onClick={() => setStatusFilter(key)} className={`shrink-0 border-b-2 px-3 py-2.5 text-sm ${statusFilter === key ? 'border-indigo-600 font-medium text-indigo-700' : 'border-transparent text-slate-500 hover:text-slate-800'}`}>{label}<span className="ml-1.5 rounded-full bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">{statusCounts[key]}</span></button>)}</div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Tìm theo mã<div className="relative min-w-0"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input value={codeQuery} onChange={e => setCodeQuery(e.target.value)} placeholder="Nhập mã khuyến mãi" className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white py-2.5 pl-9 pr-3 text-sm font-normal outline-none focus:border-indigo-400"/></div></label>
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Loại giảm giá<select value={typeFilter} onChange={e => setTypeFilter(e.target.value as typeof typeFilter)} className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"><option value="">Tất cả hình thức</option><option value="PERCENTAGE">Theo phần trăm</option><option value="FIXED_AMOUNT">Số tiền cố định</option></select></label>
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Giá trị giảm từ<input type="number" min="0" value={minimumValue} onChange={e => setMinimumValue(e.target.value)} placeholder="Giá trị tối thiểu" className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"/></label>
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Đến giá trị<input type="number" min="0" value={maximumValue} onChange={e => setMaximumValue(e.target.value)} placeholder="Giá trị tối đa" className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"/></label>
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Thời gian áp dụng từ<input type="date" value={periodStart} onChange={e => setPeriodStart(e.target.value)} className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"/></label>
        <label className="grid min-w-0 gap-1.5 text-xs font-medium text-slate-600">Đến ngày<input type="date" value={periodEnd} onChange={e => setPeriodEnd(e.target.value)} className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal outline-none focus:border-indigo-400"/></label>
      </div>
    </section>

    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}

    {showForm && <div className={`fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/45 p-3 transition-opacity duration-200 ease-out sm:p-6 ${formVisible ? 'opacity-100' : 'opacity-0'}`} onMouseDown={e => { if (e.target === e.currentTarget) cancel(); }}><div role="dialog" aria-modal="true" className={`max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-2xl shadow-2xl transition-all duration-200 ease-out ${formVisible ? 'translate-y-0 scale-100 opacity-100' : 'translate-y-3 scale-[0.98] opacity-0'}`}>{editorForm}</div></div>}

    {filteredItems.length ? <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{filteredItems.map(item => <article key={item.id} className="!overflow-hidden !rounded-2xl !border !bg-white !p-0">
      <div className={`relative flex min-h-36 items-center justify-between gap-3 px-5 py-5 ${item.status === 'ACTIVE' ? 'bg-indigo-50 text-indigo-900' : 'bg-slate-50 text-slate-700'}`}><div><span className={`mb-2 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-medium uppercase tracking-wider ${item.status === 'ACTIVE' ? 'bg-white text-indigo-700' : 'bg-white text-slate-600'}`}><TicketPercent size={12}/>{item.status === 'ACTIVE' ? 'Đang áp dụng' : item.status === 'EXPIRED' ? 'Đã hết hạn' : 'Đang tạm dừng'}</span><div className="text-2xl font-extrabold tracking-tight">{item.discountType === 'PERCENTAGE' ? `${item.discountValue}%` : money(item.discountValue)}</div><div className="mt-1 text-xs opacity-75">{item.discountType === 'PERCENTAGE' ? `Giảm tối đa ${item.maximumDiscount ? money(item.maximumDiscount) : 'không giới hạn'}` : 'trên đơn hàng'}</div></div><span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-white/80"><BadgePercent size={26}/></span></div>
      <div className="relative border-b border-dashed border-slate-200 px-5 py-4 before:absolute before:-left-2 before:top-1/2 before:h-4 before:w-4 before:-translate-y-1/2 before:rounded-full before:bg-[#f7f8fc] after:absolute after:-right-2 after:top-1/2 after:h-4 after:w-4 after:-translate-y-1/2 after:rounded-full after:bg-[#f7f8fc]"><div className="flex items-center justify-between gap-2"><div><span className="block text-[10px] font-bold uppercase tracking-widest text-slate-400">Mã ưu đãi</span><b className="mt-1 block font-mono text-lg tracking-wider text-[#17213f]">{item.code}</b></div>{item.usedCount === 0 && <button type="button" onClick={() => edit(item)} className="inline-flex items-center gap-1.5 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-xs font-medium text-[#5367C9]"><Pencil size={13}/>Chỉnh sửa</button>}</div></div>
      <div className="grid grid-cols-2 gap-y-4 px-5 py-4 text-xs"><div><span className="block text-slate-400">Đơn tối thiểu</span><b className="mt-1 block text-slate-700">{item.minimumOrder ? money(item.minimumOrder) : 'Không yêu cầu'}</b></div><div><span className="block text-slate-400">Lượt đã dùng</span><b className="mt-1 block text-slate-700">{item.usedCount}{item.usageLimit ? ` / ${item.usageLimit}` : ' lượt'}</b></div><div className="col-span-2 flex items-center gap-1.5 text-slate-500"><CalendarDays size={14}/>{date(item.startAt)} <span className="px-0.5">—</span> {date(item.endAt)}</div></div>
      <div className="flex items-center gap-1.5 border-t border-slate-100 bg-slate-50/70 px-5 py-3 text-[11px] font-semibold text-slate-500"><Clock3 size={13}/>{item.status === 'ACTIVE' ? 'Mã có thể được khách hàng sử dụng' : item.status === 'EXPIRED' ? 'Thời hạn sử dụng đã kết thúc' : 'Mã hiện không hiển thị cho khách'}</div>
    </article>)}</div> : <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center"><span className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-[#5367C9]"><TicketPercent size={23}/></span><h2 className="font-semibold text-[#17213f]">{items.length ? 'Không tìm thấy khuyến mãi phù hợp' : 'Chưa có mã khuyến mãi'}</h2><p className="mt-1 text-sm text-slate-500">{items.length ? 'Thử thay đổi trạng thái hoặc điều kiện lọc.' : 'Tạo ưu đãi đầu tiên để thu hút khách hàng.'}</p>{!items.length && <button onClick={() => setShowForm(true)} className="mt-4 inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white"><Plus size={16}/>Tạo khuyến mãi</button>}</div>}
  </div>;
}
