import { Link } from 'react-router-dom';
import type { Product } from '../services/product/productTypes.ts';

export const formatPrice = (value: number) =>
  new Intl.NumberFormat('vi-VN').format(value) + 'đ';

interface ProductCardProps {
  product: Product;
}

export default function ProductCard({ product }: ProductCardProps) {
  const discount = product.comparePrice
    ? Math.round((1 - product.price / product.comparePrice) * 100)
    : 0;

  return (
    <Link
      to={`/product/${product.slug}`}
      className="group flex flex-col overflow-hidden rounded-2xl border border-[#E1E7F2] bg-white transition hover:border-[#5B8DEF] hover:shadow-[0_12px_30px_rgba(79,124,255,0.12)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#5B8DEF]/20"
    >
      <div className="relative flex aspect-square items-center justify-center bg-[#EEF4FF]">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-full w-full object-cover"
          />
        ) : (
          <span className="text-6xl font-bold text-[#4F7CFF]/40">
            {product.name.charAt(0)}
          </span>
        )}

        {discount > 0 && (
          <span className="absolute left-3 top-3 rounded-full bg-[#4F7CFF] px-2.5 py-1 text-xs font-semibold text-white">
            -{discount}%
          </span>
        )}

        {!product.inStock && (
          <span className="absolute inset-0 flex items-center justify-center bg-white/70 text-sm font-semibold text-[#52638F]">
            Hết hàng
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col p-4">
        <h3 className="line-clamp-2 min-h-[2.5rem] text-sm font-medium leading-5 text-[#172033]">
          {product.name}
        </h3>

        <p className="mt-1 text-xs text-[#667085]">
          {product.brand} · {product.shopName}
        </p>

        <div className="mt-3 flex flex-wrap items-baseline gap-x-2">
          <span className="text-base font-bold text-[#29266F]">
            Từ {formatPrice(product.price)}
          </span>

          {product.comparePrice && (
            <span className="text-xs text-[#9AA8C6] line-through">
              {formatPrice(product.comparePrice)}
            </span>
          )}
        </div>

        <div className="mt-auto flex items-center justify-between pt-3 text-xs text-[#667085]">
          <span>
            ★ {product.rating.toFixed(1)} ({product.reviewCount})
          </span>
          <span>Đã bán {product.sold.toLocaleString('vi-VN')}</span>
        </div>
      </div>
    </Link>
  );
}