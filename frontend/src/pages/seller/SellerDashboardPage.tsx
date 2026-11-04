import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowUpRight, ClipboardList, Package, PackagePlus, ShoppingBag, Star, Wallet, Warehouse } from 'lucide-react';
import { sellerProductApi, type SellerProduct } from '../../services/seller/productApi';
import { dashboardApi, type ShopOwnerDashboard } from '../../services/seller/dashboardApi';
import { shopApi } from '../../services/shop/shopApi';
import { mediaUrl } from '../../services/api/mediaUrl';
import { getApiError } from '../../services/checkout/checkoutApi';

const styles = `.kd-page{padding-block:24px 32px;min-height:calc(100vh - 102px);color:#111827;font-size:14px}.kd-heading{display:flex;align-items:center;justify-content:space-between;gap:15px;margin-bottom:20px}.kd-heading h1{font-size:24px;font-weight:800;margin:0 0 4px}.kd-heading p{color:#64748B;font-size:14px;margin:0}.kd-new{display:flex;align-items:center;gap:9px;background:#5367C9;color:white;padding:10px 16px;border-radius:10px;text-decoration:none;font-weight:700;font-size:14px;white-space:nowrap}.kd-stats{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:16px;margin-bottom:16px}.kd-stat,.kd-card{background:white;border:1px solid #E2E8F0;border-radius:16px;box-shadow:0 3px 14px #14234b08}.kd-stat{display:flex;align-items:center;justify-content:space-between;padding:16px;min-height:94px;gap:8px}.kd-stat p{color:#64748B;font-size:12px;margin:0 0 8px}.kd-stat strong{font-size:18px;line-height:1.3}.kd-stat>span{width:44px;height:44px;flex-shrink:0;display:grid;place-items:center;border-radius:13px}.kd-grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:16px}.kd-grid>.kd-col-7{grid-column:span 7;min-width:0}.kd-grid>.kd-col-5{grid-column:span 5;min-width:0}.kd-card{padding:20px}.kd-card-head{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px}.kd-card-head h2{font-size:17px;font-weight:800;margin:0}.kd-card-head a{color:#5367C9;display:inline-flex;align-items:center;gap:3px;text-decoration:none;font-size:12px}.kd-card-head>span{color:#75809c;font-size:12px}.kd-chart-tools{display:flex;align-items:center;gap:10px}.kd-range{border:1px solid #dce3f0;border-radius:9px;padding:8px 10px;background:#fff;color:#24345b;font:inherit;font-size:12px}.kd-chart-totals{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:0 0 14px}.kd-chart-total{padding:10px 12px;border-radius:11px;background:#f7f8fc}.kd-chart-total small{display:block;color:#64748b;font-size:11px;margin-bottom:5px}.kd-chart-total strong{font-size:15px;color:#172554}.kd-chart-total.revenue strong{color:#bd6900}.kd-chart-legend{display:flex;align-items:center;gap:18px;margin:0 0 6px;font-size:11px;color:#52617d}.kd-legend-item{display:inline-flex;align-items:center;gap:7px}.kd-legend-bar{width:12px;height:12px;border-radius:3px;background:#6578d8}.kd-legend-line{width:18px;height:3px;border-radius:4px;background:#f59e0b;position:relative}.kd-legend-line:after{content:'';position:absolute;width:6px;height:6px;border-radius:50%;background:#f59e0b;left:6px;top:-2px}.kd-chart-frame{height:245px;width:100%;overflow:hidden}.kd-chart-svg{height:100%;width:100%;overflow:visible}.kd-chart-tick{font-size:11px;fill:#75809c}.kd-chart-axis{font-size:10px;fill:#8390a8}.kd-chart-note{margin:7px 0 0;color:#8390a8;font-size:11px;line-height:1.5}.kd-status-list>div{display:flex;align-items:center;gap:12px;border-bottom:1px solid #eff1f5;padding:11px 0;font-size:13px}.kd-status-list>div:last-child{border-bottom:0}.kd-status-list b{flex:1}.kd-status-list strong{font-size:15px}.kd-small-icon{width:34px;height:34px;display:grid;place-items:center;border-radius:11px;background:#EEF3FF;color:#5367C9}.kd-product{display:flex;align-items:center;gap:13px;padding:10px 0;border-bottom:1px solid #eff1f5;font-size:13px}.kd-product>span:nth-child(2){flex:1;min-width:0}.kd-product b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.kd-product p{color:#64718e;font-size:12px;margin:6px 0 0}.kd-thumb{width:48px;height:48px;flex-shrink:0;background:#f1f3f8;display:grid;place-items:center;border-radius:10px;overflow:hidden}.kd-thumb img{width:100%;height:100%;object-fit:cover}.kd-alert{background:#FFF0F0;color:#B42336;padding:12px;border-radius:9px;margin-bottom:15px}.kd-empty{color:#63718e;padding:25px 0;text-align:center}.kd-footer{text-align:center;color:#63718e;font-size:12px;padding:23px}@media(max-width:1279px){.kd-stats{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:1023px){.kd-grid>.kd-col-7,.kd-grid>.kd-col-5{grid-column:span 12}}@media(max-width:767px){.kd-stats{grid-template-columns:repeat(2,minmax(0,1fr))}.kd-page{padding-block:16px 24px}.kd-card{padding:15px}.kd-chart-frame{height:215px}}@media(max-width:500px){.kd-stats{grid-template-columns:1fr}.kd-heading{align-items:flex-start;flex-direction:column}.kd-chart-totals{grid-template-columns:1fr}.kd-chart-tools{gap:6px}.kd-range{padding:7px 6px}}`;
const money = (value: number | string | null | undefined) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value ?? 0));
const compactMoney = (value: number) => new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(value);
const dateLabel = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });

export default function SellerDashboardPage() {
  const [shopName, setShopName] = useState('Cửa hàng');
  const [stats, setStats] = useState<ShopOwnerDashboard | null>(null);
  const [products, setProducts] = useState<SellerProduct[]>([]);
  const [days, setDays] = useState<7 | 30>(7);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void shopApi.getMyShop().then(shop => { if (active) setShopName(shop.shopName); }).catch(() => {});
    void sellerProductApi.list({ page: 0, size: 6, sort: 'newest' }).then(page => { if (active) setProducts(page.items); }).catch(e => { if (active) setError(getApiError(e, 'Chưa tải được sản phẩm cửa hàng.')); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    setError('');
    void dashboardApi.get(days).then(data => { if (active) setStats(data); }).catch(e => { if (active) setError(getApiError(e, 'Chưa tải được số liệu cửa hàng.')); });
    return () => { active = false; };
  }, [days]);

  const series = stats?.dailyPerformance ?? [];
  const totals = useMemo(() => series.reduce((sum, point) => ({ orders: sum.orders + Number(point.deliveredOrders), revenue: sum.revenue + Number(point.revenue) }), { orders: 0, revenue: 0 }), [series]);
  const maxOrders = Math.max(1, ...series.map(point => Number(point.deliveredOrders)));
  const maxRevenue = Math.max(1, ...series.map(point => Number(point.revenue)));
  const plot = { left: 46, right: 62, top: 22, bottom: 34, width: 1000, height: 260 };
  const step = series.length ? (plot.width - plot.left - plot.right) / series.length : 0;
  const points = series.map((point, index) => {
    const x = plot.left + step * index + step / 2;
    const y = plot.height - plot.bottom - Number(point.revenue) / maxRevenue * (plot.height - plot.top - plot.bottom);
    const barHeight = Number(point.deliveredOrders) / maxOrders * (plot.height - plot.top - plot.bottom);
    return { ...point, x, y, barHeight, barY: plot.height - plot.bottom - barHeight };
  });
  const revenuePath = points.map((point, index) => {
    if (index === 0) return `M ${point.x} ${point.y}`;
    const previous = points[index - 1];
    const middleX = (previous.x + point.x) / 2;
    return `Q ${middleX} ${previous.y} ${middleX} ${(previous.y + point.y) / 2} T ${point.x} ${point.y}`;
  }).join(' ');
  const revenueArea = points.length ? `${revenuePath} L ${points[points.length - 1].x} ${plot.height - plot.bottom} L ${points[0].x} ${plot.height - plot.bottom} Z` : '';
  const cards = [
    { title: 'Doanh thu đã giao', value: money(stats?.revenue), icon: Wallet, color: '#5367C9', bg: '#EEF3FF' },
    { title: 'Tổng đơn hàng', value: stats?.totalOrders ?? '—', icon: ShoppingBag, color: '#7a4df1', bg: '#f0eaff' },
    { title: 'Sản phẩm đang bán', value: stats?.products.selling ?? '—', icon: Package, color: '#10ad61', bg: '#e4f9ec' },
    { title: 'Đánh giá trung bình', value: stats ? `${Number(stats.ratingAverage).toFixed(1)} / 5` : '—', icon: Star, color: '#f5ae00', bg: '#fff8e5' },
    { title: 'Lượt đánh giá', value: stats?.reviewCount ?? '—', icon: ClipboardList, color: '#f5801c', bg: '#fff0e6' },
  ];

  return <><style>{styles}</style><div className="kd-page seller-container">
    <div className="kd-heading"><div><h1>Chào mừng trở lại!</h1><p>Tổng quan hoạt động của {shopName}.</p></div><Link className="kd-new" to="/seller/products/new"><PackagePlus size={18}/> Thêm sản phẩm</Link></div>
    {error && <div className="kd-alert">{error}</div>}
    <div className="kd-stats">{cards.map(card => <div className="kd-stat" key={card.title}><div><p>{card.title}</p><strong>{card.value}</strong></div><span style={{ color: card.color, background: card.bg }}><card.icon size={23}/></span></div>)}</div>
    <div className="kd-grid">
      <section className="kd-card kd-col-7">
        <div className="kd-card-head"><h2>Hiệu quả đơn hàng</h2><div className="kd-chart-tools"><span>{days} ngày gần nhất</span><select className="kd-range" aria-label="Khoảng thời gian biểu đồ" value={days} onChange={event => setDays(Number(event.target.value) as 7 | 30)}><option value={7}>7 ngày</option><option value={30}>30 ngày</option></select></div></div>
        <div className="kd-chart-totals"><div className="kd-chart-total"><small>Đơn đã giao trong kỳ</small><strong>{totals.orders.toLocaleString('vi-VN')} đơn</strong></div><div className="kd-chart-total revenue"><small>Doanh thu trong kỳ</small><strong>{money(totals.revenue)}</strong></div></div>
        <div className="kd-chart-legend"><span className="kd-legend-item"><i className="kd-legend-bar"/> Cột: số đơn đã giao</span><span className="kd-legend-item"><i className="kd-legend-line"/> Đường: doanh thu</span></div>
        <div className="kd-chart-frame" role="img" aria-label={`Biểu đồ ${days} ngày: cột thể hiện số đơn đã giao, đường thể hiện doanh thu`}>
          <svg className="kd-chart-svg" viewBox={`0 0 ${plot.width} ${plot.height}`} preserveAspectRatio="none">
            <defs><linearGradient id="kd-revenue-area" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#f59e0b" stopOpacity=".16"/><stop offset="100%" stopColor="#f59e0b" stopOpacity="0"/></linearGradient><linearGradient id="kd-bar-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#8292ed"/><stop offset="100%" stopColor="#596dce"/></linearGradient></defs>
            {[0, 1, 2, 3].map(index => { const ratio = index / 3; const y = plot.top + ratio * (plot.height - plot.top - plot.bottom); const orderValue = Math.round(maxOrders * (1 - ratio)); const showOrder = index === 0 || index === 3 || (index === 2 && orderValue > 0 && orderValue < maxOrders); return <g key={index}><line x1={plot.left} x2={plot.width - plot.right} y1={y} y2={y} stroke="#edf0f6" strokeDasharray={index && index !== 3 ? '3 5' : undefined}/>{showOrder && <text className="kd-chart-axis" x="3" y={y + 4}>{index === 0 ? Math.ceil(maxOrders) : orderValue}</text>}<text className="kd-chart-axis" x={plot.width - plot.right + 8} y={y + 4}>{compactMoney(maxRevenue * (1 - ratio))}</text></g>; })}
            <line x1={plot.left} x2={plot.width - plot.right} y1={plot.height - plot.bottom} y2={plot.height - plot.bottom} stroke="#dfe4ee"/>
            {points.length > 1 && <path d={revenueArea} fill="url(#kd-revenue-area)"/>}
            {points.map((point, index) => <g key={point.date}><rect x={point.x - Math.min(15, step * .27)} y={point.barY} width={Math.min(30, step * .54)} height={Math.max(2, point.barHeight)} rx="5" fill="url(#kd-bar-fill)"><title>{dateLabel(point.date)}: {point.deliveredOrders} đơn đã giao</title></rect>{point.deliveredOrders > 0 && days === 7 && <text x={point.x} y={point.barY + 14} textAnchor="middle" fontSize="10" fontWeight="700" fill="#fff">{point.deliveredOrders}</text>}<text className="kd-chart-tick" x={point.x} y={plot.height - 8} textAnchor="middle">{days === 7 || index % 5 === 0 || index === points.length - 1 ? dateLabel(point.date) : ''}</text></g>)}
            {points.length > 1 && <path d={revenuePath} fill="none" stroke="#fff" strokeWidth="7" strokeLinejoin="round" strokeLinecap="round"/>}
            {points.length > 1 && <path d={revenuePath} fill="none" stroke="#f59e0b" strokeWidth="3.5" strokeLinejoin="round" strokeLinecap="round"/>}
            {points.map(point => <circle key={`revenue-${point.date}`} cx={point.x} cy={point.y} r={days === 7 ? 5 : 3.5} fill="#fff" stroke="#f59e0b" strokeWidth="3"><title>{dateLabel(point.date)}: doanh thu {money(point.revenue)}</title></circle>)}
          </svg>
        </div>
        <p className="kd-chart-note">Cột biểu diễn số đơn đã giao; đường biểu diễn doanh thu từ các đơn đó. Rê chuột lên cột hoặc điểm trên đường để xem số liệu chính xác theo ngày.</p>
      </section>
      <section className="kd-card kd-col-5"><div className="kd-card-head"><h2>Trạng thái đơn hàng</h2><Link to="/shop-owner/orders">Quản lý đơn <ArrowUpRight size={14}/></Link></div><div className="kd-status-list"><div><span className="kd-small-icon"><ShoppingBag size={18}/></span><b>Tổng đơn hàng</b><strong>{stats?.totalOrders ?? '—'}</strong></div><div><span className="kd-small-icon"><ClipboardList size={18}/></span><b>Chờ xác nhận</b><strong>{stats?.pendingOrders ?? '—'}</strong></div><div><span className="kd-small-icon"><Package size={18}/></span><b>Đang vận chuyển</b><strong>{stats?.shippingOrders ?? '—'}</strong></div><div><span className="kd-small-icon"><Wallet size={18}/></span><b>Đã giao</b><strong>{stats?.deliveredOrders ?? '—'}</strong></div></div></section>
      <section className="kd-card kd-col-7"><div className="kd-card-head"><h2>Sản phẩm mới cập nhật</h2><Link to="/seller/products">Xem tất cả <ArrowUpRight size={14}/></Link></div>{products.length ? products.map(product => <div className="kd-product" key={product.id}><span className="kd-thumb">{product.thumbnailUrl ? <img src={mediaUrl(product.thumbnailUrl)} alt=""/> : <Package size={22}/>}</span><span><b>{product.name}</b><p>{product.categoryName || 'Chưa phân loại'}</p></span><strong>{money(product.minPrice)}</strong></div>) : <p className="kd-empty">Chưa có sản phẩm.</p>}</section>
      <section className="kd-card kd-col-5"><div className="kd-card-head"><h2>Cần theo dõi</h2><Link to="/seller/inventory">Mở kho <ArrowUpRight size={14}/></Link></div><div className="kd-status-list"><div><span className="kd-small-icon"><AlertTriangle size={18}/></span><b>Sản phẩm sắp hết hàng</b><strong>{stats?.products.lowStock ?? '—'}</strong></div><div><span className="kd-small-icon"><ClipboardList size={18}/></span><b>Sản phẩm chờ duyệt</b><strong>{stats?.products.pending ?? '—'}</strong></div><div><span className="kd-small-icon"><Warehouse size={18}/></span><b>Sản phẩm hết hàng</b><strong>{stats?.products.outOfStock ?? '—'}</strong></div></div></section>
    </div><footer className="kd-footer">Kala Store · Thống kê cửa hàng</footer>
  </div></>;
}
