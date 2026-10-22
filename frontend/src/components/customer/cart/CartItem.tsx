import { Trash2, Minus, Plus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { CartItem as CartItemType } from '../../../services/cart/cartTypes';
import {
  getLineTotal,
  getProductPath,
  isItemSelectable,
} from '../../../services/cart/cartUtils';
import { formatVnd } from '../../../services/catalog/productApi';
import { useCart } from '../../../context/CartContext';
import { useState } from 'react';

interface CartItemProps {
  item: CartItemType;
  /** Sản phẩm có đang được tích chọn để thanh toán không */
  selected: boolean;
  onToggleSelect: (itemId: string) => void;
}

const CartItem = ({ item, selected, onToggleSelect }: CartItemProps) => {
  const navigate = useNavigate();
  const { updateQuantity, removeFromCart } = useCart();
  const [isRemoving, setIsRemoving] = useState(false);

  // Xóa ngay, không hỏi xác nhận
  const handleRemove = () => {
    setIsRemoving(true);
    setTimeout(async () => {
      const ok = await removeFromCart(item.id);
      if (!ok) setIsRemoving(false);
    }, 200);
  };

  const handleQuantityChange = (newQuantity: number) => {
    if (newQuantity < 1) return;
    if (item.stockQuantity != null && newQuantity > item.stockQuantity) {
      alert(`Chỉ còn ${item.stockQuantity} sản phẩm trong kho.`);
      return;
    }
    updateQuantity(item.id, newQuantity);
  };

  // BE đã chọn sẵn ảnh variant, hoặc ảnh sản phẩm nếu variant không có ảnh
  const imageSrc =
    item.imageUrl || 'https://placehold.co/600x600/F2F5FC/6575B8?text=Kala';

  const isOutOfStock =
    item.stockQuantity != null && item.stockQuantity === 0;

  const isUnavailable = item.available === false;
  const selectable = isItemSelectable(item);

  // Nhãn phân loại dưới tên sản phẩm: từng option "Tên: Giá trị" (ví dụ "Dung tích: 50ml"),
  // không thì dùng variantName BE ghép sẵn. Variant chưa gắn option thì không có nhãn.
  const variantLabels: string[] =
    item.selectedOptions && item.selectedOptions.length > 0
      ? item.selectedOptions.map((opt) => `${opt.optionName}: ${opt.value}`)
      : item.variantName
        ? [item.variantName]
        : [];

  return (
    <div
      className={`transition-all duration-200 ${
        isRemoving
          ? 'scale-95 opacity-0'
          : isOutOfStock || isUnavailable
            ? 'opacity-60'
            : 'opacity-100'
      } ${selected && selectable ? 'bg-[#F8FAFF]' : ''}`}
    >
      <div className="flex items-center gap-3 p-4 sm:gap-4 sm:p-5">
        {/* Checkbox chọn sản phẩm */}
        <input
          type="checkbox"
          checked={selected && selectable}
          disabled={!selectable}
          onChange={() => onToggleSelect(item.id)}
          aria-label={`Chọn ${item.productName}`}
          className="h-4 w-4 shrink-0 cursor-pointer accent-[#5367C9] disabled:cursor-not-allowed"
        />

        {/* Image */}
        <button
          type="button"
          onClick={() => navigate(getProductPath(item))}
          className="shrink-0 overflow-hidden rounded-lg transition-transform hover:scale-105"
        >
          <img
            src={imageSrc}
            alt={item.productName}
            className="h-20 w-20 object-cover sm:h-24 sm:w-24"
          />
        </button>

        {/* Info */}
        <div className="flex min-w-0 flex-1 flex-col self-stretch">
          <div className="mb-2 flex items-start justify-between gap-3">
            <div className="min-w-0 flex-1">
              <button
                type="button"
                onClick={() => navigate(getProductPath(item))}
                className="group/link block w-full text-left"
              >
                <h3 className="line-clamp-2 text-sm font-semibold leading-5 text-[#252A3A] transition-colors group-hover/link:text-[#5367C9]">
                  {item.productName}
                </h3>
              </button>

              {/* Phân loại (variant) hiển thị ngay dưới tên sản phẩm */}
              {variantLabels.length > 0 && (
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {variantLabels.map((label) => (
                    <span
                      key={label}
                      className="rounded-md bg-[#F3F6FC] px-2 py-0.5 text-xs text-[#667085]"
                    >
                      {label}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* Remove button */}
            <button
              type="button"
              onClick={handleRemove}
              aria-label="Xóa sản phẩm"
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[#98A1B2] transition-colors hover:bg-[#FFF5F5] hover:text-[#DC5A5A]"
            >
              <Trash2 size={16} strokeWidth={1.8} />
            </button>
          </div>

          {/* Price & Quantity */}
          <div className="mt-auto flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-col gap-1">
              <div className="flex items-baseline gap-2">
                <span className="text-base font-bold text-[#5367C9] sm:text-lg">
                  {formatVnd(item.price)}
                </span>
                {item.comparePrice && item.comparePrice > item.price && (
                  <span className="text-xs text-[#9AA2B1] line-through">
                    {formatVnd(item.comparePrice)}
                  </span>
                )}
              </div>

              {isOutOfStock ? (
                <span className="text-xs font-medium text-[#DC5A5A]">
                  Hết hàng
                </span>
              ) : isUnavailable ? (
                <span className="text-xs font-medium text-[#DC5A5A]">
                  Không đủ hàng hoặc đã ngừng bán
                </span>
              ) : item.stockQuantity != null && item.stockQuantity < 10 ? (
                <span className="text-xs text-[#F59E0B]">
                  Chỉ còn {item.stockQuantity} sản phẩm
                </span>
              ) : null}
            </div>

            <div className="flex flex-col items-end gap-1.5">
              {/* Quantity selector */}
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleQuantityChange(item.quantity - 1)}
                  disabled={item.quantity <= 1 || isOutOfStock}
                  aria-label="Giảm số lượng"
                  className="flex h-8 w-8 items-center justify-center rounded-lg border border-[#E8EDF5] bg-white text-[#667085] transition-colors hover:border-[#5367C9] hover:bg-[#F3F6FC] hover:text-[#5367C9] disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-[#E8EDF5] disabled:hover:bg-white disabled:hover:text-[#667085]"
                >
                  <Minus size={14} strokeWidth={2} />
                </button>

                <input
                  type="number"
                  min="1"
                  max={item.stockQuantity}
                  value={item.quantity}
                  onChange={(e) => {
                    const val = parseInt(e.target.value) || 1;
                    handleQuantityChange(val);
                  }}
                  disabled={isOutOfStock}
                  className="h-8 w-14 rounded-lg border border-[#E8EDF5] bg-white text-center text-sm font-semibold text-[#252A3A] transition-colors focus:border-[#5367C9] focus:outline-none focus:ring-2 focus:ring-[#5367C9]/20 disabled:cursor-not-allowed disabled:opacity-40"
                />

                <button
                  type="button"
                  onClick={() => handleQuantityChange(item.quantity + 1)}
                  disabled={
                    isOutOfStock ||
                    (item.stockQuantity != null &&
                      item.quantity >= item.stockQuantity)
                  }
                  aria-label="Tăng số lượng"
                  className="flex h-8 w-8 items-center justify-center rounded-lg border border-[#E8EDF5] bg-white text-[#667085] transition-colors hover:border-[#5367C9] hover:bg-[#F3F6FC] hover:text-[#5367C9] disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-[#E8EDF5] disabled:hover:bg-white disabled:hover:text-[#667085]"
                >
                  <Plus size={14} strokeWidth={2} />
                </button>
              </div>

              {selectable && (
                <span className="text-xs text-[#667085]">
                  Thành tiền:{' '}
                  <span className="font-semibold text-[#252A3A]">
                    {formatVnd(getLineTotal(item))}
                  </span>
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CartItem;