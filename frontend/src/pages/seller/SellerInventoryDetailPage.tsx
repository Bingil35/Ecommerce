import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Boxes, Package, TriangleAlert, Warehouse } from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { inventoryApi, type InventoryVariant } from '../../services/seller/inventoryApi';
import { getApiError } from '../../services/checkout/checkoutApi';

export default function SellerInventoryDetailPage() {
  const { productId = '' } = useParams();
  const [product, setProduct] = useState<SellerProduct | null>(null);
  const [rows, setRows] = useState<InventoryVariant[]>([]);
  const [amounts, setAmounts] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const load = useCallback(async () => {
    const [item, inventory] = await Promise.all([sellerProductApi.get(productId), inventoryApi.listProduct(productId)]);
    setProduct(item); setRows(inventory);
  }, [productId]);
  useEffect(() => { let active = true; void load().catch(e => { if (active) setError(getApiError(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [load]);
  const stockIn = async (row: InventoryVariant) => {
    const quantity = Number(amounts[row.variantId]); if (!Number.isInteger(quantity) || quantity <= 0) return;
    try { setBusy(row.variantId); await inventoryApi.stockIn(row.variantId, quantity, 'Nhập kho từ chi tiết tồn kho sản phẩm'); setAmounts(v => ({ ...v, [row.variantId]: '' })); await load(); }
    catch (e) { setError(getApiError(e)); } finally { setBusy(null); }
  };
  if (loading) return <main className="seller-container py-16 text-center text-slate-500">Đang tải chi tiết tồn kho...</main>;
  return <main className="seller-container shop-management !max-w-[1380px]">
    <Link to="/seller/inventory" className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-indigo-600"><ArrowLeft size={17}/> Danh sách sản phẩm</Link>
    {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">{error}</div>}
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-indigo-100 bg-gradient-to-r from-white to-indigo-50 p-5 sm:p-7"><div className="flex min-w-0 items-center gap-4"><span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-indigo-100 text-indigo-600"><Warehouse size={25}/></span><div className="min-w-0"><p className="text-xs font-bold uppercase tracking-widest text-indigo-500">Chi tiết tồn kho</p><h1 className="mt-1 truncate text-xl font-extrabold text-slate-900">{product?.name || 'Sản phẩm'}</h1><p className="mt-1 text-sm text-slate-500">{rows.length} biến thể · {rows.reduce((sum, row) => sum + row.quantity, 0)} sản phẩm tồn</p></div></div><Link to={`/seller/products/${productId}`} className="rounded-xl border border-indigo-200 bg-white px-4 py-2.5 text-sm font-bold text-indigo-700">Chi tiết sản phẩm</Link></header>
    <section className="overflow-hidden rounded-2xl border border-[#e4e8f1] bg-white shadow-sm"><div className="border-b border-slate-100 p-4 sm:p-5"><h2 className="font-bold text-slate-900">Tồn kho theo biến thể</h2><p className="mt-1 text-xs text-slate-500">Theo dõi số lượng, hàng đang giữ và hàng có thể bán của từng SKU.</p></div><div className="overflow-x-auto"><table className="w-full min-w-[900px] text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-5 py-3.5">SKU / Biến thể</th><th className="px-4 py-3.5">Tồn kho</th><th className="px-4 py-3.5">Đang giữ</th><th className="px-4 py-3.5">Đã bán</th><th className="px-4 py-3.5">Khả dụng</th><th className="px-5 py-3.5 text-right">Nhập kho</th></tr></thead><tbody>{rows.map(row => <tr key={row.variantId} className="border-t border-slate-100"><td className="px-5 py-4"><b className="text-slate-800">{row.sku || 'Chưa có SKU'}</b><span className="mt-1 block text-xs text-slate-500">Ngưỡng cảnh báo: {row.effectiveLowStockThreshold}</span></td><td className="px-4 py-4 font-semibold tabular-nums">{row.quantity}</td><td className="px-4 py-4 tabular-nums text-slate-500">{row.reservedQuantity}</td><td className="px-4 py-4 tabular-nums text-slate-500">{row.soldQuantity}</td><td className="px-4 py-4"><span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${row.availableQuantity <= 0 ? 'bg-rose-50 text-rose-700' : row.lowStock ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'}`}>{row.lowStock && <TriangleAlert size={13}/>} {row.availableQuantity}</span></td><td className="px-5 py-3 text-right"><div className="inline-flex items-center gap-2"><input aria-label={`Số lượng nhập kho ${row.sku}`} className="w-20 rounded-lg border border-slate-200 px-2 py-2 text-center" type="number" min="1" value={amounts[row.variantId] || ''} onChange={e => setAmounts(v => ({ ...v, [row.variantId]: e.target.value }))} placeholder="SL"/><button disabled={busy === row.variantId || !amounts[row.variantId]} onClick={() => void stockIn(row)} className="rounded-lg bg-indigo-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-50">{busy === row.variantId ? 'Đang lưu...' : 'Nhập kho'}</button></div></td></tr>)}</tbody></table>{rows.length === 0 && <div className="px-5 py-14 text-center"><Boxes size={28} className="mx-auto text-slate-300"/><p className="mt-3 text-sm font-semibold text-slate-700">Sản phẩm chưa có biến thể</p><p className="mt-1 text-xs text-slate-500">Tạo biến thể trong phần sản phẩm để quản lý tồn kho.</p><Link to={`/seller/products/${productId}`} className="mt-4 inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-bold text-white"><Package size={15}/> Mở sản phẩm</Link></div>}</div></section>
  </main>;
}
