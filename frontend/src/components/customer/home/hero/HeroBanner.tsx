import { useEffect, useState } from 'react';
import axiosClient from '../../../../services/api/axiosClient';
import { mediaUrl } from '../../../../services/api/mediaUrl';

type Banner = { id: string; title: string; imageUrl: string; priority: number };
type BannerEnvelope = { data: Banner[] };

const HeroBanner = () => {
  const [banners, setBanners] = useState<Banner[]>([]);
  const [active, setActive] = useState(0);

  useEffect(() => {
    let current = true;
    void axiosClient.get<BannerEnvelope>('/banners')
      .then(({ data }) => {
        if (current) setBanners((data.data || []).filter((banner) => banner.imageUrl));
      })
      .catch(() => {
        if (current) setBanners([]);
      });
    return () => { current = false; };
  }, []);

  useEffect(() => {
    if (banners.length < 2) return;
    const timer = window.setInterval(() => setActive((index) => (index + 1) % banners.length), 6000);
    return () => window.clearInterval(timer);
  }, [banners.length]);

  if (!banners.length) return null;
  const banner = banners[active % banners.length];

  return (
    <section className="pt-5 sm:pt-6" aria-label="Banner khuyến mãi">
      <div className="relative aspect-[2/1] overflow-hidden rounded-2xl bg-[#f3f5fa] sm:aspect-[3/1]">
        <img
          key={banner.id}
          src={mediaUrl(banner.imageUrl)}
          alt=""
          className="customer-banner-transition absolute inset-0 h-full w-full object-cover"
        />
      </div>
    </section>
  );
};

export default HeroBanner;
