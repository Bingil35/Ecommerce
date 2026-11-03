import { Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import ShopOwnerLayout from '../layouts/ShopOwnerLayout';

import SellerLoginPage from '../pages/seller/SellerLoginPage';
import SellerRegisterShopPage from '../pages/seller/SellerRegisterShopPage';
import SellerDashboardPage from '../pages/seller/SellerDashboardPage';
import SellerProductsPage from '../pages/seller/SellerProductsPage';
import SellerCreateProductPage from '../pages/seller/SellerCreateProductPage';

import BecomeSellerGuard from './BecomeSellerGuard';
import RequireShopOwner from './RequireShopOwner';

const SellerRoutes = () => {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/seller/login" element={<SellerLoginPage />} />
        <Route
          path="/seller/register"
          element={
            <BecomeSellerGuard>
              <SellerRegisterShopPage />
            </BecomeSellerGuard>
          }
        />
      </Route>

      <Route element={<RequireShopOwner />}>
        <Route element={<ShopOwnerLayout />}>
          <Route path="/seller" element={<SellerDashboardPage />} />
          <Route path="/seller/products" element={<SellerProductsPage />} />
          <Route path="/seller/products/new" element={<SellerCreateProductPage />} />
        </Route>
      </Route>
    </Routes>
  );
};

export default SellerRoutes;
