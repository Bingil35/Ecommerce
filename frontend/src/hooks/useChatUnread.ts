import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { isShopOwner } from '../services/auth/roles';
import { chatApi } from '../services/chat/chatApi';

type ChatRole = 'customer' | 'shop-owner';

export function useChatUnread(role: ChatRole) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const [hasUnread, setHasUnread] = useState(false);
  const refresh = useCallback(async () => {
    if (!isAuthenticated || (role === 'customer' && isShopOwner(user))) {
      setHasUnread(false);
      return;
    }
    try {
      const conversations = role === 'customer' ? await chatApi.customerList() : await chatApi.ownerList();
      setHasUnread(conversations.some(conversation => conversation.unreadCount > 0));
    } catch {
      // Badge state is best-effort; a temporary chat API failure should not disrupt the header.
    }
  }, [isAuthenticated, role, user]);

  useEffect(() => {
    if (isLoading) return;
    void refresh();
    const interval = window.setInterval(() => void refresh(), 10000);
    const onFocus = () => void refresh();
    const onVisibility = () => { if (document.visibilityState === 'visible') void refresh(); };
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [isLoading, refresh, user?.id]);

  return hasUnread;
}
