import { Heart, Star } from 'lucide-react';

import type { HomeProduct } from './productData';

interface ProductCardProps {
  product: HomeProduct;
}

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('vi-VN').format(price);
};

const formatSold = (sold: number) => {
  if (sold >= 1000) {
    return `${(sold / 1000).toFixed(1).replace('.0', '')}k`;
  }

  return sold.toString();
};

const ProductCard = ({ product }: ProductCardProps) => {
  return (
    <article
      className="
        group
        min-w-0
        overflow-hidden
        rounded-2xl
        border
        border-[#E8EDF5]
        bg-white
        shadow-[0_4px_18px_rgba(72,93,140,0.05)]
        transition-all
        duration-200
        hover:-translate-y-1
        hover:border-[#DCE4F4]
        hover:shadow-[0_10px_28px_rgba(72,93,140,0.10)]
      "
    >
      {/* Image */}
      <div className="relative aspect-square overflow-hidden bg-[#F3F6FC]">
        <img
          src={product.image}
          alt={product.name}
          className="
            h-full
            w-full
            object-cover
            transition-transform
            duration-300
            group-hover:scale-[1.03]
          "
        />

        {product.discount && (
          <span
            className="
              absolute
              left-2
              top-2
              rounded-md
              bg-[#5367C9]
              px-2
              py-1
              text-[10px]
              font-semibold
              text-white
            "
          >
            -{product.discount}%
          </span>
        )}

        <button
          type="button"
          aria-label="Thêm vào yêu thích"
          className="
            absolute
            right-2
            top-2
            flex
            h-8
            w-8
            items-center
            justify-center
            rounded-full
            bg-white/90
            text-[#667085]
            opacity-0
            shadow-sm
            backdrop-blur-sm
            transition-all
            duration-200
            hover:text-[#5367C9]
            group-hover:opacity-100
          "
        >
          <Heart
            size={16}
            strokeWidth={1.8}
          />
        </button>
      </div>

      {/* Information */}
      <div className="p-3">
        <h3
          className="
            line-clamp-2
            min-h-[40px]
            text-sm
            font-medium
            leading-5
            text-[#252A3A]
          "
        >
          {product.name}
        </h3>

        {/* Price */}
        <div className="mt-2 flex flex-wrap items-baseline gap-x-2">
          <span
            className="
              text-base
              font-bold
              text-[#5367C9]
            "
          >
            {formatPrice(product.price)}đ
          </span>

          {product.originalPrice && (
            <span
              className="
                text-xs
                text-[#9AA2B1]
                line-through
              "
            >
              {formatPrice(product.originalPrice)}đ
            </span>
          )}
        </div>

        {/* Rating + sold */}
        <div className="mt-2 flex items-center justify-between">
          <div className="flex items-center gap-1">
            <Star
              size={13}
              fill="currentColor"
              strokeWidth={1.5}
              className="text-[#F3B63F]"
            />

            <span className="text-xs text-[#667085]">
              {product.rating}
            </span>
          </div>

          <span className="text-xs text-[#98A1B2]">
            Đã bán {formatSold(product.sold)}
          </span>
        </div>
      </div>
    </article>
  );
};

export default ProductCard;