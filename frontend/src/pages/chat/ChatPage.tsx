import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowUpRight, Image as ImageIcon, MessageCircle, Paperclip, Send, Store, UserRound } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { authStorage } from '../../services/auth/authStorage';
import { chatApi, chatWebSocketUrl, type ChatConversation, type ChatMessage } from '../../services/chat/chatApi';
import { getApiError } from '../../services/checkout/checkoutApi';
import { mediaUrl } from '../../services/api/mediaUrl';

type Role = 'customer' | 'shop-owner';
type EventFrame = { type: string; message?: ChatMessage | string; userId?: string; conversationId?: string };

export default function ChatPage({ role = 'customer' }: { role?: Role }) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const shopId = new URLSearchParams(location.search).get('shopId');
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [content, setContent] = useState('');
  const [socketState, setSocketState] = useState<'connecting' | 'connected' | 'offline'>('offline');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [uploadingMedia, setUploadingMedia] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const mediaInputRef = useRef<HTMLInputElement>(null);
  const selected = useMemo(() => conversations.find(item => item.id === selectedId) || null, [conversations, selectedId]);

  const loadConversations = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoading(true); setError('');
    try {
      if (role === 'customer' && shopId) {
        const started = await chatApi.customerStart(shopId);
        const list = await chatApi.customerList();
        setConversations(list); setSelectedId(started.id);
      } else {
        const list = role === 'customer' ? await chatApi.customerList() : await chatApi.ownerList();
        setConversations(list); setSelectedId(current => current && list.some(item => item.id === current) ? current : list[0]?.id || null);
      }
    } catch (e) { setError(getApiError(e)); }
    finally { setLoading(false); }
  }, [isAuthenticated, role, shopId]);

  useEffect(() => { if (!isLoading && !isAuthenticated) navigate('/login', { replace: true, state: { from: `${location.pathname}${location.search}`, notice: 'Đăng nhập để nhắn tin với cửa hàng.' } }); }, [isLoading, isAuthenticated, navigate, location.pathname, location.search]);
  useEffect(() => { if (isAuthenticated) void loadConversations(); }, [isAuthenticated, loadConversations]);
  useEffect(() => {
    if (!isAuthenticated || role !== 'shop-owner') return;
    const refresh = () => { void chatApi.ownerList().then(list => { setConversations(list); setSelectedId(current => current || list[0]?.id || null); }).catch(() => {}); };
    const interval = window.setInterval(refresh, 12000);
    window.addEventListener('focus', refresh);
    return () => { window.clearInterval(interval); window.removeEventListener('focus', refresh); };
  }, [isAuthenticated, role]);

  useEffect(() => {
    if (!selectedId || !isAuthenticated) { setMessages([]); return; }
    let alive = true;
    setMessages([]); setSocketState('connecting');
    void chatApi.history(selectedId).then(result => { if (alive) setMessages(current => [...new Map([...result, ...current].map(message => [message.id, message])).values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt))); }).catch(e => { if (alive) setError(getApiError(e)); });
    const token = authStorage.getAccessToken();
    if (!token) { setSocketState('offline'); return () => { alive = false; }; }
    const socket = new WebSocket(chatWebSocketUrl()); socketRef.current = socket;
    socket.onopen = () => socket.send(JSON.stringify({ type: 'AUTH', token }));
    socket.onmessage = event => {
      let frame: EventFrame;
      try { frame = JSON.parse(event.data) as EventFrame; } catch { return; }
      if (frame.type === 'AUTH_OK') socket.send(JSON.stringify({ type: 'SUBSCRIBE', conversationId: selectedId }));
      if (frame.type === 'SUBSCRIBED') setSocketState('connected');
      const incoming = frame.message;
      if (frame.type === 'MESSAGE' && incoming && typeof incoming === 'object' && incoming.conversationId === selectedId) {
        setMessages(current => current.some(item => item.id === incoming.id) ? current : [...current, incoming]);
        setConversations(current => current.map(conversation => conversation.id === selectedId ? { ...conversation, lastMessage: incoming.content, lastMessageAt: incoming.createdAt, unreadCount: incoming.senderId === user?.id ? conversation.unreadCount : 0 } : conversation).sort((a, b) => (b.lastMessageAt || '').localeCompare(a.lastMessageAt || '')));
      }
      if (frame.type === 'ERROR') { setError(typeof frame.message === 'string' ? frame.message : 'Không thể kết nối chat.'); setSocketState('offline'); }
    };
    socket.onerror = () => { setSocketState('offline'); setError('Không kết nối được máy chủ chat.'); };
    socket.onclose = () => { if (alive) setSocketState('offline'); };
    return () => { alive = false; socket.close(); if (socketRef.current === socket) socketRef.current = null; };
  }, [selectedId, isAuthenticated]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  const send = (event: React.FormEvent) => {
    event.preventDefault(); const text = content.trim(); const socket = socketRef.current;
    if (!text || !selectedId || !socket || socket.readyState !== WebSocket.OPEN || socketState !== 'connected') return;
    const productLink = /\/product\/[^/?#]+/.test(text);
    let messageType: ChatMessage['messageType'] = productLink ? 'PRODUCT' : 'TEXT';
    if (!productLink && /^https?:\/\//i.test(text)) {
      const extension = new URL(text).pathname.split('.').pop()?.toLowerCase();
      if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension || '')) messageType = 'IMAGE';
      else if (['mp4', 'webm', 'mov'].includes(extension || '')) messageType = 'VIDEO';
      else if (['pdf', 'doc', 'docx', 'zip'].includes(extension || '')) messageType = 'FILE';
    }
    socket.send(JSON.stringify({ type: 'SEND', conversationId: selectedId, content: text, messageType })); setContent('');
  };

  const sendMedia = async (file?: File) => {
    if (!file || !selectedId) return;
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) { setError('Chỉ có thể gửi ảnh hoặc video.'); return; }
    setUploadingMedia(true); setError('');
    try {
      const uploaded = await chatApi.uploadMedia(selectedId, file);
      const socket = socketRef.current;
      if (!socket || socket.readyState !== WebSocket.OPEN || socketState !== 'connected') throw new Error('Chat đang mất kết nối. Hãy kết nối lại rồi gửi media.');
      socket.send(JSON.stringify({ type: 'SEND', conversationId: selectedId, content: uploaded.url, messageType: uploaded.messageType }));
    } catch (e) { setError(e instanceof Error ? e.message : getApiError(e)); }
    finally { setUploadingMedia(false); if (mediaInputRef.current) mediaInputRef.current.value = ''; }
  };

  const pasteMedia = (event: React.ClipboardEvent<HTMLTextAreaElement>) => {
    const file = Array.from(event.clipboardData.files).find(item => item.type.startsWith('image/') || item.type.startsWith('video/'))
      || Array.from(event.clipboardData.items).find(item => item.type.startsWith('image/') || item.type.startsWith('video/'))?.getAsFile() || undefined;
    if (file) { event.preventDefault(); void sendMedia(file); }
  };

  if (isLoading || (!isAuthenticated && !error)) return <main className="grid min-h-[60vh] place-items-center text-sm text-slate-500">Đang tải cuộc trò chuyện...</main>;
  const title = role === 'customer' ? 'Tin nhắn của tôi' : 'Chat với khách hàng';
  return <main className="min-h-[calc(100vh-72px)] bg-[#F7F9FD] px-3 py-5 sm:px-5 sm:py-8"><div className="mx-auto flex h-[min(760px,calc(100vh-120px))] max-w-[1240px] overflow-hidden rounded-3xl border border-[#E6EAF3] bg-white shadow-[0_12px_44px_rgba(42,57,102,.08)]">
    <aside className={`${selectedId ? 'hidden md:flex' : 'flex'} w-full shrink-0 flex-col border-r border-[#EDF0F6] md:w-[340px]`}><header className="flex items-center gap-3 border-b border-[#EDF0F6] px-5 py-5"><button onClick={() => navigate(role === 'shop-owner' ? '/seller' : '/')} aria-label="Quay lại" className="grid h-9 w-9 place-items-center rounded-full text-slate-500 hover:bg-slate-100"><ArrowLeft size={18}/></button><span className="grid h-10 w-10 place-items-center rounded-xl bg-[#EEF3FF] text-[#5367C9]"><MessageCircle size={20}/></span><div><h1 className="text-base font-extrabold text-[#202642]">{title}</h1><p className="mt-0.5 text-xs text-slate-500">Trao đổi trực tiếp, tức thời</p></div></header>
      <div className="flex-1 overflow-y-auto">{error && <div className="m-3 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}{loading ? <div className="p-6 text-center text-sm text-slate-400">Đang tải hội thoại...</div> : conversations.map(item => <button key={item.id} onClick={() => setSelectedId(item.id)} className={`flex w-full items-center gap-3 border-b border-slate-100 px-4 py-4 text-left transition hover:bg-[#F8F9FF] ${selectedId === item.id ? 'bg-[#F3F5FF]' : ''}`}><span className="grid h-11 w-11 shrink-0 place-items-center overflow-hidden rounded-full bg-[#EEF3FF] text-[#5367C9]">{item.otherAvatarUrl ? <img src={mediaUrl(item.otherAvatarUrl)} alt="" className="h-full w-full object-cover"/> : role === 'customer' ? <Store size={19}/> : <UserRound size={19}/>}</span><span className="min-w-0 flex-1"><b className="block truncate text-sm text-[#202642]">{item.otherName || (role === 'customer' ? 'Cửa hàng' : 'Khách hàng')}</b><span className="mt-1 block truncate text-xs text-slate-500">{item.lastMessage || 'Bắt đầu cuộc trò chuyện'}</span></span><span className="flex shrink-0 flex-col items-end gap-1"><small className="text-[10px] text-slate-400">{item.lastMessageAt ? new Date(item.lastMessageAt).toLocaleDateString('vi-VN') : ''}</small>{item.unreadCount > 0 && <b className="grid h-5 min-w-5 place-items-center rounded-full bg-[#5367C9] px-1 text-[10px] text-white">{item.unreadCount}</b>}</span></button>)}{!loading && conversations.length === 0 && <div className="px-7 py-14 text-center"><MessageCircle size={25} className="mx-auto text-slate-300"/><p className="mt-3 text-sm font-semibold text-slate-600">Chưa có hội thoại</p><p className="mt-1 text-xs leading-5 text-slate-400">Mở chat từ trang cửa hàng hoặc sản phẩm để bắt đầu.</p></div>}</div>
    </aside>
    <section className={`${selectedId ? 'flex' : 'hidden md:flex'} min-w-0 flex-1 flex-col bg-[#FBFCFF]`}>
      {selected ? <><header className="flex items-center gap-3 border-b border-[#EDF0F6] bg-white px-4 py-4 sm:px-6"><button className="grid h-9 w-9 place-items-center rounded-full text-slate-500 hover:bg-slate-100 md:hidden" onClick={() => setSelectedId(null)}><ArrowLeft size={18}/></button><span className="grid h-10 w-10 place-items-center overflow-hidden rounded-full bg-[#EEF3FF] text-[#5367C9]">{selected.otherAvatarUrl ? <img src={mediaUrl(selected.otherAvatarUrl)} alt="" className="h-full w-full object-cover"/> : role === 'customer' ? <Store size={18}/> : <UserRound size={18}/>}</span><div className="min-w-0 flex-1"><b className="block truncate text-sm text-[#202642]">{selected.otherName}</b><span className="mt-0.5 flex items-center gap-1.5 text-xs text-slate-500"><i className={`h-1.5 w-1.5 rounded-full ${socketState === 'connected' ? 'bg-emerald-500' : 'bg-amber-400'}`}/>{socketState === 'connected' ? 'Đang trực tuyến' : socketState === 'connecting' ? 'Đang kết nối...' : 'Mất kết nối'}</span></div></header>
      <div className="flex-1 space-y-3 overflow-y-auto px-4 py-5 sm:px-7">{messages.map(message => { const mine = message.senderId === user?.id; let product: { productId?: string; url: string; name: string; imageUrl: string } | null = null; if (message.messageType === 'PRODUCT') { try { product = JSON.parse(message.content) as { productId?: string; url: string; name: string; imageUrl: string }; } catch { product = null; } } const productPath = product ? new URL(product.url, window.location.origin).pathname : '/products'; return <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}><div className={`max-w-[82%] rounded-2xl px-4 py-2.5 shadow-sm sm:max-w-[70%] ${mine ? 'rounded-br-md bg-[#5367C9] text-white' : 'rounded-bl-md border border-[#E8EBF3] bg-white text-[#26314E]'}`}>{product ? <div className="block w-64 overflow-hidden rounded-xl bg-white text-[#202642]"><Link to={role === 'shop-owner' ? `/seller/products/${product.productId || ''}` : productPath} className="block"><img src={mediaUrl(product.imageUrl)} alt="" className="h-36 w-full object-cover"/><div className="px-3 pt-3"><p className="line-clamp-2 text-sm font-semibold">{product.name}</p><span className="mt-2 inline-flex items-center gap-1 text-xs text-[#5367C9]">{role === 'shop-owner' ? 'Xem trang quản lý sản phẩm' : 'Xem sản phẩm'}<ArrowUpRight size={13}/></span></div></Link><div className="p-3 pt-2"><Link to={`/shop/${selected.shopSlug}`} className="text-[10px] font-semibold uppercase tracking-wide text-[#6574C9] hover:underline">Cửa hàng: {selected.shopName}</Link></div></div> : message.messageType === 'IMAGE' ? <a href={mediaUrl(message.content)} target="_blank" rel="noreferrer"><img src={mediaUrl(message.content)} alt="Ảnh được chia sẻ" className="max-h-72 rounded-lg object-contain"/></a> : message.messageType === 'VIDEO' ? <video src={mediaUrl(message.content)} controls className="max-h-72 max-w-full rounded-lg"/> : message.messageType === 'FILE' ? <a href={mediaUrl(message.content)} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 text-sm underline"><Paperclip size={14}/> Tệp đính kèm</a> : <p className="whitespace-pre-wrap break-words text-sm leading-6">{message.content}</p>}<time className={`mt-1 block text-right text-[10px] ${mine ? 'text-white/70' : 'text-slate-400'}`}>{new Date(message.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</time></div></div>; })}<div ref={bottomRef}/>{messages.length === 0 && <div className="grid h-full place-items-center text-center"><div><span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-[#EEF3FF] text-[#5367C9]"><MessageCircle size={25}/></span><p className="mt-3 text-sm font-semibold text-slate-600">Bắt đầu trò chuyện</p><p className="mt-1 text-xs text-slate-400">Tin nhắn được gửi trực tiếp đến {selected.otherName}.</p></div></div>}</div>
      <form onSubmit={send} className="flex items-end gap-2 border-t border-[#EDF0F6] bg-white p-3 sm:gap-3 sm:px-5 sm:py-4"><input ref={mediaInputRef} type="file" accept="image/jpeg,image/png,image/gif,image/webp,video/mp4,video/webm" className="hidden" onChange={e => void sendMedia(e.target.files?.[0])}/><button type="button" aria-label="Gửi ảnh hoặc video" title="Gửi ảnh hoặc video" disabled={uploadingMedia || socketState !== 'connected'} onClick={() => mediaInputRef.current?.click()} className="grid h-11 w-11 shrink-0 place-items-center rounded-full text-[#5367C9] hover:bg-[#EEF3FF] disabled:text-slate-300"><ImageIcon size={20}/></button><textarea value={content} onChange={e => setContent(e.target.value)} onPaste={pasteMedia} onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); e.currentTarget.form?.requestSubmit(); } }} maxLength={4000} rows={1} placeholder="Nhập tin nhắn..." className="max-h-32 min-h-11 flex-1 resize-y rounded-2xl border border-[#E1E6F0] px-4 py-3 text-sm outline-none focus:border-[#9CAAE4] focus:ring-2 focus:ring-[#EEF1FF]"/><button disabled={!content.trim() || socketState !== 'connected'} aria-label="Gửi tin nhắn" className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-[#5367C9] text-white transition hover:bg-[#4559B7] disabled:cursor-not-allowed disabled:bg-slate-300"><Send size={18}/></button></form></> : <div className="hidden flex-1 flex-col items-center justify-center text-center md:flex"><span className="grid h-16 w-16 place-items-center rounded-3xl bg-[#EEF3FF] text-[#5367C9]"><MessageCircle size={28}/></span><h2 className="mt-4 font-bold text-[#202642]">Chọn một cuộc trò chuyện</h2><p className="mt-1 max-w-xs text-sm leading-6 text-slate-500">Các tin nhắn với khách hàng và cửa hàng sẽ hiển thị tại đây.</p></div>}
    </section>
  </div></main>;
}
