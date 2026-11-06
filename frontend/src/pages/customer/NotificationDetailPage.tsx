import { useEffect, useState } from 'react';
import { ArrowLeft, Bell, ExternalLink } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import axiosClient from '../../services/api/axiosClient';

type Notice = { id: string; title: string; content: string; referenceType: 'ORDER'|'PROMOTION'|'SYSTEM'|'CHAT'|'REVIEW'; referenceId: string|null; read: boolean; createdAt: string };
type Envelope<T> = { data: T };

export default function NotificationDetailPage() {
  const { notificationId } = useParams();
  const navigate = useNavigate();
  const [notice, setNotice] = useState<Notice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  useEffect(() => {
    let active = true;
    void axiosClient.get<Envelope<Notice[]>>('/notifications').then(async ({ data }) => {
      const found = (data.data ?? []).find(item => item.id === notificationId) ?? null;
      if (active) setNotice(found);
      if (found && !found.read) {
        try { await axiosClient.patch(`/notifications/${found.id}/read`); if (active) setNotice({ ...found, read: true }); }
        catch { /* Keep showing the notification if marking it read fails. */ }
      }
    }).catch(() => { if (active) setError(true); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [notificationId]);

  const target = notice?.referenceType === 'ORDER' && notice.referenceId ? `/orders/${notice.referenceId}`
    : notice?.referenceType === 'PROMOTION' && notice.referenceId ? `/account/vouchers/${notice.referenceId}`
    : notice?.referenceType === 'CHAT' ? '/chat' : null;

  return <section className="rounded-2xl border border-[#E6EAF3] bg-white p-5 shadow-sm sm:p-7">
    <Link to="/account/notifications" className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-[var(--kala-primary)]"><ArrowLeft size={16}/> Tất cả thông báo</Link>
    {loading ? <p className="py-12 text-center text-sm text-slate-500">Đang tải...</p> : error ? <p className="py-12 text-center text-sm text-rose-600">Không tải được thông báo. Vui lòng thử lại.</p> : !notice ? <div className="py-12 text-center"><p className="text-sm text-slate-600">Không tìm thấy thông báo này.</p></div> : <article className="mt-6">
      <span className="grid h-12 w-12 place-items-center rounded-2xl bg-[var(--kala-soft)] text-[var(--kala-primary)]"><Bell size={22}/></span>
      <p className="mt-5 text-xs font-semibold uppercase tracking-wide text-[var(--kala-primary)]">{notice.referenceType === 'PROMOTION' ? 'Ưu đãi' : notice.referenceType === 'ORDER' ? 'Đơn hàng' : notice.referenceType === 'CHAT' ? 'Tin nhắn' : 'Cập nhật'}</p>
      <h1 className="mt-2 text-2xl font-bold text-[#252B40]">{notice.title}</h1>
      <p className="mt-2 text-xs text-slate-400">{new Date(notice.createdAt).toLocaleString('vi-VN')}</p>
      <div className="mt-6 whitespace-pre-line rounded-xl bg-slate-50 p-4 text-sm leading-7 text-slate-700">{notice.content}</div>
      {target && <button onClick={() => navigate(target)} className="mt-5 inline-flex items-center gap-2 rounded-xl bg-[var(--kala-primary)] px-4 py-2.5 text-sm font-semibold text-white hover:opacity-90"><ExternalLink size={16}/>{notice.referenceType === 'ORDER' ? 'Xem đơn hàng' : notice.referenceType === 'PROMOTION' ? 'Xem voucher' : 'Mở tin nhắn'}</button>}
    </article>}
  </section>;
}
