import CategorySection from '../../components/customer/home/category/CategorySection';
import HeroBanner from '../../components/customer/home/hero/HeroBanner';
import RecommendationSection from '../../components/customer/home/recommendation/RecommendationSection';

const HomePage = () => {
  return (
    <main className="min-h-screen bg-[#F7FAFF]">
      <div className="mx-auto w-full max-w-[1200px] px-4">
        <HeroBanner />

        <CategorySection />
          
        <RecommendationSection />
      </div>
    </main>
  );
};

export default HomePage;