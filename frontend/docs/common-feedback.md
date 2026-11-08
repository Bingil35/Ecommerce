# Common Notification & Dialog

Một `FeedbackProvider` ở `src/main.tsx`, bên ngoài Auth/Cart/Wishlist để Toast tồn tại khi chuyển trang. Không thay đổi Backend hoặc database, không thêm dependency.

## Sử dụng

```tsx
import { useToast, useDialog } from '../context/FeedbackContext';
import { LoadingButton } from '../components/common/LoadingState';

const toast = useToast();
const { confirm, prompt } = useDialog();

toast.success('Đã lưu thay đổi.');
toast.error('Không thể lưu thay đổi.');
toast.warning('Vui lòng kiểm tra dữ liệu.');
toast.info('Thông tin đã được cập nhật.');

await confirm({
  title: 'Xóa sản phẩm?',
  message: 'Hành động này không thể hoàn tác.',
  confirmText: 'Xóa sản phẩm',
  cancelText: 'Hủy',
  variant: 'danger',
  loadingText: 'Đang xóa sản phẩm...',
  successMessage: 'Đã xóa sản phẩm.',
  onConfirm: async () => {
    await deleteProduct(productId);
  },
});

await prompt({
  title: 'Đổi tên thuộc tính',
  message: 'Nhập tên thuộc tính mới.',
  confirmText: 'Lưu thay đổi',
  variant: 'info',
  input: { defaultValue: name, required: true, maxLength: 100 },
  successMessage: 'Đã đổi tên thuộc tính.',
  onConfirm: async value => { await updateName(value.trim()); },
});

<LoadingButton loading={saving} loadingText="Đang lưu..." onClick={save}>
  Lưu thay đổi
</LoadingButton>
```

Đặt API trong `onConfirm` để dialog quản lý loading, khóa bấm lặp, giữ mở khi lỗi và hiển thị lỗi ngay trong dialog. Để Promise reject khi API thất bại: Provider sẽ hiện Error Toast, không cần gọi Toast thêm ở tầng service. Callback có thể trả `false` nếu luồng hiện tại đã tự xử lý lỗi; dialog sẽ giữ mở.

`confirm()` không có callback cũng trả `Promise<boolean>`; `prompt()` trả `Promise<string | null>`. Nếu gọi API sau khi await xác nhận, trang phải tự quản lý loading/lỗi vì dialog đã đóng. Dialog chưa chạy sẽ tự hủy khi trang gọi nó unmount; API đã bắt đầu được chờ hoàn tất trước khi đóng. Escape/nút đóng/Hủy bị khóa trong khi xử lý. Native `<dialog>.showModal()` đảm bảo focus trong modal, nền inert và khôi phục focus khi đóng.

Toast có bốn loại, góc trên bên phải, tối đa bốn thông báo, thời gian 3–5 giây (mặc định 4 giây), nút đóng, animation Framer Motion, hạn chế thông báo trùng trong 4 giây, role status/alert. Timer và listener được dọn khi unmount. LoadingButton giữ chỗ cả hai trạng thái và khóa click async lặp. CSS dùng màu Kala, có hỗ trợ `.dark` / `[data-theme="dark"]`; hệ thống hiện tại chỉ có các bảng màu Light Mode.

Không thay thế validation đang hiển thị trong form. Một nơi chịu trách nhiệm Toast cho mỗi API: Cart/Wishlist mutations do Context thông báo; các trang không Toast lại kết quả đó.

## Components và hạ tầng mới

- `src/components/common/ConfirmDialog.tsx`
- `src/components/common/ToastNotice.tsx`
- `src/components/common/LoadingState.tsx` (LoadingState, LoadingButton)
- `src/components/common/feedback.css`
- `src/context/FeedbackContext.tsx` (FeedbackProvider, useToast, useDialog, getFeedbackError)

## File tích hợp

- `src/main.tsx`
- `src/context/AuthContext.tsx`
- `src/context/CartContext.tsx`
- `src/context/WishlistContext.tsx`
- `src/components/customer/cart/CartItem.tsx`
- `src/components/customer/cart/ShopVoucherBox.tsx`
- `src/components/customer/common/ToastNotice.tsx` (re-export component chung)
- `src/components/customer/wishlist/WishlistStates.tsx` (adapter cho dialog chung)
- `src/pages/admin/AdminPage.tsx`
- `src/pages/admin/AdminBrandsPage.tsx`
- `src/pages/admin/AdminCouponAbusePage.tsx`
- `src/pages/customer/AccountInfoPage.tsx`
- `src/pages/customer/AddressBookPage.tsx`
- `src/pages/customer/CheckoutPage.tsx`
- `src/pages/customer/OrderDetailPage.tsx`
- `src/pages/customer/WishlistPage.tsx`
- `src/pages/guest/LoginPage.tsx`
- `src/pages/guest/RegisterPage.tsx`
- `src/pages/public/ProductDetailPage.tsx`
- `src/pages/seller/SellerCreateProductPage.tsx`
- `src/pages/seller/SellerEditProductPage.tsx`
- `src/pages/seller/SellerInventoryDetailPage.tsx`
- `src/pages/seller/SellerLoginPage.tsx`
- `src/pages/seller/SellerProductDetailPage.tsx`
- `src/pages/seller/SellerProductsPage.tsx`
- `src/pages/seller/SellerPromotionsPage.tsx`
- `src/pages/seller/SellerRegisterShopPage.tsx`
- `src/pages/seller/SellerShopInfoPage.tsx`
- `src/pages/shop-owner/ShopOwnerOrdersPage.tsx`
- `src/pages/shop-owner/ShopOwnerOrderStatusPage.tsx`

## Các luồng đã tích hợp

Shop owner: tạo/sửa/xóa sản phẩm, ẩn/hiện, sao chép, nhập CSV, chỉnh thuộc tính/giá trị/biến thể, thêm/xóa media và lưu ảnh, nhập kho, lưu shop/upload logo/ảnh bìa, đăng nhập/đăng ký shop, lưu khuyến mãi, cập nhật/xác nhận đơn hàng.

Customer: đăng nhập/đăng ký/đăng xuất, thêm/xóa/cập nhật giỏ hàng, yêu thích/bỏ yêu thích/xóa tất cả, lưu hồ sơ/đổi mật khẩu, lưu/xóa/đặt địa chỉ mặc định, đặt hàng/hủy đơn/xác nhận nhận hàng/COD/gửi đánh giá.

Admin: tạo/sửa/xóa dữ liệu và cập nhật trạng thái trên AdminPage, quản lý thương hiệu và xác nhận gỡ hạn chế voucher.

Không còn lời gọi hộp thoại trình duyệt mặc định trong `frontend/src`. Những lời gọi `confirm()`/`prompt()` còn trong source đều đến từ `useDialog`.

## Kiểm tra

Chỉ chạy TypeScript/build và rà soát source, không viết/cài/chạy Unit, Integration hoặc E2E test. Chưa xác nhận giao diện, tương tác bàn phím và API thực trên trình duyệt.
