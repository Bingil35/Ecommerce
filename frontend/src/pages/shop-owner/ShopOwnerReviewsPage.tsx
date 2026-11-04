import { useCallback, useEffect, useState } from 'react';
import { Star } from 'lucide-react';
import { reviewApi, reviewDate, type ProductReview } from '../../services/reviews/reviewApi';
import { getApiError } from '../../services/checkout/checkoutApi';

export default function ShopOwnerReviewsPage() {
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const load = useCallback(async () => { try { setReviews(await reviewApi.ownerList()); setError(''); } catch (e) { setError(getApiError(e)); } }, []);
  useEffect(() => { void load(); }, [load]);
  const send = async (review: ProductReview) => {
    const content = drafts[review.id] ?? review.replies[0]?.content ?? '';
    try { setBusy(review.id); await reviewApi.reply(review.id, content); await load(); } catch (e) { setError(getApiError(e)); } finally { setBusy(null); }
  };
  return <div className="seller-container shop-management"><h1 className="text-2xl font-bold">Đánh giá sản phẩm</h1><p className="mb-6 mt-1 text-sm text-slate-500">Đọc phản hồi của khách và trả lời với tư cách cửa hàng.</p>{error && <div className="mb-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}<div className="space-y-4">{reviews.map(r => <article key={r.id} className="rounded-xl border bg-white p-5"><div className="flex flex-wrap justify-between gap-2"><div><div className="font-semibold">{r.productName}</div><div className="text-sm text-slate-500">{r.customerName || 'Khách hàng'} · {reviewDate(r.createdAt)}</div></div><div className="flex items-center gap-1 text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={16} fill={i < r.rating ? 'currentColor' : 'none'} />)}</div></div>{r.content && <p className="my-4 whitespace-pre-wrap text-sm">{r.content}</p>}{r.replies.map(reply => <div key={reply.id} className="my-3 rounded-lg bg-indigo-50 p-3 text-sm"><b>Phản hồi của cửa hàng</b><p className="mt-1 whitespace-pre-wrap">{reply.content}</p></div>)}<div className="mt-4 flex flex-col gap-2 sm:flex-row"><textarea className="min-h-20 flex-1 rounded-lg border p-3 text-sm" maxLength={2000} placeholder={r.replies.length ? 'Cập nhật phản hồi của shop...' : 'Viết phản hồi...'} value={drafts[r.id] ?? r.replies[0]?.content ?? ''} onChange={e => setDrafts(d => ({ ...d, [r.id]: e.target.value }))} /><button disabled={busy === r.id || !(drafts[r.id] ?? r.replies[0]?.content)?.trim()} onClick={() => void send(r)} className="self-end rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{busy === r.id ? 'Đang lưu...' : r.replies.length ? 'Cập nhật phản hồi' : 'Gửi phản hồi'}</button></div></article>)}{reviews.length === 0 && !error && <div className="rounded-xl border bg-white p-12 text-center text-slate-500">Chưa có đánh giá nào.</div>}</div></div>;
}
