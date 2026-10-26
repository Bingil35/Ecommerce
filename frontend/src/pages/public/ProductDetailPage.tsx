import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft, ChevronRight, Heart, Minus, PackageCheck,
  MessageCircle, Plus, ShieldCheck, ShoppingCart, Star, Store, Truck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import ProductCard from '../../components/customer/home/recommendation/ProductCard';
import {
  formatVnd, productApi, productImage,
  type ProductDetailDto,
} from '../../services/catalog/productApi';

export default function ProductDetailPage() {
  const { slug = '' } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { addToCart } = useCart();
  const [product, setProduct] = useState<ProductDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedImage, setSelectedImage] = useState('');
  const [selectedValues, setSelectedValues] = useState<Record<string, string>>({});
  const [quantity, setQuantity] = useState(1);
  const [notice, setNotice] = useState('');
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [shopProducts, setShopProducts] = useState<import('../../services/catalog/productApi').ProductCardDto[]>([]);
  const [relatedProducts, setRelatedProducts] = useState<import('../../services/catalog/productApi').ProductCardDto[]>([]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setNotice('');
    setProduct(null);
    productApi.detail(slug)
      .then((data) => {
        if (!active) return;
        setProduct(data);
        const firstImage = data.media?.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))[0]?.url;
        setSelectedImage(firstImage || data.thumbnailUrl || '');
        const initial: Record<string, string> = {};
        data.options?.forEach((option) => {
          const firstValue = option.values?.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))[0];
          if (firstValue) initial[option.id] = firstValue.id;
        });
        setSelectedValues(initial);
      })
      .catch(() => { if (active) setError('Không thể tải chi tiết sản phẩm. Vui lòng kiểm tra kết nối backend hoặc thử lại.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [slug]);

  useEffect(() => {
    if (!product) return;
    let active = true;
    const loadRecommendations = async () => {
      const tasks = await Promise.allSettled([
        product.shop?.id ? productApi.list({ shopId: product.shop.id }) : Promise.resolve([]),
        product.category?.id || product.categoryId ? productApi.list({ categoryId: product.category?.id || product.categoryId || undefined }) : Promise.resolve([]),
      ]);
      if (!active) return;
      if (tasks[0].status === 'fulfilled') {
        setShopProducts(tasks[0].value.filter((item) => item.id !== product.id).slice(0, 6));
      }
      if (tasks[1].status === 'fulfilled') {
        setRelatedProducts(tasks[1].value.filter((item) => item.id !== product.id).slice(0, 6));
      }
    };
    void loadRecommendations();
    return () => { active = false; };
  }, [product]);

  const sortedOptions = useMemo(
    () => product?.options?.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)) ?? [],
    [product],
  );
  const selectedVariant = useMemo(() => {
    if (!product?.variants?.length) return undefined;
    if (!sortedOptions.length) return product.variants.find((variant) => variant.status === 'ACTIVE') ?? product.variants[0];
    const matches = product.variants.find((variant) => {
      if (variant.status && variant.status !== 'ACTIVE') return false;
      return sortedOptions.every((option) => {
        const selected = selectedValues[option.id];
        if (!selected) return true;
        const variantOption = variant.selectedOptions?.find((item) => item.optionId === option.id);
        return variantOption?.optionValueId === selected;
      });
    });
    return matches;
  }, [product, selectedValues, sortedOptions]);

  const media = useMemo(() => {
    if (!product) return [];
    const gallery = product.media?.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)).map((item) => item.url) ?? [];
    return Array.from(new Set([product.thumbnailUrl, ...gallery].filter((url): url is string => Boolean(url))));
  }, [product]);
  const imageProduct = product ? { thumbnailUrl: selectedImage || product.thumbnailUrl } : { thumbnailUrl: undefined };

  useEffect(() => {
    if (media.length < 2) return;
    const timer = window.setInterval(() => {
      setSelectedImage((current) => {
        const index = media.indexOf(current);
        return media[(index + 1 + media.length) % media.length];
      });
    }, 4000);
    return () => window.clearInterval(timer);
  }, [media, product?.id]);
  const price = selectedVariant?.price ?? product?.minPrice ?? product?.price;
  const comparePrice = selectedVariant?.comparePrice;
  const stock = selectedVariant?.stockQuantity ?? product?.stockQuantity;
  const outOfStock = stock != null && stock <= 0;

  // Keep quantity valid whenever the selected variant (and its stock) changes.
  // A quantity selected for a high-stock variant must never carry over above
  // the available stock of the newly selected variant.
  useEffect(() => {
    if (stock == null) return;
    setQuantity((current) => Math.min(current, Math.max(1, stock)));
  }, [selectedVariant?.id, stock]);

  const requireLogin = (message: string) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/product/${slug}`, notice: message } });
      return false;
    }
    return true;
  };

  const handleAddToCart = async () => {
    if (!requireLogin('Đăng nhập để thêm sản phẩm vào giỏ hàng.')) return;
    if (!product) return;
    if (!selectedVariant && product.variants?.length) {
      setNotice('Vui lòng chọn phân loại sản phẩm hợp lệ.');
      return;
    }
    if (outOfStock) {
      setNotice('Sản phẩm hiện đã hết hàng.');
      return;
    }
    if (stock != null && quantity > stock) {
      setQuantity(stock);
      setNotice(`Số lượng đã được điều chỉnh theo tồn kho (${stock}).`);
      return;
    }

    setIsAddingToCart(true);
    setNotice('');
    try {
      const success = await addToCart({
        productId: product.id,
        variantId: selectedVariant?.id ?? null,
        quantity,
      });
      if (success) setNotice('Đã thêm sản phẩm vào giỏ hàng.');
    } finally {
      setIsAddingToCart(false);
    }
  };

  if (loading) {
    return <main className="min-h-[60vh] bg-[#F7FAFF] px-4 py-16"><div className="mx-auto max-w-[1200px] animate-pulse"><div className="h-4 w-40 rounded bg-[#E7ECF6]"/><div className="mt-8 grid gap-8 md:grid-cols-2"><div className="aspect-square rounded-3xl bg-[#E7ECF6]"/><div className="space-y-4"><div className="h-8 rounded bg-[#E7ECF6]"/><div className="h-20 rounded bg-[#E7ECF6]"/><div className="h-12 rounded bg-[#E7ECF6]"/></div></div></div></main>;
  }

  if (error || !product) {
    return <main className="min-h-[60vh] bg-[#F7FAFF] px-4 py-16"><div className="mx-auto max-w-[720px] rounded-3xl border border-[#E5EAF4] bg-white p-8 text-center"><div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#EEF3FF] text-[#5367C9]"><PackageCheck size={25}/></div><h1 className="mt-4 text-xl font-bold text-[#202642]">Không tải được sản phẩm</h1><p className="mt-2 text-sm text-[#7B8498]">{error || 'Sản phẩm không tồn tại hoặc không còn được hiển thị.'}</p><div className="mt-6 flex justify-center gap-3"><button onClick={() => navigate(-1)} className="rounded-full border border-[#DCE4F2] px-5 py-2.5 text-sm font-semibold text-[#4D5872]">Quay lại</button><Link to="/products" className="rounded-full bg-[#5367C9] px-5 py-2.5 text-sm font-semibold text-white">Xem sản phẩm</Link></div></div></main>;
  }

  return (
    <main className="min-h-screen bg-[#F7FAFF] pb-12">
      <div className="mx-auto max-w-[1200px] px-4 py-5 sm:py-8">
        <nav className="mb-5 flex flex-wrap items-center gap-2 text-xs text-[#8A93A6] sm:text-sm" aria-label="Đường dẫn">
          <Link to="/" className="hover:text-[#5367C9]">Trang chủ</Link><ChevronRight size={14}/>
          {product.category && <><Link to={`/products?categoryId=${product.category.id}&category=${encodeURIComponent(product.category.name)}`} className="hover:text-[#5367C9]">{product.category.name}</Link><ChevronRight size={14}/></>}
          <span className="line-clamp-1 max-w-[60%] text-[#4C5570]">{product.name}</span>
        </nav>

        <section className="grid gap-5 rounded-3xl border border-[#E7ECF5] bg-white p-3 shadow-[0_4px_24px_rgba(72,93,140,0.04)] sm:p-5 lg:grid-cols-[minmax(0,1.02fr)_minmax(0,1fr)] lg:gap-7 lg:p-6">
          <div className="min-w-0 p-1 sm:p-0">
            <div className="overflow-hidden rounded-2xl bg-[#F3F6FC]">
              <img key={selectedImage || product.thumbnailUrl || product.id} src={productImage(imageProduct)} alt={product.name} className="product-detail-image aspect-square w-full object-contain" />
            </div>
            {media.length > 1 && <div className="mt-3 grid grid-cols-5 gap-2 sm:gap-3">{media.map((url) => <button key={url} type="button" onClick={() => setSelectedImage(url)} className={`overflow-hidden rounded-xl border-2 bg-[#F3F6FC] ${selectedImage === url ? 'border-[#5367C9]' : 'border-transparent hover:border-[#C8D2F0]'}`} aria-label="Xem ảnh sản phẩm"><img src={url} alt="" className="aspect-square w-full object-cover"/></button>)}</div>}
          </div>

          <div className="min-w-0 p-2 sm:p-3 lg:p-2">
            <div className="flex flex-wrap items-center gap-2"><span className="rounded-full bg-[#EEF3FF] px-3 py-1 text-xs font-semibold text-[#5367C9]">Kala</span>{product.featured && <span className="rounded-full bg-[#F0F7FF] px-3 py-1 text-xs font-semibold text-[#4974A8]">Nổi bật</span>}</div>
            <h1 className="mt-4 text-2xl font-bold leading-snug tracking-tight text-[#202642] sm:text-3xl">{product.name}</h1>
            <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-[#7B8498]">
              <span className="flex items-center gap-1 text-[#E7A92B]"><Star size={15} fill="currentColor"/><b className="text-[#4C5570]">{Number(product.ratingAverage ?? 0).toFixed(1)}</b></span>
              <span>{product.ratingCount ?? 0} đánh giá</span><span className="h-4 w-px bg-[#E3E8F2]"/><span>{stock != null ? `Còn ${stock} sản phẩm` : 'Đang kinh doanh'}</span>
            </div>
            <div className="mt-5 rounded-2xl bg-[#F4F7FF] p-4 sm:p-5"><div className="flex flex-wrap items-baseline gap-3"><span className="text-3xl font-extrabold tracking-tight text-[#5367C9]">{formatVnd(price)}</span>{comparePrice != null && Number(comparePrice) > Number(price) && <><span className="text-sm text-[#9AA2B1] line-through">{formatVnd(comparePrice)}</span><span className="rounded-md bg-[#FFECEE] px-2 py-1 text-xs font-bold text-[#D95368]">-{Math.round((1 - Number(price) / Number(comparePrice)) * 100)}%</span></>}</div><p className="mt-1 text-xs text-[#8A93A6]">Giá bán có thể thay đổi theo phân loại sản phẩm.</p></div>

            {sortedOptions.map((option) => <div key={option.id} className="mt-5"><div className="mb-2 text-sm font-semibold text-[#343D55]">{option.name}: <span className="font-normal text-[#7B8498]">{option.values.find((value) => value.id === selectedValues[option.id])?.value}</span></div><div className="flex flex-wrap gap-2">{option.values.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)).map((value) => <button key={value.id} type="button" onClick={() => { setSelectedValues((current) => ({ ...current, [option.id]: value.id })); setNotice(''); }} className={`rounded-xl border px-3.5 py-2 text-sm transition ${selectedValues[option.id] === value.id ? 'border-[#5367C9] bg-[#EEF3FF] font-semibold text-[#465BB7]' : 'border-[#E1E7F1] text-[#626C82] hover:border-[#AAB8E8]'}`}>{value.value}</button>)}</div></div>)}

            {selectedVariant && <p className="mt-4 text-xs text-[#8A93A6]">Mã SKU: {selectedVariant.sku || 'Đang cập nhật'}</p>}
            <div className="mt-5 flex flex-wrap items-center gap-3"><span className="text-sm font-semibold text-[#343D55]">Số lượng</span><div className="flex items-center overflow-hidden rounded-xl border border-[#DFE6F2]"><button type="button" aria-label="Giảm số lượng" disabled={quantity <= 1} onClick={() => setQuantity((n) => Math.max(1, n - 1))} className="flex h-10 w-10 items-center justify-center text-[#626C82] disabled:opacity-40"><Minus size={15}/></button><span className="min-w-10 text-center text-sm font-semibold text-[#343D55]">{quantity}</span><button type="button" aria-label="Tăng số lượng" disabled={stock != null && quantity >= stock} onClick={() => setQuantity((n) => Math.min(stock ?? 99, n + 1))} className="flex h-10 w-10 items-center justify-center text-[#626C82] disabled:opacity-40"><Plus size={15}/></button></div><span className="text-xs text-[#8A93A6]">{outOfStock ? 'Sản phẩm đã hết hàng' : stock != null ? `Có thể mua tối đa ${stock} sản phẩm` : 'Số lượng có giới hạn'}</span></div>

            {notice && <div role="status" className="mt-4 rounded-xl border border-[#DCE5FA] bg-[#F4F7FF] p-3 text-sm text-[#5367C9]">{notice}</div>}
            <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto]"><button type="button" disabled={outOfStock || isAddingToCart} onClick={handleAddToCart} className="flex min-h-12 items-center justify-center gap-2 rounded-full bg-[#5367C9] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#4559B7] disabled:cursor-not-allowed disabled:bg-[#B8C0D9]"><ShoppingCart size={18}/> {isAddingToCart ? 'Đang thêm...' : 'Thêm vào giỏ hàng'}</button><button type="button" onClick={() => { if (requireLogin('Đăng nhập để thêm sản phẩm vào danh sách yêu thích.')) setNotice('Chức năng yêu thích sẽ được kết nối khi API danh sách yêu thích được triển khai.'); }} className="flex min-h-12 items-center justify-center gap-2 rounded-full border border-[#DCE4F2] px-5 py-3 text-sm font-semibold text-[#59647D] transition hover:border-[#AAB8E8] hover:text-[#5367C9]"><Heart size={18}/> Yêu thích</button></div>
            <div className="mt-6 grid gap-3 border-t border-[#EDF0F6] pt-5 sm:grid-cols-2"><div className="flex gap-3"><Truck size={19} className="mt-0.5 shrink-0 text-[#6E7FCB]"/><div><p className="text-sm font-semibold text-[#343D55]">Thông tin giao hàng</p><p className="mt-1 text-xs leading-5 text-[#8A93A6]">Chi phí và thời gian giao hàng sẽ được hiển thị khi thanh toán.</p></div></div><div className="flex gap-3"><ShieldCheck size={19} className="mt-0.5 shrink-0 text-[#6E7FCB]"/><div><p className="text-sm font-semibold text-[#343D55]">Mua sắm trên Kala</p><p className="mt-1 text-xs leading-5 text-[#8A93A6]">Thông tin sản phẩm được cung cấp bởi cửa hàng.</p></div></div></div>
          </div>
        </section>

        {product.shop && <section className="mt-6 flex flex-col gap-4 rounded-3xl border border-[#E7ECF5] bg-white p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6"><div className="flex items-center gap-4"><img src={product.shop.logoUrl || 'https://placehold.co/100x100/e8f2ff/315b8a?text=Shop'} alt={product.shop.name} className="h-16 w-16 rounded-2xl border border-[#EDF0F6] object-cover"/><div><div className="text-xs text-[#8A93A6]">Được bán bởi</div><h2 className="mt-1 font-bold text-[#202642]">{product.shop.name}</h2><div className="mt-1 flex items-center gap-3 text-xs text-[#7B8498]"><span className="flex items-center gap-1"><Star size={13} fill="currentColor" className="text-[#E7A92B]"/>{Number(product.shop.ratingAverage ?? 0).toFixed(1)}</span><span>{product.shop.followersCount ?? 0} người theo dõi</span></div></div></div><div className="grid grid-cols-1 gap-2 sm:grid-cols-2"><button type="button" onClick={() => navigate(`/chat?shopId=${encodeURIComponent(product.shop!.id)}`)} className="inline-flex items-center justify-center gap-2 rounded-full bg-[#5367C9] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#4559B7]"><MessageCircle size={16}/> Chat</button><Link to={`/shop/${product.shop.slug || product.shop.id}`} className="inline-flex items-center justify-center gap-2 rounded-full border border-[#DCE4F2] px-5 py-2.5 text-sm font-semibold text-[#5367C9] hover:bg-[#F4F7FF]"><Store size={16}/> Xem cửa hàng <ChevronRight size={15}/></Link></div></section>}

        <section className="mt-6 rounded-3xl border border-[#E7ECF5] bg-white p-5 sm:p-7"><h2 className="text-lg font-bold text-[#202642] sm:text-xl">Mô tả sản phẩm</h2><div className="mt-4 whitespace-pre-line text-sm leading-7 text-[#626C82]">{product.description || 'Cửa hàng chưa cập nhật mô tả cho sản phẩm này.'}</div><div className="mt-5 flex flex-wrap gap-2 text-xs text-[#7B8498]">{product.category && <span className="rounded-full bg-[#F3F6FC] px-3 py-1.5">Danh mục: {product.category.name}</span>}{product.tags?.map((tag, index) => { const label = typeof tag === 'string' ? tag : tag.slug || tag.name; return label ? <span key={(typeof tag === 'string' ? tag : tag.id) || `${label}-${index}`} className="rounded-full border border-[#E1E7F1] bg-white px-3 py-1.5 text-[#5367C9]">#{label.replace(/^#/, '')}</span> : null; })}</div></section>

        {shopProducts.length > 0 && <ProductShelf title="Sản phẩm khác của cửa hàng" products={shopProducts} />}
        {relatedProducts.length > 0 && <ProductShelf title="Sản phẩm liên quan" products={relatedProducts} />}

        <div className="mt-6"><button type="button" onClick={() => navigate(-1)} className="inline-flex items-center gap-2 text-sm font-semibold text-[#5367C9] hover:text-[#394DAA]"><ArrowLeft size={16}/> Quay lại trang trước</button></div>
      </div>
    </main>
  );
}


function ProductShelf({ title, products }: { title: string; products: import('../../services/catalog/productApi').ProductCardDto[] }) {
  return (
    <section className="mt-6 rounded-3xl border border-[#E7ECF5] bg-white p-5 sm:p-6">
      <h2 className="text-lg font-bold text-[#202642] sm:text-xl">{title}</h2>
      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6 lg:gap-4">
        {products.map((item) => (
          <ProductCard key={item.id} product={item} />
        ))}
      </div>
    </section>
  );
}
