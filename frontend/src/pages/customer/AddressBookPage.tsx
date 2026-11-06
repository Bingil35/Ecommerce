import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { MapPin, Plus, Pencil, Trash2, Star, ArrowLeft } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { checkoutApi, getApiError } from '../../services/checkout/checkoutApi';
import type { CustomerAddress } from '../../services/checkout/checkoutTypes';

type FormState = { recipientName: string; recipientPhone: string; provinceCity: string; ward: string; addressDetail: string; isDefault: boolean };
const empty: FormState = { recipientName: '', recipientPhone: '', provinceCity: '', ward: '', addressDetail: '', isDefault: false };
const inputClass = 'mt-1.5 h-11 w-full rounded-xl border border-[#E2E8F2] px-3 text-sm outline-none focus:border-[#6576D8] focus:ring-2 focus:ring-[#6576D8]/15';
export default function AddressBookPage() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  const [addresses, setAddresses] = useState<CustomerAddress[]>([]);
  const [form, setForm] = useState<FormState>(empty);
  const [editingId, setEditingId] = useState<string | undefined>();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const load = async () => setAddresses(await checkoutApi.getAddresses());
  useEffect(() => { if (isAuthenticated) void load().catch(e => setError(getApiError(e))); }, [isAuthenticated]);
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: '/account/addresses' }}/>;
  const startEdit = (a?: CustomerAddress) => {
    setEditingId(a?.id); setForm(a ? { recipientName: a.recipientName, recipientPhone: a.recipientPhone, provinceCity: a.provinceCity, ward: a.ward, addressDetail: a.addressDetail, isDefault: a.isDefault } : empty);
    setError(''); setOpen(true);
  };
  const save = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setError('');
    try { await checkoutApi.saveAddress(form, editingId); await load(); setOpen(false); setForm(empty); }
    catch (e) { setError(getApiError(e)); } finally { setBusy(false); }
  };
  const remove = async (id: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa địa chỉ này?')) return;
    try { await checkoutApi.deleteAddress(id); await load(); } catch (e) { setError(getApiError(e)); }
  };
  const setDefault = async (id: string) => { try { await checkoutApi.setDefaultAddress(id); await load(); } catch (e) { setError(getApiError(e)); } };
  return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-8 sm:px-5"><div className="mx-auto max-w-[900px]">
    <Link to="/checkout" state={(location.state as { checkoutState?: unknown } | null)?.checkoutState} className="inline-flex items-center gap-2 text-sm text-[#596BD0] hover:underline"><ArrowLeft size={16}/>Quay lại thanh toán</Link>
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold text-[#252B40]">Địa chỉ của tôi</h1><p className="mt-1 text-sm text-[#7A8498]">Quản lý địa chỉ nhận hàng</p></div><button onClick={() => startEdit()} className="inline-flex h-10 items-center gap-2 rounded-xl bg-[#596BD0] px-4 text-sm font-semibold text-white"><Plus size={17}/>Thêm địa chỉ</button></div>
    {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    <div className="mt-5 space-y-3">{addresses.map(a => <article key={a.id} className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm"><div className="flex flex-wrap items-start justify-between gap-3"><div className="flex min-w-0 gap-3"><MapPin className="mt-1 shrink-0 text-[#596BD0]"/><div><div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-[#29334B]">{a.recipientName}<span className="font-normal text-[#768096]">{a.recipientPhone}</span>{a.isDefault && <span className="rounded-full bg-[#EEF2FF] px-2 py-1 text-[10px] font-semibold text-[#596BD0]">Mặc định</span>}</div><p className="mt-1 text-sm text-[#68738A]">{a.addressDetail}, {a.ward}, {a.provinceCity}</p></div></div><div className="flex shrink-0 gap-2"><button onClick={() => startEdit(a)} className="rounded-lg border border-[#E3E8F1] p-2 text-[#667085] hover:bg-[#F6F8FC]" aria-label="Sửa địa chỉ"><Pencil size={16}/></button><button onClick={() => void remove(a.id)} className="rounded-lg border border-[#F1DCDC] p-2 text-[#C65C5C] hover:bg-rose-50" aria-label="Xóa địa chỉ"><Trash2 size={16}/></button></div></div>{!a.isDefault && <button onClick={() => void setDefault(a.id)} className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-[#596BD0] hover:underline"><Star size={14}/>Đặt làm mặc định</button>}</article>)}</div>
    {open && <div className="fixed inset-0 z-[70] flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4" onClick={() => setOpen(false)}><form onSubmit={save} onClick={e => e.stopPropagation()} className="w-full max-w-lg space-y-4 rounded-t-2xl bg-white p-5 sm:rounded-2xl"><div className="flex items-center justify-between"><h2 className="text-lg font-bold text-[#252B40]">{editingId ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới'}</h2><button type="button" onClick={() => setOpen(false)} className="text-sm text-[#7A8498]">Đóng</button></div>{(['recipientName','recipientPhone','provinceCity','ward','addressDetail'] as const).map(key => <label key={key} className="block text-xs font-semibold text-[#68738A]">{({recipientName:'Người nhận',recipientPhone:'Số điện thoại',provinceCity:'Tỉnh / thành phố',ward:'Xã / phường',addressDetail:'Địa chỉ chi tiết'} as Record<string,string>)[key]}<input required className={inputClass} value={form[key]} onChange={e => setForm(v => ({...v,[key]:e.target.value}))}/></label>)}<label className="flex items-center gap-2 text-sm text-[#46516A]"><input type="checkbox" checked={form.isDefault} onChange={e => setForm(v => ({...v,isDefault:e.target.checked}))} className="accent-[#596BD0]"/>Đặt làm địa chỉ mặc định</label>{error && <p className="text-sm text-rose-600">{error}</p>}<button disabled={busy} className="h-11 w-full rounded-xl bg-[#596BD0] text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Đang lưu...' : 'Lưu địa chỉ'}</button></form></div>}
  </div></main>;
}
