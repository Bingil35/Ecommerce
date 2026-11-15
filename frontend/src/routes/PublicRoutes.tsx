import { Navigate, Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import PublicLayout from '../layouts/PublicLayout';

import DomainHomePage from '../pages/public/DomainHomePage';
import SearchPage from '../pages/public/SearchPage';
import ProductDetailPage from '../pages/public/ProductDetailPage';
import ProductListingPage from '../pages/public/ProductListingPage';

import LoginPage from '../pages/guest/LoginPage';
import RegisterPage from '../pages/guest/RegisterPage';
import ShopDetailPage from '../pages/public/ShopDetailPage';
import CartPage from '../pages/customer/CartPage';
import WishlistPage from '../pages/customer/WishlistPage';
import AccountInfoPage from '../pages/customer/AccountInfoPage';
import CheckoutPage from '../pages/customer/CheckoutPage';
import OrderHistoryPage from '../pages/customer/OrderHistoryPage';
import OrderDetailPage from '../pages/customer/OrderDetailPage';
import AddressBookPage from '../pages/customer/AddressBookPage';
import ChatPage from '../pages/chat/ChatPage';
import SettingsPage from '../pages/settings/SettingsPage';
import CustomerAccountLayout from '../layouts/CustomerAccountLayout';
import FollowedShopsPage from '../pages/customer/FollowedShopsPage';
import RecentlyViewedPage from '../pages/customer/RecentlyViewedPage';
import NotificationsPage from '../pages/customer/NotificationsPage';
import NotificationDetailPage from '../pages/customer/NotificationDetailPage';
import VoucherDetailPage from '../pages/customer/VoucherDetailPage';
const PublicRoutes = () => {
  return (
    <Routes>

      <Route element={<PublicLayout />}>
        <Route path="/" element={<DomainHomePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/products" element={<ProductListingPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/account" element={<CustomerAccountLayout />}>
          <Route index element={<AccountInfoPage />} />
          <Route path="addresses" element={<AddressBookPage />} />
          <Route path="settings" element={<SettingsPage audience="customer" />} />
          <Route path="followed-shops" element={<FollowedShopsPage />} />
          <Route path="recently-viewed" element={<RecentlyViewedPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="notifications/:notificationId" element={<NotificationDetailPage />} />
          <Route path="vouchers/:couponId" element={<VoucherDetailPage />} />
        </Route>
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/orders" element={<OrderHistoryPage />} />
        <Route path="/orders/sub-orders/:orderId" element={<OrderDetailPage />} />
        <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route
          path="/product/:slug"
          element={<ProductDetailPage />}
        />
        <Route
          path="/shop/:slug"
          element={<ShopDetailPage />}
        />
      </Route>

      <Route element={<AuthLayout />}>
        <Route
          path="/login"
          element={<DomainLoginPage />}
        />
        <Route
          path="/register"
          element={<RegisterPage />}
        />
      </Route>

    </Routes>
  );
};

function DomainLoginPage() {
  const host = window.location.hostname.toLowerCase();
  if (host.startsWith('admin.')) return <Navigate to="/admin/login" replace />;
  if (host.startsWith('shop.')) return <Navigate to="/seller/login" replace />;
  return <LoginPage />;
}

export default PublicRoutes;
