import { useEffect, useMemo, useState } from 'react';
import { Star } from 'lucide-react';
import { reviewApi, reviewDate, type ProductReview } from '../../../services/reviews/reviewApi';

export default function ProductReviews({ productId }: { productId: string }) {
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [error, setError] = useState('');
  const [ratingFilter, setRatingFilter] = useState<number | null>(null);
  useEffect(() => {
    let active = true;
    setError('');
    setRatingFilter(null);
    reviewApi.list(productId).then(data => { if (active) setReviews(data); })
      .catch(() => { if (active) setError('Chưa thể tải đánh giá sản phẩm.'); });
    return () => { active = false; };
  }, [productId]);

  const average = reviews.length ? reviews.reduce((total, review) => total + review.rating, 0) / reviews.length : 0;
  const counts = useMemo(() => reviews.reduce((result, review) => {
    result[review.rating] = (result[review.rating] ?? 0) + 1;
    return result;
  }, {} as Record<number, number>), [reviews]);
  const visibleReviews = ratingFilter === null ? reviews : reviews.filter(review => review.rating === ratingFilter);

  return <section className="mt-6 rounded-3xl border border-[#E7ECF5] bg-white p-5 sm:p-7">
    <div className="flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-lg font-bold text-[#202642] sm:text-xl">Đánh giá sản phẩm</h2><p className="mt-1 text-sm text-[#7B8498]">{reviews.length ? `${average.toFixed(1)} / 5 · ${reviews.length} đánh giá` : 'Chưa có đánh giá'}</p></div><div className="flex items-center gap-1 text-amber-500" aria-label={`Trung bình ${average.toFixed(1)} trên 5 sao`}>{Array.from({ length: 5 }, (_, i) => <Star key={i} size={18} fill={i < Math.round(average) ? 'currentColor' : 'none'} />)}</div></div>
    {error && <p className="mt-4 text-sm text-rose-600">{error}</p>}
    {!error && reviews.length > 0 && <div className="mt-5 grid gap-5 rounded-2xl bg-[#F8FAFD] p-4 sm:grid-cols-[180px_1fr] sm:p-5">
      <div className="flex flex-col items-center justify-center border-b border-[#E8EDF5] pb-4 sm:border-b-0 sm:border-r sm:pb-0 sm:pr-5"><strong className="text-4xl leading-none text-[#252B40]">{average.toFixed(1)}</strong><div className="mt-2 flex text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={16} fill={i < Math.round(average) ? 'currentColor' : 'none'} />)}</div><span className="mt-1 text-xs text-[#7B8498]">{reviews.length} đánh giá</span></div>
      <div className="space-y-2">{[5, 4, 3, 2, 1].map(stars => <button type="button" key={stars} aria-pressed={ratingFilter === stars} onClick={() => setRatingFilter(current => current === stars ? null : stars)} className={`flex w-full items-center gap-3 rounded-lg px-2 py-1 text-left text-xs transition hover:bg-white ${ratingFilter === stars ? 'bg-white ring-1 ring-[#CFD8F4]' : ''}`}><span className="flex w-10 shrink-0 items-center gap-1 text-[#667085]">{stars} <Star size={12} className="text-amber-500" fill="currentColor"/></span><span className="h-2 flex-1 overflow-hidden rounded-full bg-[#E5EAF2]"><span className="block h-full rounded-full bg-amber-400" style={{ width: `${reviews.length ? (counts[stars] ?? 0) / reviews.length * 100 : 0}%` }}/></span><span className="w-7 text-right text-[#7B8498]">{counts[stars] ?? 0}</span></button>)}</div>
    </div>}
    {!error && reviews.length === 0 && <p className="mt-5 rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Hãy là người đầu tiên đánh giá sản phẩm này.</p>}
    {reviews.length > 0 && <div className="mt-5 flex flex-wrap items-center justify-between gap-2"><h3 className="text-sm font-semibold text-[#35405A]">Nhận xét từ khách hàng</h3><label className="flex items-center gap-2 text-xs text-[#667085]">Lọc theo sao<select value={ratingFilter ?? ''} onChange={event => setRatingFilter(event.target.value ? Number(event.target.value) : null)} className="h-9 rounded-lg border border-[#DCE3EF] bg-white px-2 text-xs text-[#46516A]"><option value="">Tất cả ({reviews.length})</option>{[5, 4, 3, 2, 1].map(stars => <option key={stars} value={stars}>{stars} sao ({counts[stars] ?? 0})</option>)}</select></label></div>}
    {!error && ratingFilter !== null && visibleReviews.length === 0 && <p className="mt-5 rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Chưa có đánh giá {ratingFilter} sao.</p>}
    <div className="mt-3 divide-y divide-slate-100">{visibleReviews.map(review => <article key={review.id} className="py-4 first:pt-1"><div className="flex flex-wrap items-center justify-between gap-2"><div className="font-semibold text-slate-700">{review.customerName || 'Khách hàng'}</div><div className="text-xs text-slate-400">{reviewDate(review.createdAt)}</div></div><div className="mt-1 flex gap-0.5 text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={14} fill={i < review.rating ? 'currentColor' : 'none'} />)}</div>{review.content && <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">{review.content}</p>}{review.replies.map(reply => <div key={reply.id} className="mt-3 rounded-xl bg-indigo-50 p-3 text-sm"><b className="text-indigo-800">Phản hồi của cửa hàng</b><p className="mt-1 whitespace-pre-wrap text-slate-700">{reply.content}</p></div>)}</article>)}</div>
  </section>;
}
