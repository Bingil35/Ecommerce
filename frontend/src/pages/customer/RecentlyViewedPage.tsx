import { useEffect, useState } from 'react';
import { Eye } from 'lucide-react';
import axiosClient from '../../services/api/axiosClient';
import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import type { HomeProduct } from '../../components/customer/home/recommendation/productData';

type Envelope<T>={data:T};
export default function RecentlyViewedPage(){
 const [products,setProducts]=useState<HomeProduct[]>([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
 useEffect(()=>{let active=true;void axiosClient.get<Envelope<HomeProduct[]>>('/customer/recently-viewed').then(({data})=>{if(active)setProducts(data.data||[]);}).catch(e=>{if(active)setError(e?.response?.data?.message||'Không tải được lịch sử xem.');}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 return <div className="min-h-[360px] rounded-2xl border border-[#E6EAF3] bg-white p-5 shadow-[0_6px_24px_rgba(40,55,90,.04)] sm:p-7"><div className="mb-5"><h1 className="text-xl font-bold text-[#252B40]">Sản phẩm đã xem gần đây</h1><p className="mt-1 text-sm text-[#7A8498]">Tối đa 30 sản phẩm bạn vừa xem khi đăng nhập.</p></div>{loading?<p className="py-10 text-center text-sm text-slate-500">Đang tải...</p>:error?<p className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p>:products.length?<div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">{products.map(product=><ProductCard key={product.id} product={product}/>)}</div>:<div className="py-14 text-center"><Eye size={28} className="mx-auto text-slate-300"/><p className="mt-3 text-sm font-semibold text-slate-700">Chưa có sản phẩm đã xem</p></div>}</div>;
}
