import { BrowserRouter } from 'react-router-dom';

import PublicRoutes from './PublicRoutes';
import CustomerRoutes from './CustomerRoutes';
import SellerRoutes from './SellerRoutes';
import ShopOwnerRoutes from './ShopOwnerRoutes';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <PublicRoutes />
      <CustomerRoutes />
      <SellerRoutes />
      <ShopOwnerRoutes />
    </BrowserRouter>
  );
};

export default AppRoutes;
