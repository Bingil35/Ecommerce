import { Store } from 'lucide-react';
import { Link } from 'react-router-dom';

import type {
  CartShop,
  ShopSummary,
} from '../../../services/cart/cartTypes';
import { getShopPath, isItemSelectable } from '../../../services/cart/cartUtils';
import { formatVnd } from '../../../services/catalog/productApi';
import type { ShopVoucher } from '../../../services/voucher/voucherTypes';
import CartItem from './CartItem';
import ShopVoucherBox from './ShopVoucherBox';

interface CartShopSectionProps {
  shop: CartShop;
  selectedIds: Set<string>;
  summary: ShopSummary;
  onToggleItem: (itemId: string) => void;
  onToggleShop: (shop: CartShop, checked: boolean) => void;
  onVoucherChange: (shopId: string, voucher: ShopVoucher | null) => void;
}

const CartShopSection = ({
  shop,
  selectedIds,
  summary,
  onToggleItem,
  onToggleShop,
  onVoucherChange,
}: CartShopSectionProps) => {
  const selectableItems = shop.items.filter(isItemSelectable);
  const selectedCount = selectableItems.filter((i) =>
    selectedIds.has(i.id)
  ).length;
  const allSelected =
    selectableItems.length > 0 && selectedCount === selectableItems.length;
  const someSelected = selectedCount > 0 && !allSelected;

  return (
    <section className="overflow-hidden rounded-2xl border border-[#E8EDF5] bg-white shadow-[0_4px_18px_rgba(72,93,140,0.05)]">
      {/* Header shop */}
      <div className="flex items-center gap-3 border-b border-[#E8EDF5] bg-[#FBFCFF] px-4 py-3 sm:px-5">
        <input
          type="checkbox"
          checked={allSelected}
          disabled={selectableItems.length === 0}
          ref={(el) => {
            if (el) el.indeterminate = someSelected;
          }}
          onChange={(e) => onToggleShop(shop, e.target.checked)}
          aria-label={`Chọn tất cả sản phẩm của ${shop.shopName}`}
          className="h-4 w-4 shrink-0 cursor-pointer accent-[#5367C9] disabled:cursor-not-allowed"
        />
        <Store size={17} strokeWidth={1.8} className="text-[#5367C9]" />
        <h2 className="min-w-0 text-sm font-semibold">
          <Link
            to={getShopPath(shop)}
            title={`Xem trang chủ ${shop.shopName}`}
            className="block truncate text-[#202642] transition-colors hover:text-[#5367C9] hover:underline"
          >
            {shop.shopName}
          </Link>
        </h2>
      </div>

      {/* Sản phẩm */}
      <div className="divide-y divide-[#EEF2F8]">
        {shop.items.map((item) => (
          <CartItem
            key={item.id}
            item={item}
            selected={selectedIds.has(item.id)}
            onToggleSelect={onToggleItem}
          />
        ))}
      </div>

      {/* Voucher của shop */}
      <ShopVoucherBox
        shopId={shop.shopId}
        selectedSubtotal={summary.subtotal}
        voucher={summary.voucher}
        discount={summary.discount}
        missingForVoucher={summary.missingForVoucher}
        onChange={(voucher) => onVoucherChange(shop.shopId, voucher)}
      />

      {/* Tổng tiền shop (chỉ hiện khi có sản phẩm được chọn) */}
      {summary.selectedCount > 0 && (
        <div className="flex flex-wrap items-baseline justify-end gap-x-3 gap-y-1 border-t border-[#E8EDF5] bg-[#FBFCFF] px-4 py-3 text-sm sm:px-5">
          <span className="text-[#667085]">
            Tổng shop ({summary.selectedQuantity} sản phẩm):
          </span>
          {summary.discount > 0 && (
            <span className="text-xs text-[#9AA2B1] line-through">
              {formatVnd(summary.subtotal)}
            </span>
          )}
          <span className="text-base font-bold text-[#5367C9]">
            {formatVnd(summary.total)}
          </span>
        </div>
      )}
    </section>
  );
};

export default CartShopSection;