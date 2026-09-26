import { Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import PublicLayout from '../layouts/PublicLayout';

import HomePage from '../pages/public/HomePage';
import SearchPage from '../pages/public/SearchPage';
import ProductDetailPage from '../pages/public/ProductDetailPage';

import LoginPage from '../pages/guest/LoginPage';
import RegisterPage from '../pages/guest/RegisterPage';
import ShopDetailPage from '../pages/public/ShopDetailPage';
const PublicRoutes = () => {
  return (
    <Routes>

      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchPage />} />
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
