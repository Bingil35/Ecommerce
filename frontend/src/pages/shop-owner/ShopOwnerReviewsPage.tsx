import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Pencil, Star } from 'lucide-react';
import { reviewApi, reviewDate, type ProductReview } from '../../services/reviews/reviewApi';
import { getApiError } from '../../services/checkout/checkoutApi';
import { mediaUrl } from '../../services/api/mediaUrl';

export default function ShopOwnerReviewsPage() {
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [ratingFilter, setRatingFilter] = useState(0);
  const [editingReplies, setEditingReplies] = useState<Record<string, boolean>>({});
  const load = useCallback(async () => { try { setReviews(await reviewApi.ownerList()); setError(''); } catch (e) { setError(getApiError(e)); } }, []);
  useEffect(() => { void load(); }, [load]);
  const send = async (review: ProductReview) => {
    const content = drafts[review.id] ?? review.replies[0]?.content ?? '';
    try { setBusy(review.id); await reviewApi.reply(review.id, content); await load(); setEditingReplies(current => ({ ...current, [review.id]: false })); } catch (e) { setError(getApiError(e)); } finally { setBusy(null); }
  };
  const visibleReviews = useMemo(() => reviews.filter(review => ratingFilter === 0 || review.rating === ratingFilter), [reviews, ratingFilter]);
  const groups = useMemo(() => { const map = new Map<string, ProductReview[]>(); visibleReviews.forEach(review => map.set(review.productId, [...(map.get(review.productId) ?? []), review])); return [...map.entries()]; }, [visibleReviews]);

  return <div className="seller-container shop-management">
    <h1 style={{ fontWeight: 650 }} className="text-2xl">Đánh giá sản phẩm</h1><p className="mb-5 mt-1 text-sm font-normal text-slate-500">Đọc phản hồi của khách và trả lời với tư cách cửa hàng.</p>
    {error && <div className="mb-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}
    <div role="tablist" aria-label="Lọc đánh giá theo số sao" className="mb-5 flex gap-1 overflow-x-auto border-b border-slate-200 [scrollbar-width:none]">
      {[0, 5, 4, 3, 2, 1].map(star => <button key={star} type="button" role="tab" aria-selected={ratingFilter === star} onClick={() => setRatingFilter(star)} className={`shrink-0 border-b-2 px-3 py-3 text-sm font-medium ${ratingFilter === star ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500'}`}>{star === 0 ? 'Tất cả' : `${star} sao`}</button>)}
    </div>
    <div className="space-y-5">
      {groups.map(([productId, group]) => <section key={productId} className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <header className="flex items-center gap-3 border-b border-slate-100 p-4 sm:p-5">{group[0].imageUrl ? <img src={mediaUrl(group[0].imageUrl)} alt="" className="h-14 w-14 shrink-0 rounded-xl border border-slate-100 object-cover" /> : <span className="h-14 w-14 shrink-0 rounded-xl bg-slate-100" />}<div className="min-w-0"><h2 className="truncate text-base font-medium text-slate-800">{group[0].productName}</h2><p className="mt-1 text-xs font-normal text-slate-500">{group.length} đánh giá</p></div></header>
        <div className="divide-y divide-slate-100">{group.map(review => <article key={review.id} className="p-4 sm:p-5">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-sm font-medium text-slate-700">{review.customerName || 'Khách hàng'}</p><p className="mt-1 text-xs font-normal text-slate-500">{reviewDate(review.createdAt)}</p></div><div aria-label={`${review.rating} sao`} className="flex items-center gap-1 text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={15} fill={i < review.rating ? 'currentColor' : 'none'} />)}</div></div>
          {review.variantOptions?.length > 0 && <p className="mt-2 text-xs font-normal text-slate-500">Phân loại: {review.variantOptions.join(' · ')}</p>}
          {review.content && <p className="my-3 whitespace-pre-wrap text-sm font-normal leading-6 text-slate-700">{review.content}</p>}
          {review.orderCode && review.subOrderId && <p className="text-xs font-normal text-slate-500">Đơn hàng: <Link to={`/shop-owner/orders/${review.subOrderId}`} className="font-medium text-indigo-600 hover:underline">{review.orderCode} · Chi tiết đơn hàng</Link></p>}
          {review.replies.map(reply => <div key={reply.id} className="my-3 rounded-lg bg-indigo-50 p-3 text-sm font-normal"><div className="flex items-center justify-between gap-3"><span className="font-medium">Phản hồi của cửa hàng</span><button type="button" aria-expanded={Boolean(editingReplies[review.id])} onClick={() => { setDrafts(d => ({ ...d, [review.id]: d[review.id] ?? reply.content })); setEditingReplies(current => ({ ...current, [review.id]: !current[review.id] })); }} className="inline-flex shrink-0 items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-indigo-700 transition-colors hover:bg-indigo-100"><Pencil size={13}/>{editingReplies[review.id] ? 'Đóng chỉnh sửa' : 'Chỉnh sửa'}</button></div><p className="mt-1 whitespace-pre-wrap">{reply.content}</p></div>)}
          {review.replies.length > 0 ? <div className={`grid transition-[grid-template-rows,opacity] duration-300 ease-in-out ${editingReplies[review.id] ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}><div className="overflow-hidden"><div className="mt-3 flex flex-col gap-2 sm:flex-row"><textarea aria-label="Nội dung phản hồi của cửa hàng" className="min-h-20 min-w-0 flex-1 rounded-lg border border-slate-200 bg-white p-3 text-sm font-normal outline-none focus:border-indigo-400" maxLength={2000} placeholder="Cập nhật phản hồi của shop..." value={drafts[review.id] ?? review.replies[0]?.content ?? ''} onChange={e => setDrafts(d => ({ ...d, [review.id]: e.target.value }))} /><button disabled={busy === review.id || !(drafts[review.id] ?? review.replies[0]?.content)?.trim()} onClick={() => void send(review)} className="self-end rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">{busy === review.id ? 'Đang lưu...' : 'Cập nhật phản hồi'}</button></div></div></div> : <div className="mt-4 flex flex-col gap-2 sm:flex-row"><textarea aria-label="Viết phản hồi cho đánh giá" className="min-h-20 min-w-0 flex-1 rounded-lg border border-slate-200 p-3 text-sm font-normal" maxLength={2000} placeholder="Viết phản hồi..." value={drafts[review.id] ?? ''} onChange={e => setDrafts(d => ({ ...d, [review.id]: e.target.value }))} /><button disabled={busy === review.id || !drafts[review.id]?.trim()} onClick={() => void send(review)} className="self-end rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">{busy === review.id ? 'Đang lưu...' : 'Gửi phản hồi'}</button></div>}
        </article>)}</div>
      </section>)}
      {reviews.length === 0 && !error && <div className="rounded-xl border bg-white p-12 text-center text-slate-500">Chưa có đánh giá nào.</div>}
      {reviews.length > 0 && groups.length === 0 && <div className="rounded-xl border bg-white p-10 text-center text-slate-500">Không có đánh giá phù hợp với số sao đã chọn.</div>}
    </div>
  </div>;
}
