import { useEffect, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { ArrowUpRight, ImagePlus, MapPin, MessageSquareText, Package, Phone, Star, Store } from 'lucide-react';
import { Link } from 'react-router-dom';
import { shopApi } from '../../services/shop/shopApi';
import type { ShopResponse } from '../../services/shop/shopTypes';
import { getApiError } from '../../services/checkout/checkoutApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { sellerProductApi } from '../../services/seller/productApi';
import { reviewApi, type ProductReview } from '../../services/reviews/reviewApi';

type ShopForm = { shopName: string; phone: string; email: string; address: string; logoUrl: string; bannerUrl: string };
const imageSource = mediaUrl;

export default function SellerShopInfoPage() {
  const [shop, setShop] = useState<ShopResponse | null>(null);
  const [form, setForm] = useState<ShopForm>({ shopName: '', phone: '', email: '', address: '', logoUrl: '', bannerUrl: '' });
  const [error, setError] = useState(''); const [saved, setSaved] = useState(false); const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState<'logo' | 'banner' | null>(null);
  const [previews, setPreviews] = useState<Partial<Record<'logo' | 'banner', string>>>({});
  const [productCount, setProductCount] = useState<number | null>(null);
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  useEffect(() => { void shopApi.getMyShop().then(value => { setShop(value); setForm({ shopName: value.shopName || '', phone: value.phone || '', email: value.email || '', address: value.address || '', logoUrl: value.logoUrl || '', bannerUrl: value.bannerUrl || '' }); }).catch(e => setError(getApiError(e))); }, []);
  useEffect(() => { void Promise.all([sellerProductApi.stats(), reviewApi.ownerList()]).then(([stats, list]) => { setProductCount(stats.all); setReviews(list); }).catch(() => {}); }, []);
  const submit = async (event: FormEvent) => { event.preventDefault(); setBusy(true); setError(''); setSaved(false); try { const value = await shopApi.updateMyShop(form); setShop(value); setSaved(true); } catch (e) { setError(getApiError(e)); } finally { setBusy(false); } };
  const upload = async (kind: 'logo' | 'banner', event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; event.target.value = ''; if (!file) return;
    const localUrl = URL.createObjectURL(file);
    setPreviews(current => { if (current[kind]) URL.revokeObjectURL(current[kind]!); return { ...current, [kind]: localUrl }; });
    setUploading(kind); setError('');
    try {
      const updated = await shopApi.uploadImage(kind, file); setShop(updated);
      setForm(current => ({ ...current, [kind === 'logo' ? 'logoUrl' : 'bannerUrl']: kind === 'logo' ? updated.logoUrl || '' : updated.bannerUrl || '' }));
      setPreviews(current => { if (current[kind]) URL.revokeObjectURL(current[kind]!); return { ...current, [kind]: undefined }; });
    } catch (e) { setError(getApiError(e)); setPreviews(current => { if (current[kind]) URL.revokeObjectURL(current[kind]!); return { ...current, [kind]: undefined }; }); }
    finally { setUploading(null); }
  };
  const field = (key: 'shopName' | 'phone' | 'email', label: string, type = 'text', icon?: typeof Store) => { const Icon = icon; return <label className="grid gap-1.5 text-xs font-bold uppercase tracking-wide text-slate-500">{label}<span className="relative"><input type={type} value={form[key]} onChange={e => setForm(current => ({ ...current, [key]: e.target.value }))} className="w-full px-3 py-3 text-sm font-medium normal-case tracking-normal" />{Icon && <Icon size={16} className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"/>}</span></label>; };
  const uploadButton = (kind: 'logo' | 'banner') => <label className={`inline-flex cursor-pointer items-center gap-2 rounded-xl px-3.5 py-2.5 text-xs font-bold transition ${kind === 'banner' ? 'border border-white/50 bg-black/30 text-white backdrop-blur hover:bg-black/45' : 'border border-indigo-100 bg-indigo-50 text-[#5367C9] hover:bg-indigo-100'}`}><ImagePlus size={15}/>{uploading === kind ? 'Đang tải ảnh...' : kind === 'logo' ? 'Đổi logo' : 'Đổi ảnh bìa'}<input type="file" accept="image/jpeg,image/png,image/webp,image/gif" className="hidden" disabled={uploading !== null} onChange={e => void upload(kind, e)} /></label>;

  return <div className="seller-container shop-management !max-w-[1180px]">
    <header className="mb-6 flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-indigo-50 text-[#5367C9]"><Store size={23}/></span><div><h1>Thông tin cửa hàng</h1><p className="mt-1">Quản lý diện mạo và thông tin liên hệ của gian hàng.</p></div></header>
    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}{saved && <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 p-3.5 text-sm text-emerald-700">Thông tin cửa hàng đã được cập nhật.</div>}
    <section className="mb-5 overflow-hidden rounded-2xl border border-[#e4e8f1] bg-white shadow-sm"><div className="relative h-44 bg-gradient-to-r from-[#344782] via-[#667bd0] to-[#b7c3f4] sm:h-56">{(previews.banner || imageSource(shop?.bannerUrl)) && <img src={previews.banner || imageSource(shop?.bannerUrl)} alt="Ảnh bìa cửa hàng" className="absolute inset-0 h-full w-full object-cover"/>}<div className="absolute inset-0 bg-gradient-to-r from-slate-950/40 via-slate-900/10 to-transparent"/><div className="absolute bottom-4 left-5 flex items-end gap-3 sm:bottom-5 sm:left-7"><span className="grid h-12 w-12 place-items-center rounded-xl bg-white/20 text-white backdrop-blur"><Store size={22}/></span><div className="text-white"><span className="block text-[10px] font-bold uppercase tracking-[.18em] text-white/70">Gian hàng Kala</span><b className="mt-0.5 block text-xl sm:text-2xl">{shop?.shopName || 'Cửa hàng của bạn'}</b></div></div><div className="absolute bottom-4 right-4 sm:bottom-5 sm:right-6">{uploadButton('banner')}</div></div>
      <div className="flex flex-wrap items-center gap-4 px-5 py-4 sm:px-7"><span className="grid h-12 w-12 place-items-center overflow-hidden rounded-xl border border-slate-200 bg-[#F2F4FC] text-[#5367C9]">{(previews.logo || imageSource(shop?.logoUrl)) ? <img src={previews.logo || imageSource(shop?.logoUrl)} alt="Logo cửa hàng" className="h-full w-full object-cover"/> : <Store size={22}/>}</span><div className="min-w-0 flex-1"><b className="block truncate text-sm text-[#202642]">{shop?.shopName || 'Tên cửa hàng'}</b><span className="mt-0.5 block text-xs text-slate-500">Logo vuông giúp khách dễ nhận diện shop.</span></div>{uploadButton('logo')}</div>
    </section>

    <form onSubmit={e => void submit(e)} className="grid items-start gap-5 rounded-2xl border bg-white p-5 sm:p-7 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div><div className="mb-5 border-b border-slate-100 pb-4"><h2 className="font-extrabold text-[#202642]">Thông tin cơ bản</h2><p className="mt-1 text-xs text-slate-500">Các thông tin này sẽ xuất hiện trên trang cửa hàng của bạn.</p></div><div className="grid gap-4 sm:grid-cols-2">{field('shopName', 'Tên cửa hàng', 'text', Store)}{field('phone', 'Số điện thoại', 'tel', Phone)}{field('email', 'Email liên hệ', 'email')}<label className="grid gap-1.5 text-xs font-bold uppercase tracking-wide text-slate-500 sm:col-span-2">Địa chỉ cửa hàng<span className="relative"><textarea value={form.address} onChange={e => setForm(current => ({ ...current, address: e.target.value }))} className="min-h-24 w-full resize-y px-3 py-3 pr-10 text-sm font-medium normal-case tracking-normal"/><MapPin size={16} className="pointer-events-none absolute right-3 top-3 text-slate-400"/></span></label></div><div className="mt-5 flex items-center justify-between gap-3 border-t border-slate-100 pt-4"><span className="text-xs text-slate-500">Thay đổi sẽ được lưu ngay vào hồ sơ shop.</span><button disabled={busy || uploading !== null} className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50">{busy ? 'Đang lưu...' : 'Lưu thay đổi'}</button></div></div>
      <aside className="rounded-2xl bg-[#F7F8FC] p-4"><span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Trạng thái shop</span><div className="mt-2 inline-flex rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">{shop?.status || 'Đang tải...'}</div><div className="my-4 border-t border-slate-200"/><span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Địa chỉ trang shop</span><div className="mt-2 break-all rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-[#5367C9]">{shop?.slug ? `/shop/${shop.slug}` : 'Đang tải...'}</div><p className="mt-3 text-xs leading-5 text-slate-500">Ảnh logo và ảnh bìa được tải trực tiếp từ thiết bị. Dùng ảnh JPG, PNG, GIF hoặc WebP tối đa 5 MB.</p></aside>
    </form>
    <section className="mb-5 mt-6 rounded-2xl border border-[#e4e8f1] bg-white p-5 shadow-sm"><div className="mb-4 flex items-center justify-between gap-3"><div><h2 className="font-bold text-[#202642]">Đánh giá từ khách hàng</h2><p className="mt-1 text-xs text-slate-500">Tổng hợp đánh giá của các sản phẩm thuộc cửa hàng.</p></div><Link to="/shop-owner/reviews" className="inline-flex shrink-0 items-center gap-1 text-xs font-bold text-indigo-600">Quản lý đánh giá <ArrowUpRight size={14}/></Link></div>{reviews.length ? <div className="grid gap-3 md:grid-cols-2">{reviews.slice(0, 4).map(review => <article key={review.id} className="rounded-xl bg-slate-50 p-4"><div className="flex items-center justify-between gap-3"><b className="truncate text-sm text-slate-800">{review.customerName || 'Khách hàng'}</b><span className="inline-flex shrink-0 items-center gap-1 text-xs font-bold text-amber-600"><Star size={13} fill="currentColor"/>{review.rating}/5</span></div><p className="mt-1 truncate text-xs font-semibold text-indigo-600">{review.productName}</p><p className="mt-2 line-clamp-2 text-xs leading-5 text-slate-600">{review.content || 'Khách hàng không để lại nội dung.'}</p></article>)}</div> : <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">Cửa hàng chưa có đánh giá.</p>}</section>
    <section className="mb-2 grid gap-3 sm:grid-cols-3"><ShopMetric icon={<Package size={19}/>} label="Tổng sản phẩm" value={productCount == null ? '—' : String(productCount)}/><ShopMetric icon={<Star size={19}/>} label="Đánh giá trung bình" value={reviews.length ? `${(reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length).toFixed(1)} / 5` : 'Chưa có đánh giá'}/><ShopMetric icon={<MessageSquareText size={19}/>} label="Lượt đánh giá" value={String(reviews.length)}/></section>
  </div>;
}

function ShopMetric({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) { return <div className="flex items-center gap-3 rounded-2xl border border-[#e4e8f1] bg-white p-4 shadow-sm"><span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-600">{icon}</span><div><span className="block text-xs text-slate-500">{label}</span><b className="mt-1 block text-lg text-slate-900">{value}</b></div></div>; }
