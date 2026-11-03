import { Heart, LoaderCircle, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import {
  formatVnd,
  productImage,
  type ProductCardDto,
} from '../../../services/catalog/productApi';
import {
  comparePriceOf,
  discountPercentOf,
  isOutOfStock,
  priceOf,
  ratingOf,
} from '../../../services/wishlist/wishlistUtils';

interface WishlistProductCardProps {
  product: ProductCardDto;
  /** Đang gọi API xóa sản phẩm này */
  isRemoving: boolean;
  onRemove: (product: ProductCardDto) => void;
}

/** Card dùng cùng bố cục với ProductCard ở trang chủ / tìm kiếm. */
const WishlistProductCard = ({
  product,
  isRemoving,
  onRemove,
}: WishlistProductCardProps) => {
  const navigate = useNavigate();

  const price = priceOf(product);
  const comparePrice = comparePriceOf(product);
  const discountPercent = discountPercentOf(product);
  const outOfStock = isOutOfStock(product);
  const ratingCount = product.ratingCount ?? 0;
  const productPath = `/product/${product.slug || product.id}`;
  const openProduct = () => navigate(productPath);

  return (
    <article className="group min-w-0 overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white shadow-[0_4px_18px_rgba(72,93,140,0.05)] transition-all hover:-translate-y-1 hover:shadow-[0_10px_28px_rgba(72,93,140,0.10)]">
      <div
        className="relative aspect-square cursor-pointer overflow-hidden bg-[#F3F6FC]"
        onClick={openProduct}
        role="link"
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && openProduct()}
      >
        <img
          src={productImage(product)}
          alt={product.name}
          loading="lazy"
          className={`h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03] ${
            outOfStock ? 'opacity-60' : ''
          }`}
        />

        {product.featured && (
          <span className="absolute left-2 top-2 rounded-md bg-[#5367C9] px-2 py-1 text-[10px] font-semibold text-white">
            Nổi bật
          </span>
        )}

        {discountPercent != null && (
          <span className="absolute bottom-2 left-2 rounded-md bg-[#FFECEE] px-2 py-1 text-[10px] font-bold text-[#D95368]">
            -{discountPercent}%
          </span>
        )}

        <button
          type="button"
          aria-label={`Xóa ${product.name} khỏi yêu thích`}
          title="Xóa khỏi yêu thích"
          disabled={isRemoving}
          onClick={(e) => {
            e.stopPropagation();
            onRemove(product);
          }}
          className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-[#E5334F] shadow-sm transition disabled:cursor-wait disabled:opacity-70"
        >
          {isRemoving ? (
            <LoaderCircle size={16} className="animate-spin" />
          ) : (
            <Heart size={16} strokeWidth={1.8} fill="currentColor" />
          )}
        </button>
      </div>

      <button
        type="button"
        onClick={openProduct}
        className="block w-full p-3 text-left"
      >
        <h3 className="line-clamp-2 min-h-[40px] text-sm font-medium leading-5 text-[#252A3A]">
          {product.name}
        </h3>

        <div className="mt-2 flex flex-wrap items-baseline gap-x-1.5 gap-y-1">
          <span className="text-base font-bold text-[#5367C9]">
            {price != null ? formatVnd(price) : 'Liên hệ'}
          </span>
          {comparePrice != null && (
            <span className="text-xs text-[#9AA2B1] line-through">
              {formatVnd(comparePrice)}
            </span>
          )}
        </div>

        <div className="mt-2 flex items-center justify-between">
          <span className="flex items-center gap-1">
            <Star size={13} fill="currentColor" className="text-[#F3B63F]" />
            <span className="text-xs text-[#667085]">
              {ratingOf(product).toFixed(1)}
              {ratingCount ? ` (${ratingCount})` : ''}
            </span>
          </span>
          <span className="text-xs text-[#98A1B2]">
            {product.stockQuantity != null
              ? product.stockQuantity > 0
                ? `Còn ${product.stockQuantity}`
                : 'Hết hàng'
              : ''}
          </span>
        </div>
      </button>

    </article>
  );
};

export default WishlistProductCard;
