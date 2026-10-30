import { BrowserRouter } from 'react-router-dom';

import PublicRoutes from './PublicRoutes';
import CustomerRoutes from './CustomerRoutes';
import SellerRoutes from './SellerRoutes';


const AppRoutes = () => {
  return (
    <BrowserRouter>
      <PublicRoutes />
      <CustomerRoutes />
      <SellerRoutes />
    </BrowserRouter>
  );
};

export default AppRoutes;