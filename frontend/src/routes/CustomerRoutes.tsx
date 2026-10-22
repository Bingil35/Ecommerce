import { Route, Routes } from 'react-router-dom';

import CustomerLayout from '../layouts/CustomerLayout';
import HomePage from '../pages/customer/HomePage';

const CustomerRoutes = () => {
  return (
    <Routes>
      <Route element={<CustomerLayout />}>
        <Route
          path="/customer"
          element={<HomePage />}
        />
      </Route>
    </Routes>
  );
};

export default CustomerRoutes;