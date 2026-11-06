import { BrowserRouter } from 'react-router-dom';

import PublicRoutes from './PublicRoutes';
import CustomerRoutes from './CustomerRoutes';
import SellerRoutes from './SellerRoutes';
import ShopOwnerRoutes from './ShopOwnerRoutes';
import AdminRoutes from './AdminRoutes';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <PublicRoutes />
      <CustomerRoutes />
      <SellerRoutes />
      <ShopOwnerRoutes />
      <AdminRoutes />
    </BrowserRouter>
  );
};

export default AppRoutes;
