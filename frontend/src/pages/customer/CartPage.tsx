import { useMemo, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import CartShopSection from '../../components/customer/cart/CartShopSection';
import ShopVoucherBox from '../../components/customer/cart/ShopVoucherBox';
import EmptyCart from '../../components/customer/cart/EmptyCart';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import type { CartShop } from '../../services/cart/cartTypes';
import {
  calcOrderSummary,
  calcShopSummary,
  isItemSelectable,
} from '../../services/cart/cartUtils';
import { formatVnd } from '../../services/catalog/productApi';
import type { ShopVoucher } from '../../services/voucher/voucherTypes';

const CartPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { shops, itemCount, isLoading, error, refresh } = useCart();

  // Các sản phẩm đang được tích chọn để thanh toán
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  // Voucher đã chọn cho từng shop (key = shopId)
  const [vouchers, setVouchers] = useState<Record<string, ShopVoucher>>({});
  // Voucher tổng (áp dụng cho cả đơn)
  const [orderVoucher, setOrderVoucher] = useState<ShopVoucher | null>(null);

  // Chỉ những sản phẩm còn tồn tại trong giỏ và còn mua được mới tính là "đã chọn"
  const selectableIds = useMemo(
    () =>
      new Set(
        shops.flatMap((shop) =>
          shop.items.filter(isItemSelectable).map((item) => item.id)
        )
      ),
    [shops]
  );

  const effectiveSelected = useMemo(
    () => new Set([...selectedIds].filter((id) => selectableIds.has(id))),
    [selectedIds, selectableIds]
  );

  // Tính lại tiền từng shop mỗi khi đổi sản phẩm chọn / số lượng / voucher
  const shopSummaries = useMemo(
    () =>
      shops.map((shop) =>
        calcShopSummary(shop, effectiveSelected, vouchers[shop.shopId])
      ),
    [shops, effectiveSelected, vouchers]
  );

  // Tổng cả giỏ: trước giảm -> trừ voucher shop -> trừ voucher tổng -> sau giảm
  const totals = useMemo(
    () => calcOrderSummary(shopSummaries, orderVoucher),
    [shopSummaries, orderVoucher]
  );

  if (isAuthLoading) return null;

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname,
          notice: 'Đăng nhập để sử dụng giỏ hàng.',
        }}
      />
    );
  }

  const isEmpty = shops.length === 0;
  const selectedCount = effectiveSelected.size;
  const allSelected =
    selectableIds.size > 0 && selectedCount === selectableIds.size;

  const handleToggleItem = (itemId: string) => {
    const next = new Set(effectiveSelected);
    if (next.has(itemId)) next.delete(itemId);
    else next.add(itemId);
    setSelectedIds(next);
  };

  const handleToggleShop = (shop: CartShop, checked: boolean) => {
    const next = new Set(effectiveSelected);
    shop.items.filter(isItemSelectable).forEach((item) => {
      if (checked) next.add(item.id);
      else next.delete(item.id);
    });
    setSelectedIds(next);
  };

  const handleToggleAll = () => {
    setSelectedIds(allSelected ? new Set() : new Set(selectableIds));
  };

  const handleVoucherChange = (shopId: string, voucher: ShopVoucher | null) => {
    setVouchers((prev) => {
      const next = { ...prev };
      if (voucher) next[shopId] = voucher;
      else delete next[shopId];
      return next;
    });
  };

  const handleCheckout = () => {
    if (selectedCount === 0) return;

    // Dữ liệu sẽ gửi sang bước thanh toán
    const payload = {
      cartItemIds: [...effectiveSelected],
      shopVouchers: shopSummaries
        .filter((s) => s.voucher && s.discount > 0)
        .map((s) => ({ shopId: s.shopId, voucherId: s.voucher!.id })),
      orderVoucherId: totals.orderDiscount > 0 ? orderVoucher?.id ?? null : null,
    };

    // TODO: khi làm trang thanh toán -> navigate('/checkout', { state: payload })
    console.info('[checkout] payload', payload);
    window.alert('Chức năng thanh toán chưa được triển khai.');
  };

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-6 sm:py-9">
      <h1 className="mb-6 text-2xl font-bold text-[#202642]">
        Giỏ hàng{itemCount > 0 ? ` (${itemCount})` : ''}
      </h1>

      {isLoading && isEmpty ? (
        <p className="py-20 text-center text-sm text-[#667085]">
          Đang tải giỏ hàng...
        </p>
      ) : error && isEmpty ? (
        <div className="rounded-2xl border border-[#E8EDF5] bg-white px-6 py-16 text-center">
          <p className="mb-4 text-sm text-[#DC5A5A]">{error}</p>
          <button
            type="button"
            onClick={() => void refresh()}
            className="rounded-full bg-[#5367C9] px-6 py-2.5 text-sm font-semibold text-white hover:bg-[#4659B8]"
          >
            Thử lại
          </button>
        </div>
      ) : isEmpty ? (
        <EmptyCart />
      ) : (
        <div className="grid items-start gap-6 lg:grid-cols-[1fr_360px]">
          <div className="space-y-5">
            {/* Chọn tất cả */}
            <div className="flex items-center justify-between rounded-2xl border border-[#E8EDF5] bg-white px-4 py-3 shadow-[0_4px_18px_rgba(72,93,140,0.05)] sm:px-5">
              <label className="flex cursor-pointer items-center gap-3 text-sm font-medium text-[#252A3A]">
                <input
                  type="checkbox"
                  checked={allSelected}
                  disabled={selectableIds.size === 0}
                  onChange={handleToggleAll}
                  className="h-4 w-4 cursor-pointer accent-[#5367C9] disabled:cursor-not-allowed"
                />
                Chọn tất cả ({selectableIds.size} sản phẩm)
              </label>
              <span className="text-xs text-[#667085]">
                Đã chọn {selectedCount}
              </span>
            </div>

            {shops.map((shop, index) => (
              <CartShopSection
                key={shop.id}
                shop={shop}
                selectedIds={effectiveSelected}
                summary={shopSummaries[index]}
                onToggleItem={handleToggleItem}
                onToggleShop={handleToggleShop}
                onVoucherChange={handleVoucherChange}
              />
            ))}
          </div>

          <aside className="h-fit rounded-2xl border border-[#E8EDF5] bg-white p-5 shadow-[0_4px_18px_rgba(72,93,140,0.05)] lg:sticky lg:top-24">
            <h2 className="mb-4 text-lg font-bold text-[#202642]">
              Tóm tắt đơn hàng
            </h2>

            {/* Voucher tổng */}
            <ShopVoucherBox
              shopId={null}
              label="Voucher tổng"
              title="Chọn voucher tổng"
              className="mb-4 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-xl border border-dashed border-[#D9E0F2] bg-[#FBFCFF] px-3 py-3"
              selectedSubtotal={totals.afterShopDiscount}
              voucher={orderVoucher}
              discount={totals.orderDiscount}
              missingForVoucher={totals.missingForOrderVoucher}
              onChange={setOrderVoucher}
            />

            <dl className="space-y-3 text-sm text-[#667085]">
              <div className="flex justify-between">
                <dt>Tạm tính ({totals.selectedQuantity} sản phẩm)</dt>
                <dd className="font-medium text-[#252A3A]">
                  {formatVnd(totals.subtotal)}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>Giảm giá voucher shop</dt>
                <dd
                  className={
                    totals.shopDiscount > 0
                      ? 'font-medium text-[#16A34A]'
                      : 'font-medium text-[#252A3A]'
                  }
                >
                  {totals.shopDiscount > 0
                    ? `-${formatVnd(totals.shopDiscount)}`
                    : formatVnd(0)}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>Giảm giá voucher tổng</dt>
                <dd
                  className={
                    totals.orderDiscount > 0
                      ? 'font-medium text-[#16A34A]'
                      : 'font-medium text-[#252A3A]'
                  }
                >
                  {totals.orderDiscount > 0
                    ? `-${formatVnd(totals.orderDiscount)}`
                    : formatVnd(0)}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt>Phí vận chuyển</dt>
                <dd className="text-xs">Tính ở bước thanh toán</dd>
              </div>
              <div className="flex items-start justify-between border-t border-[#E8EDF5] pt-3 text-base">
                <dt className="font-semibold text-[#202642]">Tổng cộng</dt>
                <dd className="text-right">
                  {totals.totalDiscount > 0 && (
                    <div className="text-xs font-normal text-[#9AA2B1] line-through">
                      {formatVnd(totals.subtotal)}
                    </div>
                  )}
                  <div className="text-lg font-bold text-[#5367C9]">
                    {formatVnd(totals.total)}
                  </div>
                </dd>
              </div>
            </dl>

            {totals.totalDiscount > 0 && (
              <p className="mt-2 rounded-lg bg-[#F0FDF4] px-3 py-2 text-xs font-medium text-[#16A34A]">
                Bạn tiết kiệm được {formatVnd(totals.totalDiscount)}
              </p>
            )}

            <p className="mt-3 text-xs text-[#8A93A6]">
              Chỉ các sản phẩm được tích chọn mới được tính vào tổng tiền. Sản
              phẩm không khả dụng không thể chọn.
            </p>

            <button
              type="button"
              onClick={handleCheckout}
              disabled={selectedCount === 0}
              className="mt-5 h-12 w-full rounded-xl bg-[#5367C9] font-semibold text-white transition hover:bg-[#4659B8] disabled:cursor-not-allowed disabled:bg-[#C5CCEB] disabled:hover:bg-[#C5CCEB]"
            >
              Thanh toán{selectedCount > 0 ? ` (${selectedCount})` : ''}
            </button>
            {selectedCount === 0 && (
              <p className="mt-2 text-center text-xs text-[#8A93A6]">
                Hãy tích chọn ít nhất 1 sản phẩm để thanh toán.
              </p>
            )}
            <button
              type="button"
              onClick={() => navigate('/products')}
              className="mt-3 h-12 w-full rounded-xl border border-[#5367C9] font-semibold text-[#5367C9] transition hover:bg-[#F4F6FF]"
            >
              Tiếp tục mua sắm
            </button>
          </aside>
        </div>
      )}
    </div>
  );
};

export default CartPage;