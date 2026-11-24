import { useDialog, useToast } from '../../context/FeedbackContext';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft, BarChart3, Eye, EyeOff, Image as ImageIcon,
  LayoutGrid, Maximize2, MoreVertical, Package, Pencil, Play, Save, Star, Store, Trash2,
  Video as VideoIcon, Warehouse, X,
} from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { inventoryApi } from '../../services/seller/inventoryApi';
import {
  productDetailApi,
  type ProductMediaDto,
  type ProductOptionDto,
  type ProductOptionValueDto,
  type ProductVariantDto,
  type ProductVariantPage,
} from '../../services/seller/productDetailApi';
import { reviewApi, reviewDate, type ProductReview } from '../../services/reviews/reviewApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import SellerPagination from '../../components/seller/common/SellerPagination';

/* ---------- helpers ---------- */

type ProductWithTags = SellerProduct & { tags?: Array<string | { id?: string; name: string }> };
type Tab = 'variants' | 'reviews' | 'description' | 'other';
type GalleryItem = { id: string; type: 'IMAGE' | 'VIDEO'; url: string };

const statusText: Record<SellerProduct['status'], string> = {
  ACTIVE: 'Đang bán', INACTIVE: 'Tạm ẩn', PENDING: 'Chờ duyệt', REJECTED: 'Từ chối',
};
const statusStyle: Record<SellerProduct['status'], string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-700',
  INACTIVE: 'bg-slate-200 text-slate-600',
  PENDING: 'bg-[#FDEFB2] text-[#8A6100]',
  REJECTED: 'bg-rose-100 text-rose-700',
};

const money = (v: number | null | undefined) =>
  v == null ? '—' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(v);

const moneyRange = (values: Array<number | null | undefined>) => {
  const nums = values.filter((v): v is number => v != null);
  if (!nums.length) return '—';
  const min = Math.min(...nums);
  const max = Math.max(...nums);
  return min === max ? money(min) : `${money(min)} – ${money(max)}`;
};
const discountPercent = (price?: number | null, comparePrice?: number | null) =>
  price != null && comparePrice != null && comparePrice > price && comparePrice > 0
    ? Math.round((1 - price / comparePrice) * 100)
    : null;

const dateText = (v?: string | null) => (v ? new Date(v).toLocaleString('vi-VN') : '—');

const errorMessage = (e: unknown) => {
  const err = e as { response?: { data?: { message?: string } }; message?: string };
  return err.response?.data?.message || err.message || 'Không thể xử lý yêu cầu.';
};

const duration = (s: number) => {
  const total = Math.max(0, Math.round(s));
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
};

const SWATCHES: Record<string, string> = {
  'đen': '#111111', 'trắng': '#FFFFFF', 'xanh': '#1D4ED8', 'xanh dương': '#1D4ED8',
  'xanh lam': '#1D4ED8', 'xanh lá': '#22C55E', 'xanh lục': '#22C55E', 'đỏ': '#EF4444',
  'vàng': '#FACC15', 'hồng': '#F472B6', 'tím': '#A855F7', 'cam': '#F97316',
  'xám': '#9CA3AF', 'nâu': '#92400E', 'be': '#E8D9B8', 'kem': '#F5E6C8',
};
const swatchOf = (value: string) => SWATCHES[value.trim().toLowerCase()];

/* ---------- page ---------- */

export default function SellerProductDetailPage() {
  const toast=useToast();const {confirm}=useDialog();
  const { productId = '' } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState<ProductWithTags | null>(null);
  const [options, setOptions] = useState<ProductOptionDto[]>([]);
  const [optionValues, setOptionValues] = useState<ProductOptionValueDto[]>([]);
  const [variants, setVariants] = useState<ProductVariantDto[]>([]);
  const [variantMeta, setVariantMeta] = useState<Omit<ProductVariantPage, 'items'>>({
    page: 0, size: 10, totalElements: 0, totalPages: 0, last: true,
    minPrice: null, maxPrice: null, minComparePrice: null, maxComparePrice: null,
    minCostPrice: null, maxCostPrice: null,
  });
  const [variantsLoading, setVariantsLoading] = useState(false);
  const pagingRef = useRef({ page: 0, size: 10 });
  const [media, setMedia] = useState<ProductMediaDto[]>([]);
  const [stockByVariant, setStockByVariant] = useState<Record<string, number>>({});
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [reviewsError, setReviewsError] = useState('');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const [tab, setTab] = useState<Tab>('description');
  const [activeIdx, setActiveIdx] = useState(0);
  const [lightbox, setLightbox] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', categoryId: '', thumbnailUrl: '' });

  const fillForm = (p: SellerProduct) =>
    setForm({ name: p.name, description: p.description || '', categoryId: p.categoryId || '', thumbnailUrl: p.thumbnailUrl || '' });

  const applyVariantPage = useCallback((r: ProductVariantPage) => {
    const { items, ...meta } = r;
    pagingRef.current = { page: meta.page, size: meta.size };
    setVariants(items);
    setVariantMeta(meta);
  }, []);

  const fetchVariantPage = useCallback(async (page: number, size: number) => {
    let r = await productDetailApi.variantsPage(productId, page, size);
    if (r.items.length === 0 && r.totalPages > 0 && page >= r.totalPages) r = await productDetailApi.variantsPage(productId, r.totalPages - 1, size);
    applyVariantPage(r);
  }, [productId, applyVariantPage]);

  const changeVariantPage = async (page: number, size = pagingRef.current.size) => {
    setVariantsLoading(true);
    try { await fetchVariantPage(page, size); } catch (e) { setError(errorMessage(e)); } finally { setVariantsLoading(false); }
  };

  const loadAll = useCallback(async () => {
    const item = await sellerProductApi.get(productId);
    setProduct(item);
    fillForm(item);

    // Các phần phụ: lỗi một phần không làm hỏng cả trang.
    const [opts, vars, med, inv, rev] = await Promise.allSettled([
      productDetailApi.options(productId),
      productDetailApi.variantsPage(productId, pagingRef.current.page, pagingRef.current.size),
      productDetailApi.media(productId),
      inventoryApi.listProduct(productId),
      reviewApi.list(productId),
    ]);
    setReviews(rev.status === 'fulfilled' ? rev.value : []);
    setReviewsError(rev.status === 'rejected' ? errorMessage(rev.reason) : '');
    const optionList = opts.status === 'fulfilled' ? opts.value : [];
    setOptions(optionList);
    if (vars.status === 'fulfilled') {
      let r = vars.value;
      if (r.items.length === 0 && r.totalPages > 0 && r.page >= r.totalPages) r = await productDetailApi.variantsPage(productId, r.totalPages - 1, r.size);
      applyVariantPage(r);
    } else { setVariants([]); }
    setMedia(med.status === 'fulfilled' ? med.value : []);
    setStockByVariant(
      inv.status === 'fulfilled' ? Object.fromEntries(inv.value.map(i => [i.variantId, i.quantity])) : {},
    );

    const valueLists = await Promise.allSettled(optionList.map(o => productDetailApi.optionValues(o.id)));
    setOptionValues(valueLists.flatMap(r => (r.status === 'fulfilled' ? r.value : [])));
  }, [productId, applyVariantPage]);

  useEffect(() => {
    let active = true;
    pagingRef.current = { page: 0, size: pagingRef.current.size };
    setLoading(true);
    setError('');
    setActiveIdx(0);
    void loadAll()
      .catch(e => { if (active) setError(errorMessage(e)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loadAll]);

  useEffect(() => {
    if (!menuOpen) return;
    const close = (e: MouseEvent) => { if (!menuRef.current?.contains(e.target as Node)) setMenuOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [menuOpen]);

  useEffect(() => {
    if (!lightbox) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setLightbox(false); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [lightbox]);

  /* ---------- dữ liệu dẫn xuất ---------- */

  const gallery: GalleryItem[] = useMemo(() => {
    const items = [...media]
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
      .map(m => ({ id: m.id, type: m.mediaType, url: mediaUrl(m.mediaUrl) }));
    if (!items.length && product?.thumbnailUrl) {
      items.push({ id: 'thumb', type: 'IMAGE', url: mediaUrl(product.thumbnailUrl) });
    }
    return items;
  }, [media, product?.thumbnailUrl]);

  const current = gallery[Math.min(activeIdx, Math.max(gallery.length - 1, 0))];
  const imageCount = media.filter(m => m.mediaType === 'IMAGE').length || (media.length ? 0 : gallery.length);
  const videoCount = media.filter(m => m.mediaType === 'VIDEO').length;

  const colorOptionId = options.find(o => /màu|color|colour/i.test(o.name))?.id;
  const valueById = useMemo(() => new Map(optionValues.map(v => [v.id, v])), [optionValues]);

  const variantRows = useMemo(() => variants.map(v => {
    const parts = options
      .map(o => v.optionValueIds.map(id => valueById.get(id)).find(x => x?.optionId === o.id))
      .filter((x): x is ProductOptionValueDto => Boolean(x));
    const colorValue = parts.find(p => p.optionId === colorOptionId)?.value;
    return {
      ...v,
      label: parts.length ? parts.map(p => p.value).join(' / ') : v.sku || 'Mặc định',
      swatch: colorValue ? swatchOf(colorValue) : undefined,
      showSwatch: Boolean(colorValue),
      stock: stockByVariant[v.id] ?? v.stockQuantity ?? 0,
    };
  }), [variants, options, valueById, colorOptionId, stockByVariant]);

  const tags = (product?.tags ?? []).map(t => (typeof t === 'string' ? t : t.name));

  /* ---------- hành động ---------- */

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      await sellerProductApi.update(productId, { ...form, categoryId: form.categoryId.trim() });
      await loadAll();
      setEditing(false);
      toast.success('Đã cập nhật thông tin sản phẩm.');
    } catch (err) { setError(errorMessage(err)); toast.error(errorMessage(err)); } finally { setBusy(false); }
  };

  const toggleVisibility = async () => {
    if (!product) return;
    setMenuOpen(false); setBusy(true); setError('');
    try {
      await sellerProductApi.status(productId, product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
      await loadAll();
      toast.success('Đã cập nhật trạng thái sản phẩm.');
    } catch (err) { setError(errorMessage(err)); toast.error(errorMessage(err)); } finally { setBusy(false); }
  };

  const remove = async () => {
    setMenuOpen(false);
    await confirm({title:'Xóa sản phẩm?',message:'Bạn chắc chắn muốn xóa sản phẩm này? Hành động này không thể hoàn tác.',confirmText:'Xóa sản phẩm',variant:'danger',loadingText:'Đang xóa sản phẩm...',successMessage:'Đã xóa sản phẩm.',onConfirm:async()=>{await sellerProductApi.remove(productId);navigate('/shop-owner/products',{replace:true});}});
  };

  const replyReview = async (review: ProductReview, content: string) => {
    await reviewApi.reply(review.id, content);
    setReviews(await reviewApi.list(productId));
  };

  /* ---------- render ---------- */

  if (loading) return <main className="seller-container py-16 text-center text-sm text-slate-500">Đang tải chi tiết sản phẩm...</main>;
  if (error && !product) {
    return <main className="seller-container py-12">
      <Link to="/shop-owner/products" className="inline-flex items-center gap-2 text-sm font-semibold text-[#5367C9]"><ArrowLeft size={17} /> Quay lại sản phẩm</Link>
      <p className="mt-6 rounded-2xl bg-rose-50 p-5 text-sm text-rose-700">{error}</p>
    </main>;
  }
  if (!product) return null;

  const canToggle = product.status === 'ACTIVE' || product.status === 'INACTIVE';
  const optionSummary = options.length ? `${options.length} thuộc tính` : 'Không có';
  const optionNames = options.map(o => o.name).join(', ');
  const summaryDiscount = discountPercent(variantMeta.minPrice ?? product.minPrice, variantMeta.minComparePrice);

  return <main className="seller-container pb-12 pt-5" style={{ maxWidth: 1200 }}>
{/* Tiêu đề + hành động */}
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-3">
        <button type="button" onClick={() => navigate('/shop-owner/products')} aria-label="Quay lại"
          className="grid h-10 w-10 place-items-center rounded-full bg-white text-slate-700 shadow-sm transition hover:bg-slate-50"><ArrowLeft size={18} /></button>
        <h1 className="text-[26px] font-semibold tracking-tight text-[#1B2559]">Chi tiết sản phẩm</h1>
      </div>
      <div className="flex items-center gap-2.5">
        {editing ? <>
          <button type="button" onClick={() => { setEditing(false); fillForm(product); setError(''); }}
            className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-slate-600 shadow-sm"><X size={16} /> Hủy</button>
          <button form="seller-product-edit" disabled={busy}
            className="inline-flex items-center gap-2 rounded-xl bg-[#5367C9] px-4 py-2.5 text-sm font-semibold text-white shadow-sm disabled:opacity-50"><Save size={16} />{busy ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
        </> : <>
          <button type="button" onClick={() => navigate(`/shop-owner/products/${product.id}/edit`, { state: { returnTo: `/shop-owner/products/${product.id}` } })}
            className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-[#1B2559] shadow-sm transition hover:bg-slate-50"><Pencil size={15} className="text-[#5367C9]" /> Sửa</button>
        </>}
        <div className="relative" ref={menuRef}>
          <button type="button" aria-label="Thêm" aria-haspopup="menu" aria-expanded={menuOpen} onClick={() => setMenuOpen(o => !o)}
            className="grid h-10 w-10 place-items-center rounded-xl bg-white text-slate-700 shadow-sm transition hover:bg-slate-50"><MoreVertical size={18} /></button>
          {menuOpen && <div role="menu" className="absolute right-0 top-[calc(100%+6px)] z-30 min-w-[210px] rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl">
            <Link role="menuitem" to={`/shop-owner/inventory/${product.id}`} className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-slate-700 hover:bg-slate-50"><Warehouse size={16} /> Chi tiết tồn kho</Link>
            {canToggle && <button role="menuitem" type="button" onClick={toggleVisibility} className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm text-slate-700 hover:bg-slate-50">
              {product.status === 'ACTIVE' ? <><EyeOff size={16} /> Ẩn sản phẩm</> : <><Eye size={16} /> Hiển thị sản phẩm</>}</button>}
            <button role="menuitem" type="button" onClick={remove} className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm text-rose-600 hover:bg-rose-50"><Trash2 size={16} /> Xóa sản phẩm</button>
          </div>}
        </div>
      </div>
    </div>


    {error && <p className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}

    {/* Hàng trên: thông tin chính + tổng quan */}
    <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_380px]">
      <section className="rounded-2xl bg-white p-4 shadow-[0_4px_20px_rgba(27,37,89,.06)] sm:p-5">
        <div className="grid gap-5 md:grid-cols-[minmax(0,330px)_minmax(0,1fr)]">
          {/* Thư viện ảnh */}
          <div className="flex gap-3">
            <div className="flex w-[66px] shrink-0 flex-col gap-2.5">
              {gallery.map((g, i) => <button key={g.id} type="button" onClick={() => setActiveIdx(i)} aria-label={`Xem ${g.type === 'VIDEO' ? 'video' : 'ảnh'} ${i + 1}`}
                className={`relative h-[60px] w-[66px] overflow-hidden rounded-lg bg-[#F1F4FD] ring-2 transition ${i === activeIdx ? 'ring-[#5367C9]' : 'ring-transparent hover:ring-slate-200'}`}>
                {g.type === 'VIDEO' ? <VideoTile src={g.url} /> : <img src={g.url} alt="" className="h-full w-full object-cover" />}
              </button>)}
            </div>
            <div className="relative min-h-[300px] min-w-0 flex-1 overflow-hidden rounded-xl bg-[#F1F4FD]">
              {current ? (current.type === 'VIDEO'
                ? <video key={current.id} src={current.url} controls className="absolute inset-0 h-full w-full object-contain" />
                : <img src={current.url} alt={product.name} className="absolute inset-0 h-full w-full object-contain p-3" />)
                : <span className="absolute inset-0 grid place-items-center"><Package size={72} strokeWidth={1} className="text-indigo-200" /></span>}
              {current && <button type="button" onClick={() => setLightbox(true)} aria-label="Phóng to"
                className="absolute bottom-3 right-3 grid h-10 w-10 place-items-center rounded-xl bg-white/90 text-slate-800 shadow transition hover:bg-white"><Maximize2 size={18} /></button>}
            </div>
          </div>

          {/* Thông tin / form sửa */}
          <div className="min-w-0">
            {editing ? <form id="seller-product-edit" onSubmit={save} className="space-y-4">
              <label className="block text-sm font-semibold text-slate-700">Tên sản phẩm
                <input required maxLength={255} value={form.name} onChange={e => setForm(v => ({ ...v, name: e.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-base" /></label>
              <label className="block text-sm font-semibold text-slate-700">Mã danh mục (UUID)
                <input required value={form.categoryId} onChange={e => setForm(v => ({ ...v, categoryId: e.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-200 px-4 py-2.5 font-mono text-sm" /></label>
              <label className="block text-sm font-semibold text-slate-700">URL ảnh đại diện
                <input value={form.thumbnailUrl} onChange={e => setForm(v => ({ ...v, thumbnailUrl: e.target.value }))} placeholder="https://..." className="mt-1.5 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm" /></label>
              <label className="block text-sm font-semibold text-slate-700">Mô tả
                <textarea rows={5} value={form.description} onChange={e => setForm(v => ({ ...v, description: e.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-200 px-4 py-2.5 leading-6" /></label>
              <p className="rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">Giá, SKU, biến thể và tồn kho được quản lý riêng trong phần kho hàng.</p>
            </form> : <>
              <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${statusStyle[product.status]}`}>
                <Store size={14} className="text-[#5367C9]" />{statusText[product.status]}
              </span>
              <h2 className="mt-3 break-words text-[28px] font-semibold leading-tight tracking-tight text-[#1B2559]">{product.name}</h2>

              <div className="mt-5 grid grid-cols-2 gap-3">
                <div className="min-w-0 rounded-xl bg-slate-50 p-3 sm:p-4"><p className="text-xs font-medium text-slate-500">Giá bán</p><div className="mt-2 flex flex-wrap items-center gap-2"><p className="break-words text-base font-semibold text-[#5367C9] sm:text-lg">{moneyRange(variantMeta.totalElements ? [variantMeta.minPrice, variantMeta.maxPrice] : [product.minPrice, product.maxPrice])}</p>{summaryDiscount != null && <span className="rounded-full bg-rose-50 px-2 py-1 text-xs font-medium text-rose-600">-{summaryDiscount}%</span>}</div></div>
                <div className="min-w-0 rounded-xl bg-slate-50 p-3 sm:p-4"><p className="text-xs font-medium text-slate-500">Giá so sánh</p><p className="mt-2 break-words text-base font-medium text-slate-700 sm:text-lg">{moneyRange([variantMeta.minComparePrice, variantMeta.maxComparePrice])}</p></div>
                <div className="min-w-0 rounded-xl bg-slate-50 p-3 sm:p-4"><p className="text-xs font-medium text-slate-500">Giá gốc (giá nhập)</p><p className="mt-2 break-words text-base font-medium text-slate-700 sm:text-lg">{moneyRange([variantMeta.minCostPrice, variantMeta.maxCostPrice])}</p></div>
                <div className="min-w-0 rounded-xl bg-slate-50 p-3 sm:p-4"><p className="text-xs font-medium text-slate-500">Tổng tồn kho</p><p className="mt-2 break-words text-base font-medium text-slate-700 sm:text-lg">{(product.totalStock ?? 0).toLocaleString('vi-VN')} sản phẩm</p></div>
              </div>

              {tags.length > 0 && <div className="mt-4 flex flex-wrap gap-2">
                {tags.map(t => <span key={t} className="rounded-full bg-[#E8ECFB] px-3.5 py-1.5 text-[13px] font-medium text-[#5367C9]">{t}</span>)}
              </div>}
            </>}
          </div>
        </div>
      </section>

      {/* Tổng quan */}
      <aside className="rounded-2xl bg-white p-5 shadow-[0_4px_20px_rgba(27,37,89,.06)]">
        <div className="mb-3 flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-[#E8ECFB] text-[#5367C9]"><BarChart3 size={20} /></span>
          <h3 className="text-xl font-semibold text-[#1B2559]">Tổng quan</h3>
        </div>
        <dl className="divide-y divide-slate-100">
          <OverviewRow icon={<Package size={18} />} label="Phân loại" value={optionSummary} hint={optionNames} />
          <OverviewRow icon={<LayoutGrid size={18} />} label="Biến thể" value={`${variantMeta.totalElements || product.variantCount || 0} biến thể`} />
          <OverviewRow icon={<ImageIcon size={18} />} label="Hình ảnh" value={`${imageCount} ảnh`} />
          <OverviewRow icon={<VideoIcon size={18} />} label="Video" value={`${videoCount} video`} />
          <OverviewRow icon={<Warehouse size={18} />} label="Tổng tồn kho" value={`${product.totalStock ?? 0} sản phẩm`} />
        </dl>
      </aside>
    </div>

    {/* Tabs */}
    <section className="mt-5 rounded-2xl bg-white px-5 pb-5 pt-2 shadow-[0_4px_20px_rgba(27,37,89,.06)]">
      <div role="tablist" className="flex gap-8 overflow-x-auto border-b border-slate-200">
        {([['description', 'Thông tin mô tả'], ['variants', `Biến thể (${variantMeta.totalElements})`], ['reviews', `Đánh giá (${reviews.length})`], ['other', 'Thông tin khác']] as Array<[Tab, string]>).map(([key, label]) =>
          <button key={key} role="tab" aria-selected={tab === key} type="button" onClick={() => setTab(key)}
            className={`-mb-px whitespace-nowrap border-b-2 px-3 py-3.5 text-[15px] font-semibold transition ${tab === key ? 'border-[#5367C9] text-[#5367C9]' : 'border-transparent font-medium text-[#3B4BA8] hover:text-[#5367C9]'}`}>{label}</button>)}
      </div>

      {tab === 'variants' && <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        {variantMeta.totalElements === 0 ? <p className="p-6 text-center text-sm text-slate-500">Sản phẩm chưa có biến thể.</p> :
          <table className="w-full min-w-[760px] text-left text-[15px]">
            <thead><tr className="bg-slate-50 text-[#1B2559]">
              <th className="px-5 py-3 font-semibold">Biến thể</th><th className="px-3 py-3 font-semibold">SKU</th>
              <th className="px-3 py-3 font-semibold">Giá bán</th><th className="px-3 py-3 font-semibold">Giá so sánh</th>
              <th className="px-3 py-3 text-center font-semibold">Tồn kho</th>
            </tr></thead>
            <tbody>{variantRows.map(v => <tr key={v.id} className={`border-t border-slate-100 text-slate-800 hover:bg-slate-50/60 ${v.status === 'INACTIVE' ? 'opacity-60' : ''}`}>
              <td className="px-5 py-2.5"><span className="flex items-center gap-3">
                {v.showSwatch
                  ? <span className="h-[18px] w-[18px] shrink-0 rounded border border-slate-300" style={{ background: v.swatch ?? '#E5E7EB' }} />
                  : <span className="w-[18px] shrink-0" />}
                {v.label}{v.status === 'INACTIVE' && <span className="rounded bg-slate-200 px-1.5 py-0.5 text-[11px] text-slate-600">Ẩn</span>}
              </span></td>
              <td className="px-3 py-2.5 font-medium tracking-tight">{v.sku || '—'}</td>
              <td className="px-3 py-2.5"><span className="inline-flex items-center gap-2">{money(v.price)}{discountPercent(v.price,v.comparePrice) != null && <span className="rounded-full bg-rose-50 px-1.5 py-0.5 text-[11px] font-medium text-rose-600">-{discountPercent(v.price,v.comparePrice)}%</span>}</span></td>
              <td className="px-3 py-2.5">{money(v.comparePrice)}</td>
              <td className="px-3 py-2.5 text-center">{v.stock}</td>
            </tr>)}</tbody>
          </table>}
        {variantMeta.totalElements > 0 && <SellerPagination page={variantMeta.page} size={variantMeta.size} totalElements={variantMeta.totalElements} totalPages={variantMeta.totalPages}
          disabled={variantsLoading} onPage={n => void changeVariantPage(n)} onSize={n => void changeVariantPage(0, n)} sizes={[5, 10, 20, 50]} />}
      </div>}

      {tab === 'reviews' && <ReviewsPanel reviews={reviews} error={reviewsError} onReply={replyReview} />}

      {tab === 'description' && <p className="mt-5 whitespace-pre-line text-sm leading-7 text-slate-700">{product.description || 'Chưa có mô tả cho sản phẩm này.'}</p>}

      {tab === 'other' && <dl className="mt-4 grid gap-x-10 sm:grid-cols-2">
        <OtherRow label="Mã SKU" value={product.sku || 'Chưa có SKU'} />
        <OtherRow label="Danh mục" value={product.categoryName || 'Chưa phân loại'} />
        <OtherRow label="Đường dẫn sản phẩm" value={product.slug || '—'} />
        <OtherRow label="Nổi bật" value={product.featured ? 'Có' : 'Không'} />
        <OtherRow label="Đánh giá" value={`${product.ratingAverage ?? 0} / 5 · ${product.ratingCount ?? 0} lượt`} />
        <OtherRow label="Trạng thái" value={statusText[product.status]} />
        <OtherRow label="Ngày tạo" value={dateText(product.createdAt)} />
        <OtherRow label="Cập nhật gần nhất" value={dateText(product.updatedAt)} />
      </dl>}
    </section>

    {/* Lightbox */}
    {lightbox && current && <div role="dialog" aria-modal="true" onClick={() => setLightbox(false)} className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-6">
      <button type="button" aria-label="Đóng" onClick={() => setLightbox(false)} className="absolute right-5 top-5 grid h-10 w-10 place-items-center rounded-full bg-white/90 text-slate-900"><X size={20} /></button>
      <div onClick={e => e.stopPropagation()} className="max-h-full max-w-5xl">
        {current.type === 'VIDEO'
          ? <video src={current.url} controls autoPlay className="max-h-[85vh] max-w-full rounded-xl" />
          : <img src={current.url} alt={product.name} className="max-h-[85vh] max-w-full rounded-xl object-contain" />}
      </div>
    </div>}
  </main>;
}

/* ---------- component phụ ---------- */

function OverviewRow({ icon, label, value, hint }: { icon: React.ReactNode; label: string; value: string; hint?: string }) {
  return <div className="flex items-center gap-3 py-3">
    <span className="text-slate-700">{icon}</span>
    <dt className="w-[120px] shrink-0 whitespace-nowrap font-semibold text-[#1B2559]">{label}</dt>
    <dd title={hint || undefined} className="min-w-0 flex-1 break-words text-right text-sm text-[#3B4BA8]">{value}</dd>
  </div>;
}

function OtherRow({ label, value }: { label: string; value: string }) {
  return <div className="flex items-start justify-between gap-4 border-b border-slate-100 py-3 text-sm">
    <dt className="shrink-0 text-slate-500">{label}</dt>
    <dd className="break-words text-right font-semibold text-[#1B2559]">{value}</dd>
  </div>;
}

function VideoTile({ src }: { src: string }) {
  const [length, setLength] = useState<number | null>(null);
  return <>
    <video src={src} preload="metadata" muted playsInline className="h-full w-full object-cover" onLoadedMetadata={e => setLength(e.currentTarget.duration)} />
    <span className="absolute inset-0 grid place-items-center bg-black/25"><span className="grid h-7 w-7 place-items-center rounded-full bg-black/60 text-white"><Play size={14} fill="currentColor" /></span></span>
    {length != null && Number.isFinite(length) && <span className="absolute bottom-1 left-1/2 -translate-x-1/2 rounded bg-black/55 px-1 text-[10px] font-semibold text-white">{duration(length)}</span>}
  </>;
}

function Stars({ value, size = 16 }: { value: number; size?: number }) {
  return <span className="inline-flex items-center gap-0.5 text-amber-400" aria-label={`${value} trên 5 sao`}>
    {Array.from({ length: 5 }, (_, i) => <Star key={i} size={size} fill={i < Math.round(value) ? 'currentColor' : 'none'} />)}
  </span>;
}

function ReviewsPanel({ reviews, error, onReply }: {
  reviews: ProductReview[];
  error: string;
  onReply: (review: ProductReview, content: string) => Promise<void>;
}) {
  const [filter, setFilter] = useState(0);
  const [openId, setOpenId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [savingId, setSavingId] = useState<string | null>(null);
  const [replyError, setReplyError] = useState('');

  const total = reviews.length;
  const avg = total ? reviews.reduce((sum, r) => sum + r.rating, 0) / total : 0;
  const counts = [5, 4, 3, 2, 1].map(star => ({ star, n: reviews.filter(r => Math.round(r.rating) === star).length }));
  const shown = filter ? reviews.filter(r => Math.round(r.rating) === filter) : reviews;

  const openReply = (r: ProductReview) => { setOpenId(r.id); setDraft(r.replies[0]?.content ?? ''); setReplyError(''); };
  const submit = async (r: ProductReview) => {
    const content = draft.trim();
    if (!content) return;
    setSavingId(r.id); setReplyError('');
    try { await onReply(r, content); setOpenId(null); }
    catch (e) { setReplyError(errorMessage(e)); }
    finally { setSavingId(null); }
  };

  if (error) return <p className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">Không tải được đánh giá: {error}</p>;
  if (!total) return <p className="p-8 text-center text-sm text-slate-500">Sản phẩm này chưa có đánh giá nào.</p>;

  return <div className="mt-5">
    {/* Tổng hợp */}
    <div className="grid gap-6 rounded-xl bg-[#F1F4FD] p-5 sm:grid-cols-[200px_minmax(0,1fr)] sm:items-center">
      <div className="text-center">
        <p className="text-4xl font-semibold text-[#5367C9]">{avg.toFixed(1)}<span className="text-lg font-semibold text-slate-500"> / 5</span></p>
        <div className="mt-1 flex justify-center"><Stars value={avg} size={20} /></div>
        <p className="mt-1 text-sm text-slate-600">{total} đánh giá</p>
      </div>
      <div className="space-y-1.5">
        {counts.map(({ star, n }) => <div key={star} className="flex items-center gap-3 text-sm">
          <span className="w-12 shrink-0 text-slate-700">{star} sao</span>
          <span className="h-2 flex-1 overflow-hidden rounded-full bg-white"><span className="block h-full rounded-full bg-amber-400" style={{ width: `${(n / total) * 100}%` }} /></span>
          <span className="w-8 shrink-0 text-right text-slate-600">{n}</span>
        </div>)}
      </div>
    </div>

    {/* Lọc theo số sao */}
    <div className="mt-4 flex flex-wrap gap-2">
      {[{ star: 0, n: total }, ...counts].map(({ star, n }) => <button key={star} type="button" onClick={() => setFilter(star)}
        className={`rounded-full border px-3.5 py-1.5 text-[13px] font-semibold transition ${filter === star ? 'border-[#5367C9] bg-[#5367C9] text-white' : 'border-slate-200 bg-white text-slate-700 hover:border-[#5367C9]'}`}>
        {star === 0 ? 'Tất cả' : `${star} sao`} ({n})
      </button>)}
    </div>

    {/* Danh sách */}
    <ul className="mt-4 divide-y divide-slate-100 rounded-xl border border-slate-200">
      {shown.length === 0 && <li className="p-6 text-center text-sm text-slate-500">Không có đánh giá {filter} sao.</li>}
      {shown.map(r => <li key={r.id} className="p-5">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p className="font-semibold text-[#1B2559]">{r.customerName || 'Khách hàng'}</p>
            <p className="mt-0.5 text-xs text-slate-500">{reviewDate(r.createdAt)}</p>
          </div>
          <Stars value={r.rating} />
        </div>
        {r.content ? <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{r.content}</p> : <p className="mt-3 text-sm italic text-slate-400">Khách không để lại nội dung.</p>}

        {r.replies.map(reply => <div key={reply.id} className="mt-3 rounded-lg bg-[#F1F4FD] p-3 text-sm">
          <p className="font-semibold text-[#5367C9]">Phản hồi của cửa hàng <span className="ml-1 text-xs font-normal text-slate-500">{reviewDate(reply.createdAt)}</span></p>
          <p className="mt-1 whitespace-pre-wrap text-slate-700">{reply.content}</p>
        </div>)}

        {openId === r.id ? <div className="mt-3">
          <textarea value={draft} onChange={e => setDraft(e.target.value)} maxLength={2000} rows={3} placeholder="Viết phản hồi..."
            className="w-full rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-[#5367C9]" />
          {replyError && <p className="mt-1 text-xs text-rose-600">{replyError}</p>}
          <div className="mt-2 flex justify-end gap-2">
            <button type="button" onClick={() => setOpenId(null)} className="rounded-lg border border-slate-200 px-3.5 py-2 text-sm font-semibold text-slate-600">Hủy</button>
            <button type="button" disabled={savingId === r.id || !draft.trim()} onClick={() => void submit(r)}
              className="rounded-lg bg-[#5367C9] px-3.5 py-2 text-sm font-semibold text-white disabled:opacity-50">{savingId === r.id ? 'Đang lưu...' : 'Gửi phản hồi'}</button>
          </div>
        </div> : <button type="button" onClick={() => openReply(r)} className="mt-3 text-sm font-semibold text-[#5367C9] hover:underline">
          {r.replies.length ? 'Sửa phản hồi' : 'Trả lời'}
        </button>}
      </li>)}
    </ul>
  </div>;
}
