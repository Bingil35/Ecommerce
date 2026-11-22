import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ChevronRight, Heart, MapPin, MessageCircle, Package, Star, Store } from 'lucide-react';
import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import type { HomeProduct } from '../../components/customer/home/recommendation/productData';
import { categoryApi, productApi, shopApi, type CategoryDto, type PublicShopDto } from '../../services/catalog/productApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { useAuth } from '../../context/AuthContext';
import axiosClient from '../../services/api/axiosClient';

type ProductTab = 'all' | 'featured' | 'best_selling' | 'categories';

export default function ShopDetailPage() {
  const { slug = '' } = useParams();
  const navigate=useNavigate();const {isAuthenticated}=useAuth();
  const [shop, setShop] = useState<PublicShopDto | null>(null);
  const [products, setProducts] = useState<HomeProduct[]>([]);
  const [allCategories, setAllCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<ProductTab>('all');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [following,setFollowing]=useState(false);const [followBusy,setFollowBusy]=useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setShop(null);
    setProducts([]);
    shopApi.getBySlug(slug)
      .then(async shopInfo => {
        if (!active) return;
        setShop(shopInfo);
        if(isAuthenticated)void axiosClient.get<{data:{following:boolean}}>(`/customer/shops/${shopInfo.id}/follow`).then(({data})=>{if(active)setFollowing(Boolean(data.data.following));}).catch(()=>{});else setFollowing(false);
        const [items, categories] = await Promise.all([
          productApi.list({ shopId: shopInfo.id }),
          categoryApi.list().catch(() => []),
        ]);
        if (active) { setProducts(items); setAllCategories(categories); }
      })
      .catch(() => {
        if (active) setError('Không tìm thấy cửa hàng hoặc cửa hàng hiện không hoạt động.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [slug,isAuthenticated]);

  const toggleFollow=async()=>{if(!shop)return;if(!isAuthenticated){navigate('/login',{state:{from:`/shop/${slug}`,notice:'Đăng nhập để theo dõi cửa hàng.'}});return;}setFollowBusy(true);try{const {data}=following?await axiosClient.delete<{data:{following:boolean}}>(`/customer/shops/${shop.id}/follow`):await axiosClient.post<{data:{following:boolean}}>(`/customer/shops/${shop.id}/follow`);setFollowing(Boolean(data.data.following));setShop(current=>current?{...current,followersCount:Math.max(0,Number(current.followersCount||0)+(data.data.following?1:-1))}:current);}catch(e){setError('Không thể cập nhật theo dõi cửa hàng. Vui lòng thử lại.');}finally{setFollowBusy(false);}};

  const shopCategories = useMemo(() => {
    const byId = new Map(allCategories.map(category => [category.id, category]));
    const rootIds = new Set<string>();
    products.forEach(product => {
      let category = product.categoryId ? byId.get(product.categoryId) : undefined;
      const visited = new Set<string>();
      while (category?.parentId && !visited.has(category.id)) {
        visited.add(category.id);
        category = byId.get(category.parentId);
      }
      if (category) rootIds.add(category.id);
    });
    return allCategories.filter(category => rootIds.has(category.id) && !category.parentId)
      .sort((a, b) => a.name.localeCompare(b.name, 'vi'));
  }, [allCategories, products]);

  const categoryProductCounts = useMemo(() => {
    const byId = new Map(allCategories.map(category => [category.id, category]));
    const counts = new Map<string, number>();
    products.forEach(product => {
      let category = product.categoryId ? byId.get(product.categoryId) : undefined;
      const visited = new Set<string>();
      while (category?.parentId && !visited.has(category.id)) {
        visited.add(category.id);
        category = byId.get(category.parentId);
      }
      if (category) counts.set(category.id, (counts.get(category.id) ?? 0) + 1);
    });
    return counts;
  }, [allCategories, products]);

  const visibleProducts = useMemo(() => {
    let filtered = products;
    if (tab === 'featured') filtered = filtered.filter(product => Boolean(product.featured));
    if (tab === 'categories' && selectedCategory) {
      const byId = new Map(allCategories.map(category => [category.id, category]));
      filtered = filtered.filter(product => {
        let category = product.categoryId ? byId.get(product.categoryId) : undefined;
        const visited = new Set<string>();
        while (category && category.id !== selectedCategory && category.parentId && !visited.has(category.id)) {
          visited.add(category.id);
          category = byId.get(category.parentId);
        }
        return category?.id === selectedCategory;
      });
    }
    if (tab === 'best_selling') return filtered.filter(product => Number(product.soldQuantity ?? 0) > 0)
      .sort((a, b) => Number(b.soldQuantity ?? 0) - Number(a.soldQuantity ?? 0));
    return filtered;
  }, [products, tab, selectedCategory, allCategories]);

  if (loading) return <main className="min-h-[65vh] bg-[#F7F9FD] px-4 py-12"><div className="mx-auto max-w-[1200px] animate-pulse"><div className="h-56 rounded-3xl bg-[#E9EDF5]"/><div className="mt-5 h-28 rounded-2xl bg-white"/><div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">{Array.from({ length: 6 }, (_, i) => <div key={i} className="aspect-[.72] rounded-2xl bg-white"/>)}</div></div></main>;

  if (error || !shop) return <main className="grid min-h-[60vh] place-items-center bg-[#F7F9FD] px-4"><div className="max-w-md rounded-2xl border border-[#E7EBF3] bg-white p-8 text-center"><Store size={34} className="mx-auto text-[#7A86C9]"/><h1 className="mt-4 text-xl font-bold text-[#202642]">Không thể hiển thị cửa hàng</h1><p className="mt-2 text-sm text-[#7A8498]">{error || 'Cửa hàng không tồn tại.'}</p><Link to="/products" className="mt-5 inline-flex rounded-xl bg-[#5367C9] px-5 py-2.5 text-sm font-semibold text-white">Tiếp tục mua sắm</Link></div></main>;

  const tabs: { id: ProductTab; label: string; count: number }[] = [
    { id: 'all', label: 'Tất cả sản phẩm', count: products.length },
    { id: 'featured', label: 'Nổi bật', count: products.filter(product => product.featured).length },
    { id: 'best_selling', label: 'Bán chạy', count: products.filter(product => Number(product.soldQuantity ?? 0) > 0).length },
    { id: 'categories', label: 'Danh mục', count: shopCategories.length },
  ];

  return <main className="min-h-screen bg-[#F7F9FD] pb-12">
    <div className="mx-auto w-full max-w-[1200px] min-w-0 px-3 pt-5 sm:px-4">
      <nav aria-label="Đường dẫn" className="mb-4 flex items-center gap-2 text-xs text-[#8791A4]"><Link to="/" className="hover:text-[#5367C9]">Trang chủ</Link><ChevronRight size={14}/><span className="font-medium text-[#47516A]">{shop.shopName}</span></nav>
      <section className="relative h-[275px] overflow-hidden rounded-[26px] border border-[#E5EAF3] bg-gradient-to-br from-[#293A75] via-[#667bd0] to-[#b7c3f4] shadow-[0_12px_34px_rgba(36,52,99,0.12)] sm:h-[310px]">
        {shop.bannerUrl && <img src={mediaUrl(shop.bannerUrl)} alt="Ảnh bìa cửa hàng" className="absolute inset-0 h-full w-full object-cover"/>}
        <div className="absolute inset-0 bg-gradient-to-t from-[#17264e]/55 via-[#17264e]/5 to-transparent"/>
        <div className="absolute bottom-3 left-3 flex w-fit max-w-[calc(100%-1.5rem)] flex-col gap-3 rounded-[20px] border border-white/35 bg-white/40 p-3 shadow-[0_10px_30px_rgba(20,32,66,0.16)] backdrop-blur-md sm:bottom-5 sm:left-5 sm:max-w-[calc(100%-2.5rem)] sm:flex-row sm:items-center sm:gap-5 sm:p-4">
          <div className="flex min-w-0 items-center gap-3 sm:gap-4">
            <div className="grid h-[64px] w-[64px] shrink-0 place-items-center overflow-hidden rounded-[17px] border border-[#E7EAF4] bg-white p-1.5 shadow-[0_3px_10px_rgba(30,45,80,0.10)] sm:h-[72px] sm:w-[72px] sm:p-2">
              {shop.logoUrl ? <img src={mediaUrl(shop.logoUrl)} alt={shop.shopName} className="block h-full w-full object-contain"/> : <Store size={28} className="text-[#596BD0]"/>}
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-[17px] font-bold leading-tight tracking-[-.02em] text-[#202642] sm:text-lg">{shop.shopName}</h1>
              <div className="mt-2 flex flex-wrap gap-2">
                <button type="button" disabled={followBusy} onClick={()=>void toggleFollow()} className={`inline-flex h-9 items-center justify-center gap-1.5 rounded-full border px-3.5 text-xs font-semibold transition disabled:opacity-60 ${following?'border-rose-200 bg-rose-50 text-rose-600':'border-[#E1E6F1] bg-white/80 text-[#5866B7] hover:bg-white'}`}><Heart size={14} fill={following?'currentColor':'none'}/>{following?'Đang theo dõi':'Theo dõi shop'}</button>
                <Link to={`/chat?shopId=${encodeURIComponent(shop.id)}`} className="inline-flex h-9 items-center justify-center gap-1.5 rounded-full bg-[#5367C9] px-4 text-xs font-semibold text-white shadow-sm transition hover:bg-[#4659B8]"><MessageCircle size={14}/>Chat với shop</Link>
              </div>
            </div>
          </div>
          <div className="grid min-w-0 grid-cols-2 gap-x-4 gap-y-2 border-t border-white/40 pt-3 text-xs text-[#46516A] sm:border-l sm:border-t-0 sm:pl-5 sm:pt-0">
            <span className="inline-flex min-w-0 items-center gap-1"><Star size={13} className="shrink-0 text-[#E8A832]" fill="currentColor"/>{Number(shop.ratingAverage ?? 0).toFixed(1)} <span className="truncate text-[11px]">({shop.ratingCount ?? 0} đánh giá)</span></span>
            <span className="inline-flex min-w-0 items-center gap-1"><Heart size={12} className="shrink-0"/><span className="truncate">{shop.followersCount ?? 0} người theo dõi</span></span>
            <span className="inline-flex min-w-0 items-center gap-1"><Package size={13} className="shrink-0"/><span>{products.length} sản phẩm</span></span>
            {shop.address && <span className="inline-flex min-w-0 items-center gap-1"><MapPin size={13} className="shrink-0 text-[#7B87A1]"/><span className="truncate">{shop.address}</span></span>}
          </div>
        </div>
      </section>

      <section className="mt-7">
        <div className="rounded-2xl border border-[#E7EBF3] bg-white p-3 sm:p-4">
          <div className="flex gap-2 overflow-x-auto pb-1">{tabs.map(item => <button key={item.id} type="button" onClick={() => { setTab(item.id); setSelectedCategory(null); }} className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold transition ${tab === item.id ? 'bg-[#5367C9] text-white' : 'bg-[#F3F5FA] text-[#697386] hover:bg-[#E9EDFA]'}`}>{item.label}<span className={`ml-1.5 text-xs ${tab === item.id ? 'text-white/75' : 'text-[#98A1B2]'}`}>{item.count}</span></button>)}</div>
          {tab === 'categories' && <div className="mt-3 flex flex-wrap gap-2 border-t border-[#EEF1F6] pt-3">
            <button type="button" onClick={() => setSelectedCategory(null)} className={`rounded-full border px-3 py-1.5 text-xs font-semibold ${selectedCategory === null ? 'border-[#5367C9] bg-[#F1F3FF] text-[#5367C9]' : 'border-[#E5EAF3] text-[#697386]'}`}>Tất cả danh mục</button>
            {shopCategories.map(category => <button key={category.id} type="button" onClick={() => setSelectedCategory(category.id)} className={`rounded-full border px-3 py-1.5 text-xs font-semibold ${selectedCategory === category.id ? 'border-[#5367C9] bg-[#F1F3FF] text-[#5367C9]' : 'border-[#E5EAF3] text-[#697386]'}`}>{category.name}<span className="ml-1.5 opacity-60">{categoryProductCounts.get(category.id) ?? 0}</span></button>)}
          </div>}
        </div>
        <div className="mb-4 mt-5 flex items-center justify-between"><h2 className="text-lg font-bold text-[#202642]">Sản phẩm của cửa hàng</h2><span className="text-sm text-[#8791A4]">{visibleProducts.length} sản phẩm</span></div>
        {visibleProducts.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">{visibleProducts.map(product => <ProductCard key={product.id} product={product}/>)}</div> : <div className="rounded-2xl border border-[#E7EBF3] bg-white px-6 py-16 text-center"><Package size={32} className="mx-auto text-[#A7B0C1]"/><p className="mt-3 font-semibold text-[#35405A]">Không có sản phẩm</p><p className="mt-1 text-sm text-[#7A8498]">{tab === 'best_selling' ? 'Shop chưa có sản phẩm đã phát sinh lượt bán.' : tab === 'categories' && selectedCategory ? 'Danh mục này hiện chưa có sản phẩm.' : 'Shop chưa có sản phẩm phù hợp.'}</p></div>}
      </section>
    </div>
  </main>;
}
