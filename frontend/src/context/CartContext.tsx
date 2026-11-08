import { useDialog, useToast } from './FeedbackContext';
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
  const toast = useToast();
  const { confirm } = useDialog();
  const mutations = useRef(new Set<string>());
  const [mutationCount,setMutationCount] = useState(0);
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
  const runMutation = async (key: string, action: () => Promise<CartDto>, message: string) => {
    if(mutations.current.has(key)) return false;
    mutations.current.add(key);setMutationCount(count=>count+1);
    try { setCart(await action()); toast.success(message); return true; }
    catch(e) { toast.error(getCartErrorMessage(e)); return false; }
    finally { mutations.current.delete(key);setMutationCount(count=>count-1); }
  };
  const addToCart = (payload: AddToCartPayload) =>
    runMutation(`add:${JSON.stringify(payload)}`, () => cartApi.addItem(payload), 'Đã thêm sản phẩm vào giỏ hàng.');
  const removeFromCart = async (itemId: string) => confirm({
    title:'Xóa sản phẩm khỏi giỏ hàng?', message:'Sản phẩm sẽ được xóa khỏi giỏ hàng của bạn.',
    confirmText:'Xóa sản phẩm',variant:'danger',loadingText:'Đang xóa...',
    onConfirm:()=>runMutation(itemId,()=>cartApi.removeItem(itemId),'Đã xóa sản phẩm khỏi giỏ hàng.'),
  });
  const updateQuantity = (itemId: string, quantity: number) => quantity < 1 ? removeFromCart(itemId) :
    runMutation(itemId, () => cartApi.updateItem(itemId, quantity), 'Đã cập nhật số lượng sản phẩm.');

  const shops = useMemo(() => toCartShops(cart), [cart]);
  const items = useMemo(() => shops.flatMap((shop) => shop.items), [shops]);
  const distinctItemCount = useMemo(() => new Set(items.map(item => item.variantId || item.productId).filter(Boolean)).size, [items]);

  const value: CartContextValue = {
    shops,
    items,
    itemCount: Number(cart.totalQuantity ?? 0),
    distinctItemCount,
    subtotal: Number(cart.subtotal ?? 0),
    isLoading: isLoading || mutationCount > 0,
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
