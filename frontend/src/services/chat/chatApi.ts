import axiosClient from '../api/axiosClient';

type Envelope<T> = { success: boolean; message: string; data: T };
export interface ChatConversation { id: string; customerId: string; shopId: string; otherName: string; otherAvatarUrl: string | null; lastMessage: string | null; lastMessageAt: string | null; unreadCount: number; shopName: string; shopOwnerName: string; shopSlug: string; }
export interface ChatMessage { id: string; conversationId: string; senderId: string; content: string; messageType: 'TEXT' | 'IMAGE' | 'VIDEO' | 'FILE' | 'PRODUCT'; read: boolean; createdAt: string; }

export const chatApi = {
  async customerList() { return (await axiosClient.get<Envelope<ChatConversation[]>>('/chat/customer/conversations')).data.data; },
  async customerStart(shopId: string) { return (await axiosClient.post<Envelope<ChatConversation>>('/chat/customer/conversations', null, { params: { shopId } })).data.data; },
  async ownerList() { return (await axiosClient.get<Envelope<ChatConversation[]>>('/chat/shop-owner/conversations')).data.data; },
  async history(conversationId: string) { return (await axiosClient.get<Envelope<ChatMessage[]>>(`/chat/conversations/${conversationId}/messages`, { params: { size: 200 } })).data.data; },
  async uploadMedia(conversationId: string, file: File) {
    const form = new FormData(); form.append('file', file);
    return (await axiosClient.post<Envelope<{ url: string; messageType: 'IMAGE' | 'VIDEO' }>>(`/chat/conversations/${conversationId}/attachments`, form, { headers: { 'Content-Type': undefined } })).data.data;
  },
};

export function chatWebSocketUrl() {
  const base = new URL(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api', window.location.origin);
  base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:';
  base.pathname = base.pathname.replace(/\/api\/?$/, '').replace(/\/$/, '') + '/ws/chat';
  base.search = '';
  return base.toString();
}
