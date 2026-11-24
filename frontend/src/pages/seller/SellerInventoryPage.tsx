import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Package, Search, TriangleAlert, Warehouse } from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { inventoryApi, type InventoryVariant } from '../../services/seller/inventoryApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { getApiError } from '../../services/checkout/checkoutApi';
import { categoryApi, type CategoryDto } from '../../services/catalog/productApi';

type InventoryTab = 'ALL' | 'STABLE' | 'LOW' | 'OUT' | 'HIDDEN';
const tabs: { id: InventoryTab; label: string }[] = [
  { id: 'ALL', label: 'Tất cả' }, { id: 'STABLE', label: 'Tồn kho ổn định' },
  { id: 'LOW', label: 'Biến thể sắp hết' }, { id: 'OUT', label: 'Hết hàng' }, { id: 'HIDDEN', label: 'Đã ẩn' },
];

export default function SellerInventoryPage() {
  const [products, setProducts] = useState<SellerProduct[]>([]);
  const [rows, setRows] = useState<InventoryVariant[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [keyword, setKeyword] = useState('');
  const [rootCategoryId, setRootCategoryId] = useState('');
  const [childCategoryId, setChildCategoryId] = useState('');
  const [tab, setTab] = useState<InventoryTab>('ALL');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    void Promise.all([sellerProductApi.list({ page: 0, size: 100, sort: 'name_asc' }), inventoryApi.listShop(), categoryApi.list()])
      .then(async ([firstPage, inventory, allCategories]) => {
        const remaining = await Promise.all(Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) => sellerProductApi.list({ page: index + 1, size: 100, sort: 'name_asc' })));
        if (active) { setProducts([...firstPage.items, ...remaining.flatMap(page => page.items)]); setRows(inventory); setCategories(allCategories); }
      }).catch(e => { if (active) setError(getApiError(e)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const cards = useMemo(() => products.map(product => {
    const variants = rows.filter(row => row.productId === product.id);
    return { product, variants, stock: variants.reduce((sum, v) => sum + v.quantity, 0), available: variants.reduce((sum, v) => sum + v.availableQuantity, 0), low: variants.filter(v => v.lowStock).length };
  }), [products, rows]);
  const categoryById = useMemo(() => new Map(categories.map(category => [category.id, category])), [categories]);
  const rootCategories = useMemo(() => {
    const usedRoots = new Set(products.flatMap(product => {
      const assigned = product.categoryId ? categoryById.get(product.categoryId) : undefined;
      if (!assigned) return [];
      const root = assigned.parentId ? categoryById.get(assigned.parentId) : assigned;
      return root ? [root.id] : [];
    }));
    return categories.filter(category => !category.parentId && usedRoots.has(category.id)).sort((a,b)=>a.name.localeCompare(b.name,'vi'));
  }, [categories, categoryById, products]);
  const childCategories = useMemo(() => categories.filter(category => category.parentId === rootCategoryId
    && products.some(product => product.categoryId === category.id)).sort((a,b)=>a.name.localeCompare(b.name,'vi')), [categories, rootCategoryId, products]);
  const matchesTab = (card: typeof cards[number], id: InventoryTab) => {
    if (id === 'ALL') return true;
    if (id === 'HIDDEN') return card.product.status === 'INACTIVE';
    if (card.product.status !== 'ACTIVE') return false;
    if (id === 'OUT') return card.available <= 0;
    if (id === 'LOW') return card.available > 0 && card.low > 0;
    return card.available > 0 && card.low === 0;
  };
  const filtered = cards.filter(card => {
    const assignedCategory = card.product.categoryId ? categoryById.get(card.product.categoryId) : undefined;
    const assignedRootId = assignedCategory?.parentId || assignedCategory?.id || '';
    return matchesTab(card, tab)
    && (!rootCategoryId || assignedRootId === rootCategoryId)
    && (!childCategoryId || card.product.categoryId === childCategoryId)
    && `${card.product.name} ${card.product.sku || ''}`.toLocaleLowerCase('vi').includes(keyword.trim().toLocaleLowerCase('vi'));
  });
  const countFor = (id: InventoryTab) => cards.filter(card => matchesTab(card, id)).length;
  const statusLabel = (card: typeof cards[number]) => card.product.status === 'INACTIVE' ? 'Đã ẩn'
    : card.product.status === 'PENDING' ? 'Chờ duyệt' : card.product.status === 'REJECTED' ? 'Bị từ chối'
    : card.available <= 0 ? 'Hết hàng' : card.low ? `${card.low} biến thể sắp hết` : 'Tồn kho ổn định';
  const statusClass = (card: typeof cards[number]) => card.product.status === 'INACTIVE' ? 'bg-slate-100 text-slate-600'
    : card.product.status === 'PENDING' ? 'bg-amber-50 text-amber-700' : card.product.status === 'REJECTED' ? 'bg-rose-50 text-rose-700'
    : card.available <= 0 ? 'bg-rose-50 text-rose-700' : card.low ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700';

  return <div className="seller-container shop-management !max-w-[1380px] min-w-0">
    <header className="mb-5 flex flex-wrap items-center gap-3"><span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-[#EEF3FF] text-[#5367C9]"><Warehouse size={22}/></span><div><h1 style={{ fontWeight: 650 }}>Kho hàng</h1><p className="mt-1 text-sm font-normal">Theo dõi tồn kho của từng sản phẩm và biến thể.</p></div></header>
    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}
    <section className="overflow-hidden rounded-2xl border border-[#e4e8f1] bg-white shadow-sm">
      <div role="tablist" aria-label="Lọc sản phẩm theo tình trạng kho" className="flex gap-1 overflow-x-auto border-b border-slate-100 px-3 [scrollbar-width:none] sm:px-5">{tabs.map(item=><button key={item.id} type="button" role="tab" aria-selected={tab===item.id} onClick={()=>setTab(item.id)} className={`shrink-0 border-b-2 px-3 py-3.5 text-sm font-medium ${tab===item.id?'border-indigo-600 text-indigo-600':'border-transparent text-slate-500 hover:text-slate-800'}`}>{item.label}<span className={`ml-2 rounded-full px-1.5 py-0.5 text-[11px] ${tab===item.id?'bg-indigo-50 text-indigo-600':'bg-slate-100 text-slate-500'}`}>{countFor(item.id)}</span></button>)}</div>
      <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center sm:p-5"><label className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-slate-200 px-3 text-slate-400"><Search size={16}/><input value={keyword} onChange={e=>setKeyword(e.target.value)} placeholder="Tìm theo tên sản phẩm" className="w-full border-0 py-2.5 text-sm font-normal outline-none focus:!border-0 focus:!shadow-none"/></label><div className="grid w-full gap-2 sm:w-auto sm:grid-cols-2"><select aria-label="Lọc theo danh mục cấp cao nhất" value={rootCategoryId} onChange={e=>{setRootCategoryId(e.target.value);setChildCategoryId('');}} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal text-slate-700 outline-none sm:min-w-52"><option value="">Tất cả danh mục</option>{rootCategories.map(category=><option key={category.id} value={category.id}>{category.name}</option>)}</select>{rootCategoryId && childCategories.length > 0 && <select aria-label="Lọc theo danh mục cấp nhỏ hơn" value={childCategoryId} onChange={e=>setChildCategoryId(e.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-normal text-slate-700 outline-none sm:min-w-52"><option value="">Tất cả danh mục con</option>{childCategories.map(category=><option key={category.id} value={category.id}>{category.name}</option>)}</select>}</div></div>
      {loading?<p className="p-12 text-center text-sm text-slate-500">Đang tải sản phẩm...</p>:filtered.length?<div className="grid gap-3 p-3 sm:grid-cols-2 sm:gap-4 sm:p-4 xl:grid-cols-3">{filtered.map(({product,variants,stock,available,low})=><Link key={product.id} to={`/shop-owner/inventory/${product.id}`} className="group flex min-w-0 gap-3 rounded-2xl border border-slate-200 p-3 transition hover:border-indigo-200 hover:shadow-sm sm:gap-4 sm:p-4"><span className="grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded-xl bg-slate-50 text-slate-300 sm:h-24 sm:w-24">{product.thumbnailUrl?<img src={mediaUrl(product.thumbnailUrl)} alt={product.name} className="h-full w-full object-cover"/>:<Package size={28}/>}</span><span className="min-w-0 flex-1"><span className="block line-clamp-2 text-sm font-medium leading-5 text-slate-800 group-hover:text-indigo-700">{product.name}</span><span className="mt-1 block text-xs font-normal text-slate-500">{variants.length} biến thể · {stock} tồn kho</span><span className="mt-1 block text-xs font-normal text-slate-500">Khả dụng: {available}</span><span className={`mt-2 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-medium ${statusClass({product,variants,stock,available,low})}`}>{low>0&&product.status==='ACTIVE'&&<TriangleAlert size={12}/>} {statusLabel({product,variants,stock,available,low})}</span></span></Link>)}</div>:<p className="p-10 text-center text-sm font-normal text-slate-500">Không tìm thấy sản phẩm phù hợp.</p>}
    </section>
  </div>;
}
