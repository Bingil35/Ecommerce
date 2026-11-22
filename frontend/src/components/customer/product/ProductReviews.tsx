import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Star, ThumbsUp } from 'lucide-react';
import { reviewApi, reviewDate, type ProductReview } from '../../../services/reviews/reviewApi';
import { useAuth } from '../../../context/AuthContext';
import { ROLES, hasRole } from '../../../services/auth/roles';
import { useToast } from '../../../context/FeedbackContext';

const REVIEWS_PER_PAGE = 5;

export default function ProductReviews({ productId }: { productId: string }) {
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [error, setError] = useState('');
  const [ratingFilter, setRatingFilter] = useState<number | null>(null);
  const [page, setPage] = useState(1);
  const [likeBusyIds, setLikeBusyIds] = useState<string[]>([]);
  const { user } = useAuth();
  const toast = useToast();
  const canLike = hasRole(user, ROLES.CUSTOMER);

  useEffect(() => {
    let active = true;
    setError('');
    setRatingFilter(null);
    setPage(1);
    reviewApi.list(productId)
      .then(data => { if (active) setReviews(data); })
      .catch(() => { if (active) setError('Chưa thể tải đánh giá sản phẩm.'); });
    return () => { active = false; };
  }, [productId]);

  const average = reviews.length ? reviews.reduce((total, review) => total + review.rating, 0) / reviews.length : 0;
  const counts = useMemo(() => reviews.reduce((result, review) => {
    result[review.rating] = (result[review.rating] ?? 0) + 1;
    return result;
  }, {} as Record<number, number>), [reviews]);
  const visibleReviews = ratingFilter === null ? reviews : reviews.filter(review => review.rating === ratingFilter);
  const totalPages = Math.ceil(visibleReviews.length / REVIEWS_PER_PAGE);
  const pageReviews = visibleReviews.slice((page - 1) * REVIEWS_PER_PAGE, page * REVIEWS_PER_PAGE);

  const toggleLike = async (review: ProductReview) => {
    if (!canLike || likeBusyIds.includes(review.id)) return;
    setLikeBusyIds(current => [...current, review.id]);
    try {
      const result = review.likedByMe
        ? await reviewApi.unlike(review.id)
        : await reviewApi.like(review.id);
      setReviews(current => current.map(item => item.id === review.id
        ? { ...item, likedByMe: result.liked, likeCount: result.likeCount }
        : item));
    } catch {
      toast.error('Không thể cập nhật lượt thích. Vui lòng thử lại.');
    } finally {
      setLikeBusyIds(current => current.filter(id => id !== review.id));
    }
  };

  return <section className="mt-6 min-w-0 rounded-3xl border border-[#E7ECF5] bg-white p-4 sm:p-7">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><h2 className="text-lg font-bold text-[#202642] sm:text-xl">Đánh giá sản phẩm</h2><p className="mt-1 text-sm text-[#7B8498]">{reviews.length ? `${average.toFixed(1)} / 5 · ${reviews.length} đánh giá` : 'Chưa có đánh giá'}</p></div>
      <div className="flex items-center gap-1 text-amber-500" aria-label={`Trung bình ${average.toFixed(1)} trên 5 sao`}>{Array.from({ length: 5 }, (_, i) => <Star key={i} size={18} fill={i < Math.round(average) ? 'currentColor' : 'none'} />)}</div>
    </div>
    {error && <p className="mt-4 text-sm text-rose-600">{error}</p>}
    {!error && reviews.length > 0 && <div className="mt-5 grid gap-5 rounded-2xl bg-[#F8FAFD] p-4 sm:grid-cols-[180px_minmax(0,1fr)] sm:p-5">
      <div className="flex flex-col items-center justify-center border-b border-[#E8EDF5] pb-4 sm:border-b-0 sm:border-r sm:pb-0 sm:pr-5"><strong className="text-4xl leading-none text-[#252B40]">{average.toFixed(1)}</strong><div className="mt-2 flex text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={16} fill={i < Math.round(average) ? 'currentColor' : 'none'} />)}</div><span className="mt-1 text-xs text-[#7B8498]">{reviews.length} đánh giá</span></div>
      <div className="space-y-2">{[5, 4, 3, 2, 1].map(stars => <div key={stars} className="flex items-center gap-3 px-2 py-1 text-xs"><span className="flex w-10 shrink-0 items-center gap-1 text-[#667085]">{stars} <Star size={12} className="text-amber-500" fill="currentColor"/></span><span className="h-2 flex-1 overflow-hidden rounded-full bg-[#E5EAF2]"><span className="block h-full rounded-full bg-amber-400" style={{ width: `${(counts[stars] ?? 0) / reviews.length * 100}%` }}/></span><span className="w-7 text-right text-[#7B8498]">{counts[stars] ?? 0}</span></div>)}</div>
    </div>}
    {!error && reviews.length === 0 && <p className="mt-5 rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Hãy là người đầu tiên đánh giá sản phẩm này.</p>}
    {reviews.length > 0 && <div className="mt-5">
      <h3 className="text-sm font-semibold text-[#35405A]">Nhận xét từ khách hàng</h3>
      <div role="tablist" aria-label="Lọc nhận xét theo số sao" className="-mx-4 mt-3 flex gap-2 overflow-x-auto border-b border-[#E9EDF4] px-4 [scrollbar-width:none] sm:mx-0 sm:px-0 [&::-webkit-scrollbar]:hidden">
        <button type="button" role="tab" aria-selected={ratingFilter === null} onClick={() => { setRatingFilter(null); setPage(1); }} className={`shrink-0 border-b-2 px-3 py-2 text-xs font-semibold transition sm:text-sm ${ratingFilter === null ? 'border-[#5367C9] text-[#5367C9]' : 'border-transparent text-[#778197] hover:text-[#46516A]'}`}>Tất cả <span className="ml-1 opacity-70">({reviews.length})</span></button>
        {[5, 4, 3, 2, 1].map(stars => <button key={stars} type="button" role="tab" aria-selected={ratingFilter === stars} onClick={() => { setRatingFilter(stars); setPage(1); }} className={`inline-flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2 text-xs font-semibold transition sm:text-sm ${ratingFilter === stars ? 'border-[#5367C9] text-[#5367C9]' : 'border-transparent text-[#778197] hover:text-[#46516A]'}`}><Star size={13} className="text-amber-500" fill="currentColor"/>{stars} sao <span className="opacity-70">({counts[stars] ?? 0})</span></button>)}
      </div>
    </div>}
    {!error && ratingFilter !== null && visibleReviews.length === 0 && <p className="mt-5 rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Chưa có đánh giá {ratingFilter} sao.</p>}
      <div className="mt-3 divide-y divide-slate-100">{pageReviews.map(review => <article key={review.id} className="py-4 first:pt-1"><div className="flex flex-wrap items-start justify-between gap-2"><div><div className="font-semibold text-slate-700">{review.customerName || 'Khách hàng'}</div><div className="mt-1 flex gap-0.5 text-amber-500">{Array.from({ length: 5 }, (_, i) => <Star key={i} size={14} fill={i < review.rating ? 'currentColor' : 'none'} />)}</div></div><div className="flex flex-col items-end gap-1 text-[13px] text-slate-400"><span>{reviewDate(review.createdAt)}</span>{canLike ? <button type="button" aria-label={review.likedByMe ? 'Bỏ thích đánh giá' : 'Thích đánh giá'} aria-pressed={review.likedByMe} disabled={likeBusyIds.includes(review.id)} onClick={() => void toggleLike(review)} className={`inline-flex items-center gap-1 rounded px-1 py-0.5 text-[13px] transition disabled:cursor-wait disabled:opacity-50 ${review.likedByMe ? 'text-[#5367C9]' : 'text-slate-400 hover:text-[#5367C9]'}`}><ThumbsUp size={16} fill={review.likedByMe ? 'currentColor' : 'none'}/><span>{review.likeCount}</span></button> : <span className="inline-flex items-center gap-1 px-1 py-0.5 text-[13px]"><ThumbsUp size={16}/><span>{review.likeCount}</span></span>}</div></div>{review.content && <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">{review.content}</p>}{review.replies.map(reply => <div key={reply.id} className="mt-3 rounded-xl bg-indigo-50 p-3 text-sm"><b className="text-indigo-800">Phản hồi của cửa hàng</b><p className="mt-1 whitespace-pre-wrap text-slate-700">{reply.content}</p></div>)}</article>)}</div>
    {visibleReviews.length > 0 && <div className="mt-4 flex flex-col items-center justify-between gap-3 border-t border-slate-100 pt-4 sm:flex-row">
      <p className="text-xs text-[#7B8498]">Đang hiển thị {((page - 1) * REVIEWS_PER_PAGE) + 1}–{Math.min(page * REVIEWS_PER_PAGE, visibleReviews.length)} trong {visibleReviews.length} nhận xét</p>
      {totalPages > 1 && <nav aria-label="Phân trang nhận xét" className="flex items-center gap-1">
        <button type="button" aria-label="Trang trước" disabled={page === 1} onClick={() => setPage(current => current - 1)} className="grid h-9 w-9 place-items-center rounded-lg border border-[#E1E7F2] text-[#5367C9] transition hover:bg-[#F3F5FC] disabled:cursor-not-allowed disabled:opacity-40"><ChevronLeft size={17}/></button>
        {Array.from({ length: totalPages }, (_, index) => index + 1).map(pageNumber => <button key={pageNumber} type="button" aria-label={`Trang ${pageNumber}`} aria-current={pageNumber === page ? 'page' : undefined} onClick={() => setPage(pageNumber)} className={`h-9 min-w-9 rounded-lg px-2 text-sm font-semibold transition ${pageNumber === page ? 'bg-[#5367C9] text-white' : 'text-[#5367C9] hover:bg-[#F3F5FC]'}`}>{pageNumber}</button>)}
        <button type="button" aria-label="Trang sau" disabled={page === totalPages} onClick={() => setPage(current => current + 1)} className="grid h-9 w-9 place-items-center rounded-lg border border-[#E1E7F2] text-[#5367C9] transition hover:bg-[#F3F5FC] disabled:cursor-not-allowed disabled:opacity-40"><ChevronRight size={17}/></button>
      </nav>}
    </div>}
  </section>;
}
