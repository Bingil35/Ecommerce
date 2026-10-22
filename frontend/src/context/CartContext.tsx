import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import axios from 'axios';

import { useAuth } from './AuthContext';
import {
  cartApi,
  getCartErrorMessage,
  toCartShops,
} from '../services/cart/cartApi';
import type {
  AddToCartPayload,
  CartContextValue,
  CartDto,
} from '../services/cart/cartTypes';

const CartContext = createContext<CartContextValue | undefined>(undefined);

const EMPTY_CART: CartDto = {
  cartId: null,
  shops: [],
  totalQuantity: 0,
  subtotal: 0,
};

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();

  const [cart, setCart] = useState<CartDto>(EMPTY_CART);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Bỏ qua kết quả của request cũ (ví dụ đã đăng xuất/đổi tài khoản)
  const requestIdRef = useRef(0);

  const refresh = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    setIsLoading(true);
    setError(null);

    try {
      const data = await cartApi.get();
      if (requestId === requestIdRef.current) setCart(data);
    } catch (e) {
      if (requestId !== requestIdRef.current) return;

      // Tài khoản không phải CUSTOMER (shop owner/admin) không có giỏ hàng
      if (axios.isAxiosError(e) && e.response?.status === 403) {
        setCart(EMPTY_CART);
      } else {
        setError(getCartErrorMessage(e));
      }
    } finally {
      if (requestId === requestIdRef.current) setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthLoading) return;

    if (isAuthenticated) {
      void refresh();
    } else {
      requestIdRef.current++;
      setCart(EMPTY_CART);
      setError(null);
      setIsLoading(false);
    }
  }, [isAuthenticated, isAuthLoading, user?.id, refresh]);

  // Mỗi thao tác trả về giỏ hàng mới nhất từ BE -> thay thế state
  const runMutation = async (action: () => Promise<CartDto>) => {
    try {
      setCart(await action());
      return true;
    } catch (e) {
      window.alert(getCartErrorMessage(e));
      return false;
    }
  };

  const addToCart = (payload: AddToCartPayload) =>
    runMutation(() => cartApi.addItem(payload));

  const updateQuantity = (itemId: string, quantity: number) =>
    quantity < 1
      ? removeFromCart(itemId)
      : runMutation(() => cartApi.updateItem(itemId, quantity));

  const removeFromCart = (itemId: string) =>
    runMutation(() => cartApi.removeItem(itemId));

  const shops = useMemo(() => toCartShops(cart), [cart]);
  const items = useMemo(() => shops.flatMap((shop) => shop.items), [shops]);

  const value: CartContextValue = {
    shops,
    items,
    itemCount: Number(cart.totalQuantity ?? 0),
    subtotal: Number(cart.subtotal ?? 0),
    isLoading,
    error,
    refresh,
    addToCart,
    updateQuantity,
    removeFromCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within CartProvider');
  }
  return context;
};