import { useEffect, useState } from 'react';
import { Star } from 'lucide-react';
import { reviewApi, reviewDate, type ProductReview } from '../../../services/reviews/reviewApi';

export default function ProductReviews({productId}:{productId:string}){
  const [reviews,setReviews]=useState<ProductReview[]>([]),[error,setError]=useState('');
  useEffect(()=>{let active=true;reviewApi.list(productId).then(data=>{if(active)setReviews(data);}).catch(()=>{if(active)setError('Chưa thể tải đánh giá sản phẩm.');});return()=>{active=false;};},[productId]);
  const average=reviews.length?reviews.reduce((total,r)=>total+r.rating,0)/reviews.length:0;
  return <section className="mt-6 rounded-3xl border border-[#E7ECF5] bg-white p-5 sm:p-7"><div className="flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-lg font-bold text-[#202642] sm:text-xl">Đánh giá sản phẩm</h2><p className="mt-1 text-sm text-[#7B8498]">{reviews.length?`${average.toFixed(1)} / 5 · ${reviews.length} đánh giá`:'Chưa có đánh giá'}</p></div><div className="flex items-center gap-1 text-amber-500">{Array.from({length:5},(_,i)=><Star key={i} size={18} fill={i<Math.round(average)?'currentColor':'none'}/>)}</div></div>
    {error&&<p className="mt-4 text-sm text-rose-600">{error}</p>}{!error&&reviews.length===0&&<p className="mt-5 rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Hãy là người đầu tiên đánh giá sản phẩm này.</p>}
    <div className="mt-4 divide-y divide-slate-100">{reviews.map(review=><article key={review.id} className="py-4 first:pt-1"><div className="flex flex-wrap items-center justify-between gap-2"><div className="font-semibold text-slate-700">{review.customerName||'Khách hàng'}</div><div className="text-xs text-slate-400">{reviewDate(review.createdAt)}</div></div><div className="mt-1 flex gap-0.5 text-amber-500">{Array.from({length:5},(_,i)=><Star key={i} size={14} fill={i<review.rating?'currentColor':'none'}/>)}</div>{review.content&&<p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">{review.content}</p>}{review.replies.map(reply=><div key={reply.id} className="mt-3 rounded-xl bg-indigo-50 p-3 text-sm"><b className="text-indigo-800">Phản hồi của cửa hàng</b><p className="mt-1 whitespace-pre-wrap text-slate-700">{reply.content}</p></div>)}</article>)}</div>
  </section>;
}
