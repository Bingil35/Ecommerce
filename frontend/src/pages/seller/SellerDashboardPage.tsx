import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowRight, CalendarDays, ClipboardList, Clock3, Package, ShoppingBag, Star, TrendingUp, Wallet } from 'lucide-react';
import { dashboardApi, type ShopOwnerDashboard } from '../../services/seller/dashboardApi';
import { shopApi } from '../../services/shop/shopApi';
import { getApiError } from '../../services/checkout/checkoutApi';

const styles = `.kd-page{width:100%;max-width:1200px;box-sizing:border-box;margin-inline:auto;min-height:calc(100vh - 102px);padding-block:26px 38px;color:#202642;font-size:14px}.kd-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:22px}.kd-title h1{margin:0;color:#202642;font-size:25px;font-weight:700;letter-spacing:-.035em}.kd-title p{margin:6px 0 0;color:#7b8498;font-size:13px}.kd-period{display:flex;align-items:flex-end;justify-content:flex-end;gap:8px;flex-wrap:wrap}.kd-period label{display:grid;gap:5px;color:#75809a;font-size:10px;font-weight:550}.kd-period select,.kd-period input{height:38px;min-width:125px;border:1px solid #dfe5f0;border-radius:10px;background:#fff;padding:0 10px;color:#35405a;font:inherit;font-size:12px;outline:none}.kd-period input{min-width:136px}.kd-period select:focus,.kd-period input:focus{border-color:#9eabe7;box-shadow:0 0 0 3px #edf0ff}.kd-period button{display:inline-flex;height:38px;align-items:center;gap:7px;border:0;border-radius:10px;background:#5367c9;padding:0 14px;color:#fff;font-size:12px;font-weight:600;cursor:pointer}.kd-period button:hover{background:#4659b8}.kd-period button:disabled{opacity:.55;cursor:wait}.kd-range-error{margin:-10px 0 15px;color:#c2414d;font-size:12px}.kd-alert{margin-bottom:16px;border:1px solid #f4c9cd;border-radius:12px;background:#fff4f4;padding:12px 14px;color:#ae3947;font-size:13px}.kd-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:16px}.kd-kpi,.kd-card{min-width:0;border:1px solid #e4e9f2;border-radius:17px;background:#fff;box-shadow:0 5px 20px rgba(31,48,88,.045)}.kd-kpi{display:flex;min-height:116px;align-items:flex-start;justify-content:space-between;gap:12px;padding:18px}.kd-kpi-copy{min-width:0}.kd-kpi small{display:block;margin-bottom:10px;color:#78839a;font-size:12px;font-weight:550}.kd-kpi strong{display:block;overflow:hidden;color:#202642;font-size:21px;font-weight:700;letter-spacing:-.03em;text-overflow:ellipsis;white-space:nowrap}.kd-kpi em{display:block;margin-top:7px;color:#9aa3b3;font-size:10px;font-style:normal}.kd-kpi-icon{display:grid;width:40px;height:40px;flex:none;place-items:center;border-radius:13px}.kd-kpi-icon.revenue{background:#f1efff;color:#596bd0}.kd-kpi-icon.orders{background:#eaf5fb;color:#4a91b6}.kd-kpi-icon.average{background:#fff4e7;color:#cc8c31}.kd-kpi-icon.delivered{background:#eaf7ef;color:#47a273}.kd-dashboard-grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:16px}.kd-card{padding:19px}.kd-chart-card{grid-column:span 8}.kd-status-card{grid-column:span 4}.kd-recent-card{grid-column:span 7}.kd-watch-card{grid-column:span 5}.kd-card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:16px}.kd-card-head h2{margin:0;color:#27314c;font-size:16px;font-weight:650;letter-spacing:-.015em}.kd-card-head p{margin:5px 0 0;color:#8b95a8;font-size:11px}.kd-card-link{display:inline-flex;flex:none;align-items:center;gap:4px;color:#596bd0;text-decoration:none;font-size:11px;font-weight:600}.kd-card-link:hover{color:#3e50b2}.kd-chart-legend{display:flex;flex-wrap:wrap;align-items:center;gap:15px;margin:-3px 0 8px;color:#68758d;font-size:11px}.kd-legend-item{display:inline-flex;align-items:center;gap:7px}.kd-legend-bar{width:11px;height:11px;border-radius:3px;background:#7081da}.kd-legend-line{position:relative;width:19px;height:3px;border-radius:4px;background:#f1a53b}.kd-legend-line:after{position:absolute;top:-2px;left:7px;width:7px;height:7px;border:1px solid white;border-radius:50%;background:#f1a53b;content:''}.kd-chart-frame{width:100%;height:270px;overflow:hidden}.kd-chart-svg{width:100%;height:100%;overflow:visible}.kd-chart-tick{fill:#7e899e;font-size:10px}.kd-chart-axis{fill:#9aa3b3;font-size:9px}.kd-chart-note{margin:5px 0 0;color:#8a94a8;font-size:10px;line-height:1.5}.kd-donut-wrap{display:flex;align-items:center;justify-content:center;gap:18px;min-height:218px}.kd-donut{width:min(48%,210px);min-width:130px;overflow:visible}.kd-donut-total{fill:#27314c;font-size:22px;font-weight:700}.kd-donut-caption{fill:#8a94a8;font-size:9px}.kd-status-legend{display:grid;min-width:135px;flex:1;gap:12px}.kd-status-item{display:grid;grid-template-columns:9px minmax(0,1fr) auto;align-items:center;gap:7px;color:#66728a;font-size:10px}.kd-status-dot{width:8px;height:8px;border-radius:50%}.kd-status-item b{color:#35405a;font-size:11px}.kd-status-empty{grid-column:1/-1;padding:18px 4px;color:#8a94a8;font-size:12px;text-align:center}.kd-order-list{display:grid}.kd-order-columns,.kd-order-row{display:grid;grid-template-columns:minmax(125px,1.1fr) minmax(105px,.9fr) minmax(140px,1fr) minmax(max-content,.7fr);align-items:center;gap:10px}.kd-order-columns{padding:0 2px 9px;color:#8791a5;font-size:10px;font-weight:550}.kd-order-row{border-top:1px solid #edf0f5;padding:13px 2px;text-align:left}.kd-order-main{min-width:0;text-align:left}.kd-order-main a{display:block;overflow:hidden;color:#35405a;font-size:12px;font-weight:600;text-align:left;text-decoration:none;text-overflow:ellipsis;white-space:nowrap}.kd-order-main a:hover{color:#5367c9}.kd-order-main small,.kd-order-time{display:block;margin-top:5px;color:#929caf;font-size:10px;text-align:left}.kd-order-buyer{min-width:0;overflow:hidden;color:#59647c;font-size:11px;text-align:left;text-overflow:ellipsis;white-space:nowrap}.kd-status-badge{justify-self:start;border-radius:30px;padding:5px 9px;font-size:9px;font-weight:600;text-align:left;white-space:nowrap}.kd-status-pending{background:#fff5e5;color:#b47718}.kd-status-confirmed{background:#f0efff;color:#625bc2}.kd-status-shipping{background:#eaf5fb;color:#397da2}.kd-status-delivered{background:#eaf7ef;color:#37865d}.kd-status-cancelled{background:#f1f3f6;color:#788397}.kd-empty{padding:30px 10px;color:#929caf;font-size:12px;text-align:center}.kd-watch-list{display:grid}.kd-watch-item{display:flex;align-items:center;gap:12px;border-top:1px solid #edf0f5;padding:13px 2px;color:inherit;text-decoration:none;transition:background .15s}.kd-watch-item:hover{background:#fafbff}.kd-watch-icon{display:grid;width:36px;height:36px;flex:none;place-items:center;border-radius:12px}.kd-watch-icon.low{background:#fff5e5;color:#d49327}.kd-watch-icon.out{background:#fff0f0;color:#d35b62}.kd-watch-icon.pending{background:#f0efff;color:#6a65ce}.kd-watch-icon.review{background:#eaf5fb;color:#4c8db1}.kd-watch-copy{min-width:0;flex:1}.kd-watch-copy b{display:block;color:#3e4962;font-size:12px;font-weight:600}.kd-watch-copy small{display:block;margin-top:4px;color:#929caf;font-size:10px}.kd-watch-count{min-width:30px;color:#35405a;font-size:18px;font-weight:650;text-align:right}.kd-footer{padding:22px 0 0;color:#9aa3b3;font-size:10px;text-align:center}.kd-loading{padding:65px 12px;color:#8b95a8;text-align:center}.kd-skeleton{height:116px;border-radius:16px;background:linear-gradient(90deg,#f2f4f9 25%,#fafbfc 40%,#f2f4f9 65%);background-size:300% 100%;animation:kd-shimmer 1.4s ease infinite}@keyframes kd-shimmer{to{background-position:-150% 0}}@media(max-width:1100px){.kd-kpis{grid-template-columns:repeat(2,minmax(0,1fr))}.kd-chart-card{grid-column:span 7}.kd-status-card{grid-column:span 5}.kd-order-columns,.kd-order-row{grid-template-columns:minmax(0,1.1fr) minmax(0,.9fr) minmax(125px,1fr) minmax(max-content,.7fr);gap:7px}.kd-order-columns{font-size:9px}.kd-order-row{padding-block:12px}}@media(max-width:800px){.kd-heading{align-items:flex-start;flex-direction:column}.kd-period{justify-content:flex-start}.kd-chart-card,.kd-status-card,.kd-recent-card,.kd-watch-card{grid-column:span 12}.kd-donut-wrap{justify-content:flex-start}}@media(max-width:640px){.kd-order-columns{display:none}.kd-order-row{grid-template-columns:minmax(0,1fr) auto;align-items:start;gap:7px 10px;padding:12px 2px}.kd-order-main{grid-column:1;grid-row:1}.kd-order-buyer{grid-column:1;grid-row:2}.kd-order-time{grid-column:1;grid-row:3;margin:0}.kd-status-badge{grid-column:2;grid-row:1;justify-self:end}.kd-order-main a{white-space:normal;overflow-wrap:anywhere}}@media(max-width:520px){.kd-page{padding-block:18px 28px}.kd-title h1{font-size:22px}.kd-period{display:grid;width:100%;grid-template-columns:1fr 1fr}.kd-period label:first-child{grid-column:1/-1}.kd-period label{min-width:0}.kd-period input,.kd-period select{width:100%;min-width:0}.kd-period button{width:100%;justify-content:center}.kd-kpis{gap:9px}.kd-kpi{min-height:105px;padding:13px}.kd-kpi strong{font-size:17px}.kd-kpi-icon{width:34px;height:34px}.kd-card{padding:15px}.kd-chart-frame{height:235px}.kd-donut-wrap{flex-direction:column}.kd-donut{width:170px}.kd-status-legend{width:100%;grid-template-columns:1fr 1fr}.kd-order-row{grid-template-columns:minmax(0,1fr) auto;gap:7px}.kd-order-buyer{grid-column:1;grid-row:2}.kd-order-time{grid-column:1;grid-row:3}.kd-status-badge{grid-column:2;grid-row:1/4}.kd-card-head{align-items:center}}`;

const colors: Record<ShopOwnerDashboard['orderStatusBreakdown'][number]['status'], string> = {
  PENDING: '#6479d8', CONFIRMED: '#8a6bd5', SHIPPING: '#f0a52e', DELIVERED: '#42a477', CANCELLED: '#e16c75',
};
const statusLabels: Record<ShopOwnerDashboard['orderStatusBreakdown'][number]['status'], string> = {
  PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang vận chuyển', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy',
};
const dateToInput = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const shiftDate = (date: string, amount: number) => { const value = new Date(`${date}T12:00:00`); value.setDate(value.getDate() + amount); return dateToInput(value); };
const money = (value: number | string | null | undefined) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value ?? 0));
const compactMoney = (value: number) => new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(value);
const shortDate = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
const dateTime = (value: string) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
const periodDays = (from: string, to: string) => Math.round((new Date(`${to}T12:00:00`).getTime() - new Date(`${from}T12:00:00`).getTime()) / 86400000) + 1;

export default function SellerDashboardPage() {
  const today = useMemo(() => dateToInput(new Date()), []);
  const [shopName, setShopName] = useState('Cửa hàng');
  const [appliedRange, setAppliedRange] = useState({ from: shiftDate(dateToInput(new Date()), -6), to: dateToInput(new Date()) });
  const [draftRange, setDraftRange] = useState(appliedRange);
  const [period, setPeriod] = useState('7');
  const [stats, setStats] = useState<ShopOwnerDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rangeError, setRangeError] = useState('');

  useEffect(() => { void shopApi.getMyShop().then(shop => setShopName(shop.shopName)).catch(() => {}); }, []);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    void dashboardApi.get(appliedRange.from, appliedRange.to)
      .then(data => { if (active) setStats(data); })
      .catch(e => { if (active) setError(getApiError(e, 'Chưa tải được số liệu cửa hàng.')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [appliedRange]);

  const applyPreset = (value: string) => {
    setPeriod(value);
    if (value === 'custom') return;
    const days = Number(value);
    const range = { from: shiftDate(today, -(days - 1)), to: today };
    setDraftRange(range);
    setAppliedRange(range);
    setRangeError('');
  };
  const applyCustomRange = () => {
    if (!draftRange.from || !draftRange.to) { setRangeError('Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.'); return; }
    if (draftRange.from > draftRange.to) { setRangeError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.'); return; }
    if (draftRange.to > today) { setRangeError('Ngày kết thúc không thể ở tương lai.'); return; }
    if (periodDays(draftRange.from, draftRange.to) < 7) { setRangeError('Khoảng thời gian phải tối thiểu 7 ngày.'); return; }
    setRangeError('');
    setAppliedRange(draftRange);
  };

  const series = stats?.dailyPerformance ?? [];
  const statusData = stats?.orderStatusBreakdown ?? [];
  const statusTotal = statusData.reduce((sum, item) => sum + item.count, 0);
  const orderMax = Math.max(1, ...series.map(point => point.orderCount));
  const revenueMax = Math.max(1, ...series.map(point => Number(point.revenue)));
  const plot = { left: 54, right: 60, top: 20, bottom: 37, width: 960, height: 280 };
  const plotWidth = plot.width - plot.left - plot.right;
  const plotHeight = plot.height - plot.top - plot.bottom;
  const step = series.length > 1 ? plotWidth / (series.length - 1) : plotWidth;
  const points = series.map((point, index) => {
    const x = series.length > 1 ? plot.left + step * index : plot.left + plotWidth / 2;
    const revenueY = plot.top + plotHeight - Number(point.revenue) / revenueMax * plotHeight;
    const barHeight = point.orderCount / orderMax * plotHeight;
    return { ...point, x, revenueY, barHeight, barY: plot.top + plotHeight - barHeight };
  });
  const revenuePath = points.map(point => `${point === points[0] ? 'M' : 'L'} ${point.x} ${point.revenueY}`).join(' ');
  const kpis = [
    { label: 'Doanh thu đã giao', value: money(stats?.revenue), note: 'Trong khoảng thời gian chọn', icon: Wallet, tone: 'revenue' },
    { label: 'Đơn đặt trong kỳ', value: stats?.orderCount.toLocaleString('vi-VN') ?? '—', note: 'Tính theo thời điểm đặt hàng', icon: ShoppingBag, tone: 'orders' },
    { label: 'Giá trị đơn trung bình', value: money(stats?.averageOrderValue), note: 'Trên các đơn đã giao trong kỳ', icon: TrendingUp, tone: 'average' },
    { label: 'Đơn chờ xác nhận', value: stats?.pendingOrders.toLocaleString('vi-VN') ?? '—', note: 'Trong số đơn được đặt trong kỳ', icon: Clock3, tone: 'delivered' },
  ];

  return <><style>{styles}</style><div className="kd-page seller-container">
    <header className="kd-heading">
      <div className="kd-title"><h1>Chào mừng trở lại!</h1><p>Tổng quan hoạt động cửa hàng {shopName}.</p></div>
      <div className="kd-period" aria-label="Chọn thời gian thống kê">
        <label>Khoảng thời gian<select value={period} onChange={event => applyPreset(event.target.value)}><option value="7">7 ngày gần nhất</option><option value="30">30 ngày gần nhất</option><option value="90">90 ngày gần nhất</option><option value="custom">Tùy chọn ngày</option></select></label>
        <label>Từ ngày<input type="date" required max={draftRange.to || today} value={draftRange.from} onChange={event => { setPeriod('custom'); setDraftRange(current => ({ ...current, from: event.target.value })); }}/></label>
        <label>Đến ngày<input type="date" required min={draftRange.from} max={today} value={draftRange.to} onChange={event => { setPeriod('custom'); setDraftRange(current => ({ ...current, to: event.target.value })); }}/></label>
        <button type="button" disabled={loading} onClick={applyCustomRange}><CalendarDays size={15}/>Áp dụng</button>
      </div>
    </header>
    {rangeError && <p className="kd-range-error" role="alert">{rangeError}</p>}
    {error && <div className="kd-alert" role="alert">{error}</div>}

    <section className="kd-kpis" aria-label="Chỉ số kinh doanh trong kỳ">
      {kpis.map(item => <article className="kd-kpi" key={item.label}><div className="kd-kpi-copy"><small>{item.label}</small><strong>{loading ? 'Đang tải…' : item.value}</strong><em>{item.note}</em></div><span className={`kd-kpi-icon ${item.tone}`}><item.icon size={20}/></span></article>)}
    </section>

    {loading && !stats ? <div className="kd-skeleton" aria-label="Đang tải dashboard"/> : <div className="kd-dashboard-grid">
      <section className="kd-card kd-chart-card">
        <div className="kd-card-head"><div><h2>Hiệu quả kinh doanh</h2><p>{appliedRange.from} — {appliedRange.to}</p></div><span className="kd-card-link"><CalendarDays size={14}/>{periodDays(appliedRange.from, appliedRange.to)} ngày</span></div>
        <div className="kd-chart-legend"><span className="kd-legend-item"><i className="kd-legend-bar"/>Số đơn được đặt</span><span className="kd-legend-item"><i className="kd-legend-line"/>Doanh thu đơn đã giao</span></div>
        <div className="kd-chart-frame" role="img" aria-label="Biểu đồ cột số đơn được đặt mỗi ngày và đường doanh thu đơn đã giao mỗi ngày">
          <svg className="kd-chart-svg" viewBox={`0 0 ${plot.width} ${plot.height}`} preserveAspectRatio="none">
            <defs><linearGradient id="kd-order-bars" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#8493e3"/><stop offset="1" stopColor="#596dce"/></linearGradient></defs>
            {[0, 1, 2, 3].map(index => { const ratio = index / 3; const y = plot.top + plotHeight * ratio; const orderValue = Math.round(orderMax * (1 - ratio)); const revenueValue = revenueMax * (1 - ratio); return <g key={index}><line x1={plot.left} x2={plot.width - plot.right} y1={y} y2={y} stroke="#edf0f6" strokeDasharray={index === 3 ? undefined : '3 5'}/><text className="kd-chart-axis" x="2" y={y + 3}>{orderValue}</text><text className="kd-chart-axis" x={plot.width - plot.right + 7} y={y + 3}>{compactMoney(revenueValue)}</text></g>; })}
            <line x1={plot.left} x2={plot.width - plot.right} y1={plot.top + plotHeight} y2={plot.top + plotHeight} stroke="#dfe4ee"/>
            {points.map((point, index) => { const barWidth = Math.min(20, Math.max(3, (step || plotWidth) * .46)); const showTick = series.length <= 14 || index % Math.ceil(series.length / 10) === 0 || index === points.length - 1; return <g key={point.date}><rect x={point.x - barWidth / 2} y={point.barY} width={barWidth} height={Math.max(1.5, point.barHeight)} rx="4" fill="url(#kd-order-bars)"><title>{shortDate(point.date)}: {point.orderCount} đơn được đặt</title></rect>{showTick && <text className="kd-chart-tick" x={point.x} y={plot.height - 9} textAnchor="middle">{shortDate(point.date)}</text>}</g>; })}
            {points.length > 0 && <path d={revenuePath} fill="none" stroke="#fff" strokeWidth="7" strokeLinejoin="round" strokeLinecap="round"/>}
            {points.length > 0 && <path d={revenuePath} fill="none" stroke="#e89a32" strokeWidth="3.5" strokeLinejoin="round" strokeLinecap="round"/>}
            {points.map(point => <circle key={`rev-${point.date}`} cx={point.x} cy={point.revenueY} r={series.length <= 14 ? 4 : 2.7} fill="#fff" stroke="#e89a32" strokeWidth="2.5"><title>{shortDate(point.date)}: doanh thu {money(point.revenue)}</title></circle>)}
          </svg>
        </div>
        <p className="kd-chart-note">Trục trái là số đơn theo ngày đặt; trục phải là doanh thu (VND) của các đơn chuyển sang trạng thái đã giao trong ngày.</p>
      </section>

      <section className="kd-card kd-status-card">
        <div className="kd-card-head"><div><h2>Trạng thái đơn hàng</h2><p>{statusTotal.toLocaleString('vi-VN')} đơn được đặt trong kỳ</p></div><Link className="kd-card-link" to="/shop-owner/orders">Quản lý<ArrowRight size={14}/></Link></div>
        <div className="kd-donut-wrap">
          <svg className="kd-donut" viewBox="0 0 160 160" role="img" aria-label={`Tổng ${statusTotal} đơn hàng, phân bổ theo trạng thái`}>
            <circle cx="80" cy="80" r="57" fill="none" stroke="#eef1f7" strokeWidth="20"/>
            {statusTotal > 0 && (() => { let offset = 0; return statusData.map(item => { const segment = item.count / statusTotal * 100; const node = <circle key={item.status} cx="80" cy="80" r="57" fill="none" stroke={colors[item.status]} strokeWidth="20" strokeDasharray={`${segment} ${100 - segment}`} strokeDashoffset={-offset} pathLength="100" transform="rotate(-90 80 80)"><title>{statusLabels[item.status]}: {item.count} đơn</title></circle>; offset += segment; return node; }); })()}
            <text className="kd-donut-total" x="80" y="78" textAnchor="middle">{statusTotal}</text><text className="kd-donut-caption" x="80" y="96" textAnchor="middle">đơn hàng</text>
          </svg>
          <div className="kd-status-legend">{statusData.map(item => <div className="kd-status-item" key={item.status}><i className="kd-status-dot" style={{ background: colors[item.status] }}/><span>{statusLabels[item.status]}</span><b>{item.count}</b></div>)}</div>
        </div>
      </section>

      <section className="kd-card kd-recent-card">
        <div className="kd-card-head"><div><h2>Đơn hàng gần đây</h2><p>Mới nhất trên cửa hàng</p></div><Link className="kd-card-link" to="/shop-owner/orders">Xem tất cả<ArrowRight size={14}/></Link></div>
        {stats?.recentOrders.length ? <div className="kd-order-list"><div className="kd-order-columns" aria-hidden="true"><span>Mã đơn hàng</span><span>Người mua</span><span>Thời điểm đặt</span><span>Trạng thái</span></div>{stats.recentOrders.map(order => <article className="kd-order-row" key={order.id}>
          <div className="kd-order-main"><Link to={`/shop-owner/orders/${order.id}`}>{order.orderCode}</Link></div>
          <span className="kd-order-buyer">{order.customerName || 'Khách hàng'}</span>
          <time className="kd-order-time"><Clock3 size={12} style={{ display: 'inline', verticalAlign: '-2px', marginRight: 4 }}/>{dateTime(order.createdAt)}</time>
          <span className={`kd-status-badge kd-status-${order.status.toLowerCase()}`}>{statusLabels[order.status]}</span>
        </article>)}</div> : <div className="kd-empty">Chưa có đơn hàng gần đây.</div>}
      </section>

      <section className="kd-card kd-watch-card">
        <div className="kd-card-head"><div><h2>Cần theo dõi</h2><p>Các mục cần cửa hàng xử lý</p></div></div>
        <div className="kd-watch-list">
          <Link className="kd-watch-item" to="/seller/inventory"><span className="kd-watch-icon low"><AlertTriangle size={18}/></span><span className="kd-watch-copy"><b>Sắp hết hàng</b><small>Tồn kho dưới ngưỡng cảnh báo</small></span><strong className="kd-watch-count">{stats?.attention.lowStock ?? 0}</strong></Link>
          <Link className="kd-watch-item" to="/seller/inventory"><span className="kd-watch-icon out"><Package size={18}/></span><span className="kd-watch-copy"><b>Hết hàng</b><small>Sản phẩm đang bán không còn tồn khả dụng</small></span><strong className="kd-watch-count">{stats?.attention.outOfStock ?? 0}</strong></Link>
          <Link className="kd-watch-item" to="/seller/products"><span className="kd-watch-icon pending"><ClipboardList size={18}/></span><span className="kd-watch-copy"><b>Sản phẩm chờ duyệt</b><small>Đang chờ Admin xét duyệt</small></span><strong className="kd-watch-count">{stats?.attention.pendingProducts ?? 0}</strong></Link>
          <Link className="kd-watch-item" to="/shop-owner/reviews"><span className="kd-watch-icon review"><Star size={18}/></span><span className="kd-watch-copy"><b>Đánh giá chưa phản hồi</b><small>Chờ cửa hàng trả lời khách hàng</small></span><strong className="kd-watch-count">{stats?.attention.unansweredReviews ?? 0}</strong></Link>
        </div>
      </section>
    </div>}
    <footer className="kd-footer">Kala Store · Số liệu đơn hàng được giới hạn trong phạm vi cửa hàng của bạn.</footer>
  </div></>;
}
