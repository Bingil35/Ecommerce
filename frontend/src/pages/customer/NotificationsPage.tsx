import { useEffect, useState } from 'react';
import { Bell, CheckCheck, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../../services/api/axiosClient';

type Notice = { id: string; title: string; content: string; referenceType: 'ORDER'|'PROMOTION'|'SYSTEM'|'CHAT'|'REVIEW'; referenceId: string|null; read: boolean; createdAt: string };
type Envelope<T> = { data: T };

export default function NotificationsPage() {
  const [items, setItems] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const load = async () => {
    try { const { data } = await axiosClient.get<Envelope<Notice[]>>('/notifications'); setItems(data.data ?? []); setError(''); }
    catch { setError('Không tải được thông báo. Vui lòng thử lại.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  const markAll = async () => { await axiosClient.patch('/notifications/read-all'); await load(); };

  return <section className="rounded-2xl border border-[#E6EAF3] bg-white p-5 shadow-sm sm:p-7">
    <header className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-xl font-bold text-[#252B40]">Thông báo</h1><p className="mt-1 text-sm text-slate-500">Cập nhật đơn hàng và ưu đãi dành cho bạn.</p></div>
      {items.some(item => !item.read) && <button onClick={() => void markAll()} className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold text-[var(--kala-primary)] hover:bg-[var(--kala-soft)]"><CheckCheck size={16}/> Đọc tất cả</button>}
    </header>
    {loading ? <p className="py-12 text-center text-sm text-slate-500">Đang tải thông báo...</p> : error ? <p className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p> : items.length ? <div className="mt-5 divide-y divide-slate-100 rounded-xl border border-slate-100">
      {items.map(item => <button key={item.id} onClick={() => navigate(`/account/notifications/${item.id}`)} className={`flex w-full items-start gap-3 p-4 text-left transition hover:bg-slate-50 ${item.read ? 'bg-white' : 'bg-[var(--kala-soft)]/50'}`}>
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><Bell size={18}/></span>
        <span className="min-w-0 flex-1"><b className="block text-sm text-[#303950]">{item.title}</b><span className="mt-1 block line-clamp-2 text-sm text-slate-600">{item.content}</span><small className="mt-2 block text-xs text-slate-400">{new Date(item.createdAt).toLocaleString('vi-VN')}</small></span>
        {!item.read && <i className="mt-2 h-2 w-2 shrink-0 rounded-full bg-[var(--kala-primary)]"/>}<ChevronRight size={17} className="mt-2 shrink-0 text-slate-400"/>
      </button>)}
    </div> : <div className="py-14 text-center"><Bell size={28} className="mx-auto text-slate-300"/><p className="mt-3 text-sm font-semibold text-slate-700">Chưa có thông báo</p></div>}
  </section>;
}
