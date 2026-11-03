import { Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import PublicLayout from '../layouts/PublicLayout';

import HomePage from '../pages/public/HomePage';
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
const PublicRoutes = () => {
  return (
    <Routes>

      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/products" element={<ProductListingPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/account" element={<AccountInfoPage />} />
        <Route path="/account/addresses" element={<AddressBookPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/orders" element={<OrderHistoryPage />} />
        <Route path="/orders/:orderId" element={<OrderDetailPage />} />
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
          element={<LoginPage />}
        />
        <Route
          path="/register"
          element={<RegisterPage />}
        />
      </Route>

    </Routes>
  );
};

export default PublicRoutes;