import { Route, Routes } from 'react-router-dom';

import AuthLayout from '../layouts/AuthLayout';
import ShopOwnerLayout from '../layouts/ShopOwnerLayout';

import SellerLoginPage from '../pages/seller/SellerLoginPage';
import SellerRegisterShopPage from '../pages/seller/SellerRegisterShopPage';
import SellerDashboardPage from '../pages/seller/SellerDashboardPage';
import SellerProductsPage from '../pages/seller/SellerProductsPage';
import SellerProductDetailPage from '../pages/seller/SellerProductDetailPage';
import SellerCreateProductPage from '../pages/seller/SellerCreateProductPage';
import SellerInventoryPage from '../pages/seller/SellerInventoryPage';
import SellerInventoryDetailPage from '../pages/seller/SellerInventoryDetailPage';
import SellerPromotionsPage from '../pages/seller/SellerPromotionsPage';
import SellerShopInfoPage from '../pages/seller/SellerShopInfoPage';
import ChatPage from '../pages/chat/ChatPage';
import SettingsPage from '../pages/settings/SettingsPage';

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
          <Route path="/seller/products/:productId" element={<SellerProductDetailPage />} />
          <Route path="/seller/products/new" element={<SellerCreateProductPage />} />
          <Route path="/seller/inventory" element={<SellerInventoryPage />} />
          <Route path="/seller/inventory/:productId" element={<SellerInventoryDetailPage />} />
          <Route path="/seller/promotions" element={<SellerPromotionsPage />} />
          <Route path="/seller/shop" element={<SellerShopInfoPage />} />
          <Route path="/seller/chat" element={<ChatPage role="shop-owner" />} />
          <Route path="/seller/settings" element={<SettingsPage audience="shop-owner" />} />
        </Route>
      </Route>
    </Routes>
  );
};

export default SellerRoutes;
