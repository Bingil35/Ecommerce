import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Store, UsersRound } from 'lucide-react';
import axiosClient from '../../services/api/axiosClient';
import { mediaUrl } from '../../services/api/mediaUrl';

type Shop={id:string;shopName:string;slug:string;logoUrl?:string|null;address?:string|null;followersCount?:number|null};
type Envelope<T>={data:T};
export default function FollowedShopsPage(){
 const [shops,setShops]=useState<Shop[]>([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
 useEffect(()=>{let active=true;void axiosClient.get<Envelope<Shop[]>>('/customer/shops/followed').then(({data})=>{if(active)setShops(data.data||[]);}).catch(e=>{if(active)setError(e?.response?.data?.message||'Không tải được danh sách cửa hàng.');}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 return <div className="min-h-[360px] rounded-2xl border border-[#E6EAF3] bg-white p-5 shadow-[0_6px_24px_rgba(40,55,90,.04)] sm:p-7"><div className="mb-5"><h1 className="text-xl font-bold text-[#252B40]">Shop đang theo dõi</h1><p className="mt-1 text-sm text-[#7A8498]">Các cửa hàng bạn đã chọn theo dõi.</p></div>{loading?<p className="py-10 text-center text-sm text-slate-500">Đang tải...</p>:error?<p className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p>:shops.length?<div className="grid gap-3 sm:grid-cols-2">{shops.map(shop=><Link key={shop.id} to={`/shop/${shop.slug}`} className="flex items-center gap-3 rounded-xl border border-slate-100 p-4 transition hover:border-indigo-200 hover:bg-indigo-50/40"><span className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-xl bg-indigo-50 text-indigo-600">{shop.logoUrl?<img className="h-full w-full object-cover" src={mediaUrl(shop.logoUrl)} alt=""/>:<Store size={20}/>}</span><span className="min-w-0 flex-1"><b className="block truncate text-sm text-slate-800">{shop.shopName}</b><small className="mt-1 flex items-center gap-1 text-xs text-slate-500"><UsersRound size={12}/>{shop.followersCount||0} người theo dõi</small></span></Link>)}</div>:<div className="py-14 text-center"><Store size={28} className="mx-auto text-slate-300"/><p className="mt-3 text-sm font-semibold text-slate-700">Bạn chưa theo dõi cửa hàng nào</p><Link to="/products" className="mt-3 inline-flex text-sm font-semibold text-indigo-600">Khám phá sản phẩm</Link></div>}</div>;
}
