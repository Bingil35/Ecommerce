import { ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProductCard from './ProductCard';
import type { HomeProduct } from './productData';
import { productApi } from '../../../../services/catalog/productApi';
export default function RecommendationSection() {
  const [products, setProducts] = useState<HomeProduct[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(false); const navigate = useNavigate();
  useEffect(() => { let active = true; productApi.list({ featured: true, sortBy: 'newest' }).then((items) => { if (active) setProducts(items.slice(0, 12)); }).catch(() => { if (active) setError(true); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, []);
  return <motion.section layout transition={{ layout: { duration: 0.45, ease: [0.22, 1, 0.36, 1] } }} className="pb-12 pt-12">
    <div className="mb-5 flex items-end justify-between"><div><h2 className="text-xl font-bold tracking-tight text-[#202642] sm:text-2xl">Gợi ý hôm nay</h2><p className="mt-1 text-xs text-[#8A93A6] sm:text-sm">Sản phẩm nổi bật được chọn dành cho bạn</p></div>
      <button type="button" onClick={() => navigate('/products?featured=true&title=Gợi%20ý%20hôm%20nay')} className="flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium text-[#5367C9] transition-colors hover:bg-[#EEF3FF] sm:text-sm">Xem tất cả <ArrowRight size={15}/></button></div>
    {loading && <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">{Array.from({length:6},(_,i)=><div key={i} className="animate-pulse overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white"><div className="aspect-square bg-[#EDF1F8]"/><div className="space-y-2 p-3"><div className="h-3 rounded bg-[#EDF1F8]"/><div className="h-3 w-2/3 rounded bg-[#EDF1F8]"/></div></div>)}</div>}
    {!loading && error && <div className="rounded-2xl bg-white p-6 text-sm text-rose-500">Không thể tải sản phẩm. Hãy kiểm tra backend và thử tải lại trang.</div>}
    {!loading && !error && products.length === 0 && <div className="rounded-2xl border border-dashed border-[#DCE5F5] bg-white p-8 text-center text-sm text-[#8A93A6]">Chưa có sản phẩm nổi bật đang được bán.</div>}
    {!loading && !error && products.length > 0 && <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 lg:grid-cols-6 lg:gap-5">{products.map((product) => <ProductCard key={product.id} product={product}/>)}</div>}
  </motion.section>;
}
