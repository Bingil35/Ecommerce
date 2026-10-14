import { BrowserRouter } from 'react-router-dom';

import PublicRoutes from './PublicRoutes';
import CustomerRoutes from './CustomerRoutes';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <PublicRoutes />
      <CustomerRoutes />
    </BrowserRouter>
  );
};

export default AppRoutes;