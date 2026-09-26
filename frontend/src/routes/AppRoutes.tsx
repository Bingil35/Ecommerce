import { BrowserRouter } from 'react-router-dom';
import PublicRoutes from './PublicRoutes';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <PublicRoutes />
    </BrowserRouter>
  );
};

export default AppRoutes;
