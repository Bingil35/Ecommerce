import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ChevronRight, MapPin, MessageCircle, Package, Star, Store } from 'lucide-react';
import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import type { HomeProduct } from '../../components/customer/home/recommendation/productData';
import { categoryApi, productApi, shopApi, type CategoryDto, type PublicShopDto } from '../../services/catalog/productApi';
import { mediaUrl } from '../../services/api/mediaUrl';

type ProductTab = 'all' | 'featured' | 'best_selling' | 'categories';

export default function ShopDetailPage() {
  const { slug = '' } = useParams();
  const [shop, setShop] = useState<PublicShopDto | null>(null);
  const [products, setProducts] = useState<HomeProduct[]>([]);
  const [allCategories, setAllCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<ProductTab>('all');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

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
  }, [slug]);

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
    <div className="mx-auto max-w-[1200px] px-4 pt-5">
      <nav aria-label="Đường dẫn" className="mb-4 flex items-center gap-2 text-xs text-[#8791A4]"><Link to="/" className="hover:text-[#5367C9]">Trang chủ</Link><ChevronRight size={14}/><span className="font-medium text-[#47516A]">{shop.shopName}</span></nav>
      <section className="overflow-hidden rounded-3xl border border-[#E5EAF3] bg-white shadow-[0_8px_28px_rgba(53,69,112,0.06)]">
        <div className="relative h-44 bg-gradient-to-r from-[#293A75] via-[#566CC3] to-[#A8B7F0] sm:h-60">
          {shop.bannerUrl && <img src={mediaUrl(shop.bannerUrl)} alt="" className="h-full w-full object-cover"/>}
          <div className="absolute inset-0 bg-gradient-to-r from-[#202D5A]/55 to-transparent"/>
          <div className="absolute bottom-4 left-5 text-white sm:bottom-6 sm:left-8"><p className="text-xs font-semibold uppercase tracking-[.18em] text-white/80">Gian hàng Kala</p><h1 className="mt-1 text-2xl font-bold sm:text-3xl">{shop.shopName}</h1></div>
        </div>
        <div className="flex flex-col gap-5 p-5 sm:flex-row sm:items-center sm:px-8 sm:py-6">
          <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border-4 border-white bg-[#F0F3FF] shadow-md sm:h-28 sm:w-28">
            {shop.logoUrl ? <img src={mediaUrl(shop.logoUrl)} alt={shop.shopName} className="h-full w-full object-cover"/> : <Store size={35} className="text-[#596BD0]"/>}
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-lg font-bold text-[#202642] sm:text-xl">{shop.shopName}</h2>
            <div className="mt-2 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-[#6F7A90]">
              <span className="inline-flex items-center gap-1.5"><Star size={15} fill="currentColor" className="text-[#E8A832]"/>{Number(shop.ratingAverage ?? 0).toFixed(1)} <span className="text-xs">({shop.ratingCount ?? 0} đánh giá)</span></span>
              <span>{shop.followersCount ?? 0} người theo dõi</span>
              <span className="inline-flex items-center gap-1.5"><Package size={15}/>{products.length} sản phẩm</span>
            </div>
            {shop.address && <p className="mt-2 flex items-start gap-1.5 text-xs text-[#8993A6]"><MapPin size={14} className="mt-0.5 shrink-0"/>{shop.address}</p>}
          </div>
          <Link to={`/chat?shopId=${encodeURIComponent(shop.id)}`} className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-full bg-[#5367C9] px-5 text-sm font-semibold text-white transition hover:bg-[#4659B8]"><MessageCircle size={16}/>Chat với shop</Link>
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
