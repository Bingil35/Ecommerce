import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Package, Search, TriangleAlert, Warehouse } from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { inventoryApi, type InventoryVariant } from '../../services/seller/inventoryApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { getApiError } from '../../services/checkout/checkoutApi';

export default function SellerInventoryPage() {
  const [products, setProducts] = useState<SellerProduct[]>([]);
  const [rows, setRows] = useState<InventoryVariant[]>([]);
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    void Promise.all([sellerProductApi.list({ page: 0, size: 100, sort: 'name_asc' }), inventoryApi.listShop()])
      .then(async ([firstPage, inventory]) => {
        const remaining = await Promise.all(Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) => sellerProductApi.list({ page: index + 1, size: 100, sort: 'name_asc' })));
        if (active) { setProducts([...firstPage.items, ...remaining.flatMap(page => page.items)]); setRows(inventory); }
      })
      .catch(e => { if (active) setError(getApiError(e)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  const cards = useMemo(() => products.map(product => {
    const variants = rows.filter(row => row.productId === product.id);
    return { product, variants, stock: variants.reduce((sum, v) => sum + v.quantity, 0), available: variants.reduce((sum, v) => sum + v.availableQuantity, 0), low: variants.filter(v => v.lowStock).length };
  }).filter(card => `${card.product.name} ${card.product.sku || ''}`.toLowerCase().includes(keyword.trim().toLowerCase())), [products, rows, keyword]);
  const lowCount = rows.filter(row => row.lowStock).length;
  return <div className="seller-container shop-management !max-w-[1380px]">
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4"><div className="flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-[#EEF3FF] text-[#5367C9]"><Warehouse size={23}/></span><div><h1>Kho hàng</h1><p className="mt-1">Chọn sản phẩm để xem và cập nhật kho của từng biến thể.</p></div></div><div className="flex gap-3"><Metric icon={<Package size={18}/>} label="Sản phẩm" value={products.length}/><Metric icon={<TriangleAlert size={18}/>} label="Biến thể cần nhập" value={lowCount}/></div></header>
    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}
    <section className="overflow-hidden rounded-2xl border border-[#e4e8f1] bg-white shadow-sm"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4 sm:p-5"><div><h2 className="font-bold text-[#202642]">Sản phẩm trong kho</h2><p className="mt-1 text-xs text-slate-500">Nhấn vào sản phẩm để quản lý tồn kho của tất cả biến thể.</p></div><label className="flex min-w-52 items-center gap-2 rounded-xl border px-3 text-slate-400"><Search size={16}/><input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Tìm tên sản phẩm" className="w-full border-0 py-2 text-sm outline-none focus:!border-0 focus:!shadow-none"/></label></div>
      {loading ? <p className="p-12 text-center text-sm text-slate-500">Đang tải sản phẩm...</p> : cards.length ? <div className="grid gap-4 p-4 sm:grid-cols-2 xl:grid-cols-3">{cards.map(({ product, variants, stock, available, low }) => <Link key={product.id} to={`/seller/inventory/${product.id}`} className="group flex min-w-0 gap-4 rounded-2xl border border-slate-200 p-4 transition hover:-translate-y-0.5 hover:border-indigo-200 hover:shadow-md"><span className="grid h-24 w-24 shrink-0 place-items-center overflow-hidden rounded-xl bg-slate-50 text-slate-300">{product.thumbnailUrl ? <img src={mediaUrl(product.thumbnailUrl)} alt={product.name} className="h-full w-full object-cover"/> : <Package size={30}/>}</span><span className="min-w-0 flex-1"><b className="block line-clamp-2 text-sm leading-5 text-slate-800 group-hover:text-indigo-700">{product.name}</b><span className="mt-2 block text-xs text-slate-500">{variants.length} biến thể · {stock} tồn kho</span><span className="mt-1 block text-xs text-slate-500">Khả dụng: {available}</span><span className={`mt-2 inline-flex rounded-full px-2.5 py-1 text-[11px] font-bold ${low ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'}`}>{low ? `${low} biến thể sắp hết` : 'Tồn kho ổn định'}</span></span></Link>)}</div> : <p className="p-12 text-center text-sm text-slate-500">Không tìm thấy sản phẩm.</p>}
    </section>
  </div>;
}

function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) { return <div className="flex items-center gap-2 rounded-xl border border-[#e4e8f1] bg-white px-3 py-2 shadow-sm"><span className="text-indigo-600">{icon}</span><span><small className="block text-[10px] text-slate-500">{label}</small><b className="text-sm text-slate-800">{value}</b></span></div>; }
