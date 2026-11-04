import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Boxes, CalendarDays, Check, CircleDollarSign, Package, Pencil, Save, Store, Tag, X } from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { mediaUrl } from '../../services/api/mediaUrl';

const money = (value: number | null) => value == null ? 'Chưa có giá' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
const statusText: Record<SellerProduct['status'], string> = { ACTIVE: 'Đang bán', INACTIVE: 'Tạm ẩn', PENDING: 'Chờ duyệt', REJECTED: 'Từ chối' };
const errorMessage = (e: unknown) => { const err = e as { response?: { data?: { message?: string } }; message?: string }; return err.response?.data?.message || err.message || 'Không thể xử lý yêu cầu.'; };

export default function SellerProductDetailPage() {
  const { productId = '' } = useParams();
  const [product, setProduct] = useState<SellerProduct | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', categoryId: '', thumbnailUrl: '' });
  const load = async () => { const item = await sellerProductApi.get(productId); setProduct(item); setForm({ name: item.name, description: item.description || '', categoryId: item.categoryId || '', thumbnailUrl: item.thumbnailUrl || '' }); };
  useEffect(() => { let active = true; void sellerProductApi.get(productId).then(item => { if (active) { setProduct(item); setForm({ name: item.name, description: item.description || '', categoryId: item.categoryId || '', thumbnailUrl: item.thumbnailUrl || '' }); } }).catch(e => { if (active) setError(errorMessage(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [productId]);
  const save = async (event: React.FormEvent) => { event.preventDefault(); setSaving(true); setError(''); setSaved(false); try { await sellerProductApi.update(productId, { ...form, categoryId: form.categoryId.trim() }); await load(); setEditing(false); setSaved(true); } catch (e) { setError(errorMessage(e)); } finally { setSaving(false); } };
  const startEdit = () => { if (product) setForm({ name: product.name, description: product.description || '', categoryId: product.categoryId || '', thumbnailUrl: product.thumbnailUrl || '' }); setEditing(true); setSaved(false); };

  if (loading) return <main className="seller-container py-16 text-center text-sm text-slate-500">Đang tải chi tiết sản phẩm...</main>;
  if (error && !product) return <main className="seller-container py-12"><Link to="/seller/products" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-600"><ArrowLeft size={17}/> Quay lại sản phẩm</Link><p className="mt-6 rounded-2xl bg-rose-50 p-5 text-sm text-rose-700">{error}</p></main>;
  if (!product) return null;
  return <main className="seller-container max-w-[1180px] py-7 sm:py-10">
    <Link to="/seller/products" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-indigo-600"><ArrowLeft size={17}/> Tất cả sản phẩm</Link>
    {saved && <p className="mb-4 flex items-center gap-2 rounded-xl bg-emerald-50 p-3 text-sm font-semibold text-emerald-700"><Check size={17}/> Đã cập nhật thông tin sản phẩm.</p>}
    {error && <p className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    <section className="overflow-hidden rounded-[28px] border border-[#E5EAF4] bg-white shadow-[0_16px_50px_rgba(48,63,112,.08)]">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 px-6 py-5 sm:px-8"><div className="flex items-center gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-indigo-50 text-indigo-600"><Store size={21}/></span><div><p className="text-xs font-bold uppercase tracking-widest text-indigo-500">Quản lý sản phẩm</p><p className="mt-1 text-sm text-slate-500">Thông tin sản phẩm và tình trạng kinh doanh</p></div></div><div className="flex gap-2">{editing ? <><button type="button" onClick={() => { setEditing(false); setError(''); }} className="inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-bold text-slate-600"><X size={16}/> Hủy</button><button form="seller-product-edit" disabled={saving} className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50"><Save size={16}/>{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</button></> : <button type="button" onClick={startEdit} className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-bold text-white"><Pencil size={16}/> Chỉnh sửa</button>}</div></header>
      <div className="grid lg:grid-cols-[minmax(0,.9fr)_minmax(0,1.1fr)]">
        <div className="relative grid min-h-[320px] place-items-center bg-[radial-gradient(circle_at_50%_35%,#fff_0,#f2f5ff_62%,#e8edfc_100%)] p-8 sm:min-h-[480px]">
          {form.thumbnailUrl || product.thumbnailUrl ? <img src={mediaUrl(editing ? form.thumbnailUrl : product.thumbnailUrl)} alt={product.name} className="max-h-[420px] w-full object-contain drop-shadow-[0_18px_24px_rgba(44,54,98,.12)]"/> : <Package size={88} strokeWidth={1} className="text-indigo-200"/>}
          <span className={`absolute left-6 top-6 rounded-full px-3.5 py-1.5 text-xs font-bold ${product.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : product.status === 'REJECTED' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'}`}>{statusText[product.status]}</span>
          {editing && <label className="absolute inset-x-6 bottom-6 rounded-xl border border-white/80 bg-white/90 p-3 text-xs font-semibold text-slate-600 shadow-sm">URL ảnh đại diện<input value={form.thumbnailUrl} onChange={e => setForm(v => ({ ...v, thumbnailUrl: e.target.value }))} className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" placeholder="https://..."/></label>}
        </div>
        <div className="p-6 sm:p-9 lg:p-10">
          {editing ? <form id="seller-product-edit" onSubmit={save} className="space-y-5"><label className="block text-sm font-semibold text-slate-700">Tên sản phẩm<input required maxLength={255} value={form.name} onChange={e => setForm(v => ({ ...v, name: e.target.value }))} className="mt-2 w-full rounded-xl border border-slate-200 px-4 py-3 text-base"/></label><label className="block text-sm font-semibold text-slate-700">Mã danh mục (UUID)<input required value={form.categoryId} onChange={e => setForm(v => ({ ...v, categoryId: e.target.value }))} className="mt-2 w-full rounded-xl border border-slate-200 px-4 py-3 font-mono text-sm"/></label><label className="block text-sm font-semibold text-slate-700">Mô tả<textarea rows={7} value={form.description} onChange={e => setForm(v => ({ ...v, description: e.target.value }))} className="mt-2 w-full rounded-xl border border-slate-200 px-4 py-3 leading-6"/></label><p className="rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">Giá, SKU, biến thể và tồn kho được quản lý riêng trong phần kho hàng.</p></form> : <>
            <h1 className="break-words text-2xl font-extrabold leading-tight tracking-tight text-[#202642] sm:text-3xl">{product.name}</h1><p className="mt-4 whitespace-pre-line text-sm leading-7 text-slate-600">{product.description || 'Chưa có mô tả cho sản phẩm này.'}</p>
            <div className="mt-7 rounded-2xl bg-[#F5F7FF] p-5"><span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Khoảng giá bán</span><p className="mt-1 text-2xl font-extrabold text-[#5367C9]">{money(product.minPrice)}{product.maxPrice != null && product.maxPrice !== product.minPrice ? <span className="text-base font-semibold"> – {money(product.maxPrice)}</span> : null}</p></div>
            <div className="mt-5 grid grid-cols-2 gap-3"><Info icon={<CircleDollarSign size={18}/>} label="Mã SKU" value={product.sku || 'Chưa có SKU'}/><Info icon={<Boxes size={18}/>} label="Tồn kho" value={`${product.totalStock ?? 0} sản phẩm`}/><Info icon={<Tag size={18}/>} label="Danh mục" value={product.categoryName || 'Chưa phân loại'}/><Info icon={<Package size={18}/>} label="Số biến thể" value={String(product.variantCount ?? 0)}/><Info icon={<CalendarDays size={18}/>} label="Ngày tạo" value={product.createdAt ? new Date(product.createdAt).toLocaleString('vi-VN') : '—'}/><Info icon={<Store size={18}/>} label="Trạng thái" value={statusText[product.status]}/><Info icon={<Tag size={18}/>} label="Đường dẫn sản phẩm" value={product.slug || '—'}/><Info icon={<Package size={18}/>} label="Nổi bật" value={product.featured ? 'Có' : 'Không'}/><Info icon={<CircleDollarSign size={18}/>} label="Đánh giá" value={`${product.ratingAverage ?? 0} / 5 · ${product.ratingCount ?? 0} lượt`}/><Info icon={<CalendarDays size={18}/>} label="Cập nhật gần nhất" value={product.updatedAt ? new Date(product.updatedAt).toLocaleString('vi-VN') : '—'}/></div>
            <Link to={`/seller/inventory/${product.id}`} className="mt-7 inline-flex items-center gap-2 rounded-xl border border-indigo-100 bg-indigo-50 px-5 py-3 text-sm font-bold text-indigo-700"><Boxes size={17}/> Chi tiết tồn kho</Link>
          </>}
        </div>
      </div>
    </section>
  </main>;
}

function Info({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) { return <div className="min-w-0 rounded-2xl border border-slate-100 p-4"><span className="text-indigo-500">{icon}</span><span className="mt-3 block text-xs text-slate-500">{label}</span><b className="mt-1 block break-words text-sm text-[#202642]">{value}</b></div>; }
