import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, CreditCard, MapPin, PackageCheck, ShieldCheck, Ticket, ShoppingBag } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import CheckoutVoucherBox from '../../components/customer/checkout/CheckoutVoucherBox';
import { checkoutApi, formatMoney, getApiError } from '../../services/checkout/checkoutApi';
import type { CheckoutPreview, CustomerAddress, Coupon } from '../../services/checkout/checkoutTypes';

type CheckoutState = { itemIds?: string[]; platformCouponCode?: string | null; shopCouponCodes?: Record<string, string> };
const isPositive = (value: number | string | null | undefined) => Number(value ?? 0) > 0;
const CheckoutPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { shops: cartShops, isLoading: cartLoading } = useCart();
  const state = (location.state ?? {}) as CheckoutState;
  const itemIds = useMemo(() => state.itemIds ?? [], [state.itemIds]);
  const [addresses, setAddresses] = useState<CustomerAddress[]>([]);
  const [addressId, setAddressId] = useState('');
  const [platformCoupons, setPlatformCoupons] = useState<Coupon[]>([]);
  const [shopCoupons, setShopCoupons] = useState<Coupon[]>([]);
  const [platformCouponCode, setPlatformCouponCode] = useState(state.platformCouponCode ?? '');
  const [shopCouponCodes, setShopCouponCodes] = useState<Record<string, string>>(state.shopCouponCodes ?? {});
  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [previewing, setPreviewing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'COD'>('COD');

  useEffect(() => {
    if (!isAuthenticated || itemIds.length === 0) return;
    let alive = true;
    Promise.all([checkoutApi.getAddresses(), checkoutApi.getCoupons('PLATFORM'), checkoutApi.getCoupons('SHOP')])
      .then(([a, p, s]) => {
        if (!alive) return;
        setAddresses(a); setAddressId(a.find(x => x.isDefault)?.id ?? a[0]?.id ?? '');
        setPlatformCoupons(p); setShopCoupons(s);
      })
      .catch(e => { if (alive) setError(getApiError(e, 'Không thể tải dữ liệu thanh toán.')); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [isAuthenticated, itemIds]);

  const request = (): Parameters<typeof checkoutApi.preview>[0] => ({
    addressId, itemIds, platformCouponCode: platformCouponCode || null, shopCouponCodes,
  });

  useEffect(() => {
    if (!addressId || itemIds.length === 0) return;
    let alive = true;
    setPreviewing(true);
    checkoutApi.preview(request())
      .then(data => { if (alive) { setPreview(data); setError(''); } })
      .catch(e => { if (alive) { setPreview(null); setError(getApiError(e, 'Không thể tính toán đơn hàng.')); } })
      .finally(() => { if (alive) setPreviewing(false); });
    return () => { alive = false; };
  }, [addressId, itemIds, platformCouponCode, shopCouponCodes]);

  if (authLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: '/checkout', notice: 'Đăng nhập để thanh toán.' }} />;
  if (itemIds.length === 0) return <Navigate to="/cart" replace />;
  const selectedAddress = addresses.find(a => a.id === addressId);
  const selectedItemsByShop = useMemo(() => cartShops
    .map(shop => ({ ...shop, items: shop.items.filter(item => itemIds.includes(item.id)) }))
    .filter(shop => shop.items.length > 0), [cartShops, itemIds]);
  const shopSubtotal = (shopId: string) => selectedItemsByShop
    .find(shop => shop.shopId === shopId)?.items
    .reduce((sum, item) => sum + item.price * item.quantity, 0) ?? 0;
  const platformCouponSubtotal = Number(preview?.subtotal ?? selectedItemsByShop.reduce(
    (sum, shop) => sum + shop.items.reduce((shopSum, item) => shopSum + item.price * item.quantity, 0), 0
  ));
  const couponEligible = (coupon: Coupon, subtotal: number) =>
    subtotal > 0 && subtotal >= Number(coupon.minimumOrder ?? 0);

  useEffect(() => {
    if (cartLoading || loading) return;
    setShopCouponCodes(current => Object.fromEntries(Object.entries(current).filter(([shopId, code]) => {
      const coupon = shopCoupons.find(item => item.shopId === shopId && item.code === code);
      return Boolean(coupon && couponEligible(coupon, shopSubtotal(shopId)));
    })));
    if (platformCouponCode) {
      const coupon = platformCoupons.find(item => item.code === platformCouponCode);
      if (!coupon || !couponEligible(coupon, platformCouponSubtotal)) setPlatformCouponCode('');
    }
  }, [cartLoading, loading, shopCoupons, platformCoupons, selectedItemsByShop, preview?.shopDiscount, preview?.subtotal, platformCouponCode]);

  const placeOrder = async () => {
    if (!addressId || !preview) { setError('Vui lòng chọn địa chỉ và chờ hệ thống tính lại đơn hàng.'); return; }
    setSubmitting(true); setError('');
    try {
      const result = await checkoutApi.placeOrder({ ...request(), paymentMethod });
      navigate(`/orders/${result.id}`, { replace: true, state: { justPlaced: true } });
    } catch (e) { setError(getApiError(e, 'Đặt hàng thất bại. Vui lòng kiểm tra lại giỏ hàng.')); }
    finally { setSubmitting(false); }
  };

  return <main className="min-h-[70vh] bg-[#F7F9FD] px-3 py-7 sm:px-5 sm:py-10"><div className="mx-auto max-w-[1100px]">
    <div className="mb-6"><Link to="/cart" className="inline-flex items-center gap-2 text-sm font-medium text-[#6573C8] hover:underline"><ArrowLeft size={16}/>Quay lại giỏ hàng</Link><h1 className="mt-3 text-2xl font-bold text-[#202642] sm:text-3xl">Thanh toán</h1><p className="mt-1 text-sm text-[#7A8498]">Kiểm tra địa chỉ, coupon và tổng tiền trước khi đặt hàng.</p></div>
    {error && <div role="alert" className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>}
    {loading ? <div className="rounded-2xl bg-white p-12 text-center text-sm text-[#697386]">Đang tải thông tin thanh toán...</div> : <div className="grid items-start gap-6 lg:grid-cols-[1fr_360px]">
      <div className="space-y-5">
        <section className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2"><ShoppingBag size={19} className="text-[#596BD0]"/><h2 className="font-bold text-[#252B40]">Sản phẩm thanh toán ({itemIds.length})</h2></div>
          {cartLoading ? <p className="text-sm text-[#7B8497]">Đang tải danh sách sản phẩm...</p> : selectedItemsByShop.length ? <div className="space-y-4">{selectedItemsByShop.map(shop => <div key={shop.id}>
            <h3 className="mb-2 text-sm font-semibold text-[#364057]">{shop.shopName}</h3>
            <div className="divide-y divide-[#EEF1F6] rounded-xl border border-[#EEF1F6]">{shop.items.map(item => {
              const priceLine = preview?.shops.find(s => s.shopId === shop.shopId)?.items.find(line => line.cartItemId === item.id);
              const unitPrice = priceLine ? Number(priceLine.price) : item.price;
              const comparePrice = priceLine?.comparePrice != null ? Number(priceLine.comparePrice) : item.comparePrice;
              const shopDiscount = Number(priceLine?.shopCouponDiscount ?? 0);
              const platformDiscount = Number(priceLine?.platformCouponDiscount ?? 0);
              const lineSubtotal = Number(priceLine?.subtotal ?? unitPrice * item.quantity);
              const payable = Number(priceLine?.totalAmount ?? lineSubtotal - shopDiscount - platformDiscount);
              return <div key={item.id} className="flex gap-3 p-3">
              <img src={item.imageUrl || '/placeholder.png'} alt={item.productName} className="h-16 w-16 shrink-0 rounded-lg bg-[#F4F6FA] object-cover" />
              <div className="min-w-0 flex-1"><p className="line-clamp-2 text-sm font-medium text-[#273047]">{item.productName}</p>
                {(item.variantName || item.selectedOptions?.length) && <p className="mt-1 text-xs text-[#7B8497]">{item.variantName || item.selectedOptions?.map(option => `${option.optionName}: ${option.value}`).join(' · ')}</p>}
                <p className="mt-1 text-xs text-[#7B8497]">Số lượng: {item.quantity}</p>
                <p className="mt-1 text-xs"><span className="text-[#98A1B2]">Đơn giá: </span>{comparePrice != null && comparePrice > unitPrice && <span className="mr-1 text-[#98A1B2] line-through">{formatMoney(comparePrice)}</span>}<span className="font-medium text-[#35405A]">{formatMoney(unitPrice)}</span></p>
                {isPositive(shopDiscount) && <p className="mt-1 text-xs text-emerald-700">Coupon shop: −{formatMoney(shopDiscount)}</p>}
                {isPositive(platformDiscount) && <p className="mt-1 text-xs text-emerald-700">Coupon toàn sàn: −{formatMoney(platformDiscount)}</p>}
              </div><div className="shrink-0 text-right"><p className="text-xs text-[#7B8497]">{formatMoney(lineSubtotal)}</p><p className="mt-1 text-sm font-semibold text-[#35405A]">{formatMoney(payable)}</p></div>
            </div>})}</div>
            <div className="mt-3"><CheckoutVoucherBox title={`Voucher của ${shop.shopName}`} label="Voucher cửa hàng" coupons={shopCoupons.filter(c => c.shopId === shop.shopId)} selectedCode={shopCouponCodes[shop.shopId] ?? ''} selectedSubtotal={shopSubtotal(shop.shopId)} discount={Number(preview?.shops.find(s => s.shopId === shop.shopId)?.shopCouponDiscount ?? 0)} onChange={code => setShopCouponCodes(prev => { const next={...prev}; if(code) next[shop.shopId]=code; else delete next[shop.shopId]; return next; })} /></div>
          </div>)}</div> : <p className="text-sm text-amber-700">Không tìm thấy sản phẩm trong giỏ hàng. Vui lòng quay lại giỏ để cập nhật.</p>}
        </section>
        <section className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2"><MapPin size={19} className="text-[#596BD0]"/><h2 className="font-bold text-[#252B40]">Địa chỉ nhận hàng</h2></div>
          {addresses.length ? <div className="space-y-3">{addresses.map(a => <label key={a.id} className={`flex cursor-pointer gap-3 rounded-xl border p-4 ${addressId === a.id ? 'border-[#6576D8] bg-[#F5F7FF]' : 'border-[#E8ECF3] hover:border-[#C8D1EA]'}`}><input type="radio" name="address" checked={addressId === a.id} onChange={() => setAddressId(a.id)} className="mt-1 accent-[#596BD0]"/><span className="min-w-0 flex-1"><span className="flex flex-wrap items-center gap-2 text-sm font-semibold text-[#273047]">{a.recipientName}<span className="font-normal text-[#788195]">{a.recipientPhone}</span>{a.isDefault && <span className="rounded-full bg-[#EEF2FF] px-2 py-0.5 text-[10px] font-semibold text-[#596BD0]">Mặc định</span>}</span><span className="mt-1 block text-sm text-[#697386]">{a.addressDetail}, {a.ward}, {a.provinceCity}</span></span></label>)}</div> : <div className="rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Bạn chưa có địa chỉ nhận hàng. <Link to="/account/addresses" state={{ checkoutState: location.state }} className="font-semibold underline">Thêm địa chỉ</Link>.</div>}
        </section>
        <section className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2"><Ticket size={19} className="text-[#E88B42]"/><h2 className="font-bold text-[#252B40]">Coupon ưu đãi</h2></div>
          <CheckoutVoucherBox title="Voucher toàn sàn" label="Voucher toàn sàn" coupons={platformCoupons} selectedCode={platformCouponCode} selectedSubtotal={platformCouponSubtotal} discount={Number(preview?.platformDiscount ?? 0)} onChange={setPlatformCouponCode} />
          <p className="mt-3 text-xs text-[#7B8497]">Tổng tiền sẽ tự cập nhật khi bạn chọn hoặc bỏ voucher.</p>
        </section>
        <section className="rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2"><CreditCard size={19} className="text-[#596BD0]"/><h2 className="font-bold text-[#252B40]">Phương thức thanh toán</h2></div>
          <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-[#6576D8] bg-[#F5F7FF] p-4"><input type="radio" checked={paymentMethod === 'COD'} onChange={() => setPaymentMethod('COD')} className="accent-[#596BD0]"/><span className="flex-1"><span className="block text-sm font-semibold text-[#273047]">Thanh toán khi nhận hàng (COD)</span><span className="mt-1 block text-xs text-[#778197]">Thanh toán trực tiếp cho đơn vị giao hàng khi nhận đủ sản phẩm.</span></span><PackageCheck size={22} className="text-[#596BD0]"/></label>
          <div className="mt-3 flex flex-wrap gap-2"><span className="rounded-lg border border-dashed border-[#E1E6EF] px-3 py-2 text-xs text-[#A0A7B5]">VNPAY · Chưa tích hợp cổng thanh toán</span><span className="rounded-lg border border-dashed border-[#E1E6EF] px-3 py-2 text-xs text-[#A0A7B5]">MOMO · Chưa tích hợp cổng thanh toán</span></div>
        </section>
      </div>
      <aside className="sticky top-24 rounded-2xl border border-[#E7EBF3] bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2"><ShieldCheck size={19} className="text-[#596BD0]"/><h2 className="font-bold text-[#252B40]">Tóm tắt đơn hàng</h2></div>
        {preview ? <>
          <div className="mt-4 space-y-3">{preview.shops.map(shop => <div key={shop.shopId} className="border-b border-[#EEF1F6] pb-3"><div className="flex justify-between gap-3 text-sm"><span className="font-semibold text-[#364057]">{shop.shopName}</span><span className="text-[#697386]">{shop.itemCount} sản phẩm</span></div>
            <div className="mt-2 space-y-2">{shop.items.map(line => {
              const cartItem = cartShops.flatMap(group => group.items).find(item => item.id === line.cartItemId);
              return <div key={line.cartItemId} className="rounded-lg bg-[#F8FAFD] px-2.5 py-2 text-xs">
                <div className="flex justify-between gap-2"><span className="min-w-0 truncate text-[#697386]">{cartItem?.productName ?? 'Sản phẩm'} × {line.quantity}</span><span className="shrink-0 font-medium text-[#35405A]">{formatMoney(line.totalAmount)}</span></div>
                <div className="mt-1 flex justify-between text-[#7B8497]"><span>Giá bán {line.comparePrice != null && Number(line.comparePrice) > Number(line.price) && <span className="ml-1 line-through">{formatMoney(line.comparePrice)}</span>}</span><span>{formatMoney(line.price)} / sản phẩm</span></div>
                {isPositive(line.shopCouponDiscount) && <div className="mt-1 flex justify-between text-emerald-700"><span>Coupon shop</span><span>−{formatMoney(line.shopCouponDiscount)}</span></div>}
                {isPositive(line.platformCouponDiscount) && <div className="mt-1 flex justify-between text-emerald-700"><span>Coupon toàn sàn</span><span>−{formatMoney(line.platformCouponDiscount)}</span></div>}
              </div>;
            })}</div>
            <div className="mt-2 flex justify-between text-sm text-[#697386]"><span>Tạm tính</span><span>{formatMoney(shop.subtotal)}</span></div>
            {isPositive(shop.shopCouponDiscount) && <div className="mt-1 flex justify-between text-sm text-emerald-700"><span>Coupon shop</span><span>−{formatMoney(shop.shopCouponDiscount)}</span></div>}
            {isPositive(shop.platformCouponDiscount) && <div className="mt-1 flex justify-between text-sm text-emerald-700"><span>Coupon toàn sàn phân bổ</span><span>−{formatMoney(shop.platformCouponDiscount)}</span></div>}
            <div className="mt-1 flex justify-between text-sm font-semibold text-[#35405A]"><span>Thành tiền</span><span>{formatMoney(shop.totalAmount)}</span></div></div>)}</div>
          <dl className="mt-4 space-y-3 text-sm"><div className="flex justify-between text-[#697386]"><dt>Tạm tính</dt><dd>{formatMoney(preview.subtotal)}</dd></div>{isPositive(preview.shopDiscount) && <div className="flex justify-between text-emerald-700"><dt>Giảm giá coupon shop</dt><dd>−{formatMoney(preview.shopDiscount)}</dd></div>}{isPositive(preview.platformDiscount) && <div className="flex justify-between text-emerald-700"><dt>Giảm giá coupon toàn sàn</dt><dd>−{formatMoney(preview.platformDiscount)}</dd></div>}<div className="flex justify-between border-t border-[#E9EDF4] pt-3 text-base font-bold text-[#26304A]"><dt>Tổng thanh toán</dt><dd className="text-[#596BD0]">{formatMoney(preview.totalAmount)}</dd></div></dl>
          {preview.appliedCoupons.length > 0 && <p className="mt-3 rounded-lg bg-[#F1F5FF] px-3 py-2 text-xs text-[#596BD0]">Đã áp dụng: {preview.appliedCoupons.join(', ')}</p>}
        </> : <p className="mt-4 text-sm text-[#8A93A6]">{previewing ? 'Đang tính toán...' : 'Chọn địa chỉ để xem tổng tiền.'}</p>}
        {selectedAddress && <p className="mt-4 rounded-xl bg-[#F8FAFD] p-3 text-xs leading-5 text-[#68738A]"><strong className="text-[#34405B]">Giao đến:</strong> {selectedAddress.recipientName} · {selectedAddress.recipientPhone}<br/>{selectedAddress.addressDetail}, {selectedAddress.ward}, {selectedAddress.provinceCity}</p>}
        <button type="button" onClick={() => void placeOrder()} disabled={submitting || !preview || !selectedAddress || previewing} className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#596BD0] text-sm font-semibold text-white hover:bg-[#4859C2] disabled:cursor-not-allowed disabled:bg-[#C4CBEA]">{submitting ? 'Đang đặt hàng...' : <><CheckCircle2 size={17}/>Đặt hàng</>}</button>
        <p className="mt-3 text-center text-[11px] leading-5 text-[#8A93A6]">Giá và tồn kho sẽ được backend xác minh lại trong giao dịch đặt hàng.</p>
      </aside>
    </div>}
  </div></main>;
};
export default CheckoutPage;
