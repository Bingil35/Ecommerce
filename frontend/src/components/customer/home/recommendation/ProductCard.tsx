import { Heart, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../../context/AuthContext';
import type { HomeProduct } from './productData';
import { formatVnd, productImage } from '../../../../services/catalog/productApi';
interface ProductCardProps { product: HomeProduct; }
export default function ProductCard({ product }: ProductCardProps) {
  const navigate = useNavigate(); const { isAuthenticated } = useAuth();
  const price = product.minPrice ?? product.price;
  const comparePrice = product.comparePrice;
  const hasDiscount = comparePrice != null && Number(comparePrice) > Number(price);
  const discountPercent = hasDiscount ? Math.round((1 - Number(price) / Number(comparePrice)) * 100) : 0;
  const productPath = `/product/${product.slug || product.id}`;
  const openProduct = () => navigate(productPath);
  return <article className="group min-w-0 overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white shadow-[0_4px_18px_rgba(72,93,140,0.05)] transition-all hover:-translate-y-1 hover:shadow-[0_10px_28px_rgba(72,93,140,0.10)]">
    <div className="relative aspect-square cursor-pointer overflow-hidden bg-[#F3F6FC]" onClick={openProduct} role="link" tabIndex={0} onKeyDown={(e) => e.key === 'Enter' && openProduct()}>
      <img src={productImage(product)} alt={product.name} loading="lazy" className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]" />
      {product.featured && <span className="absolute left-2 top-2 rounded-md bg-[#5367C9] px-2 py-1 text-[10px] font-semibold text-white">Nổi bật</span>}
      {hasDiscount && <span className="absolute bottom-2 left-2 rounded-md bg-[#FFECEE] px-2 py-1 text-[10px] font-bold text-[#D95368]">-{discountPercent}%</span>}
      <button type="button" aria-label="Thêm vào yêu thích" onClick={(e) => { e.stopPropagation(); if (!isAuthenticated) navigate('/login', { state: { from: productPath, notice: 'Đăng nhập để thêm sản phẩm vào yêu thích.' } }); else window.alert('Chức năng yêu thích sẽ được kết nối ở sprint tiếp theo.'); }} className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-[#667085] shadow-sm transition hover:text-[#5367C9]"><Heart size={16} strokeWidth={1.8}/></button>
    </div>
    <button type="button" onClick={openProduct} className="block w-full p-3 text-left">
      <h3 className="line-clamp-2 min-h-[40px] text-sm font-medium leading-5 text-[#252A3A]">{product.name}</h3>
      <div className="mt-2 flex flex-wrap items-baseline gap-x-1.5 gap-y-1"><span className="text-base font-bold text-[#5367C9]">{formatVnd(price)}</span>{hasDiscount && <span className="text-xs text-[#9AA2B1] line-through">{formatVnd(comparePrice)}</span>}</div>
      <div className="mt-2 flex items-center justify-between"><span className="flex items-center gap-1"><Star size={13} fill="currentColor" className="text-[#F3B63F]"/><span className="text-xs text-[#667085]">{Number(product.ratingAverage ?? 0).toFixed(1)}{product.ratingCount ? ` (${product.ratingCount})` : ''}</span></span><span className="text-xs text-[#98A1B2]">{product.stockQuantity != null ? (product.stockQuantity > 0 ? `Còn ${product.stockQuantity}` : 'Hết hàng') : ''}</span></div>
    </button>
  </article>;
}
