import { ImageWithFallback } from "../components/figma/ImageWithFallback";
import { TrendingUp, Flame } from "lucide-react";

interface WatchlistItem {
  id: string;
  name: string;
  recentTrades: number[];
  avg7day: number;
}

interface CostEffectItem {
  id: string;
  name: string;
  damageIncrease: number;
  avg7day: number;
  costEfficiency: number;
}

export function MainPage() {
  // 유저 설정: 화속성 덱
  const userElement = "fire";

  const watchlist: WatchlistItem[] = [
    { id: '1', name: '5북두 풀지국', recentTrades: [680, 690, 675, 670, 685], avg7day: 682 },
    { id: '2', name: '3북두 풀지국반쌍', recentTrades: [390, 395, 385, 388, 392], avg7day: 390 },
    { id: '3', name: '풀지국', recentTrades: [115, 118, 112, 116, 115], avg7day: 115 },
    { id: '4', name: '5천추 변지국', recentTrades: [88, 90, 87, 89, 88], avg7day: 88 },
    { id: '5', name: '갑투지국', recentTrades: [32, 33, 31, 32, 32], avg7day: 32 },
  ];

  const costEffItems: CostEffectItem[] = [
    { id: '1', name: '화염의 검', damageIncrease: 450, avg7day: 85, costEfficiency: 5.3 },
    { id: '2', name: '불사조 갑옷', damageIncrease: 380, avg7day: 120, costEfficiency: 3.2 },
    { id: '3', name: '열화 투구', damageIncrease: 280, avg7day: 65, costEfficiency: 4.3 },
    { id: '4', name: '염제의 반지', damageIncrease: 520, avg7day: 180, costEfficiency: 2.9 },
  ];

  return (
    <div className="max-w-[1400px] mx-auto px-6 py-8">
      {/* 속성 배너 */}
      <div className="mb-8 bg-gradient-to-r from-[#D94F3D]/20 to-transparent border border-[#D94F3D]/30 rounded p-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-[#D94F3D]/10 rounded-full blur-3xl"></div>
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-3">
            <Flame className="w-8 h-8 text-[#D94F3D]" />
            <h1 className="font-serif text-3xl font-bold text-[#3E3A36]">화속성 가성비 루트</h1>
          </div>
          <p className="text-[#7A7368] text-sm">당신의 화속성 덱에 최적화된 아이템을 추천합니다</p>
        </div>
      </div>

      {/* 가성비 루트 이미지 2장 */}
      <div className="grid grid-cols-2 gap-6 mb-10">
        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <div className="aspect-video bg-gradient-to-br from-[#D94F3D]/20 to-[#8B6B4A]/10 relative">
            <ImageWithFallback
              src="https://images.unsplash.com/photo-1677916042908-ef67437da720?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhbmNpZW50JTIwYXNpYW4lMjB3YXJyaW9yJTIwZXF1aXBtZW50JTIwYXJtb3J8ZW58MXx8fHwxNzc5MTczNTMyfDA&ixlib=rb-4.1.0&q=80&w=1080"
              alt="화속성 갑옷 루트"
              className="w-full h-full object-cover"
            />
            <div className="absolute top-3 right-3 bg-[#D94F3D] text-white px-3 py-1 rounded text-xs font-semibold">
              추천 루트 1
            </div>
          </div>
          <div className="p-4">
            <h3 className="font-serif text-lg font-semibold text-[#3E3A36] mb-2">화염 방어구 세트</h3>
            <p className="text-sm text-[#7A7368] mb-3">풀지국 + 화염 강화 주술 조합</p>
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#7A7368]">예상 투자금</span>
              <span className="font-serif text-lg font-bold text-[#8B6B4A]">850억 전</span>
            </div>
          </div>
        </div>

        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <div className="aspect-video bg-gradient-to-br from-[#D94F3D]/20 to-[#8B6B4A]/10 relative">
            <ImageWithFallback
              src="https://images.unsplash.com/photo-1440711085503-89d8ec455791?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtZWRpZXZhbCUyMHdlYXBvbiUyMHN3b3JkfGVufDF8fHx8MTc3OTE3MzUzMnww&ixlib=rb-4.1.0&q=80&w=1080"
              alt="화속성 무기 루트"
              className="w-full h-full object-cover"
            />
            <div className="absolute top-3 right-3 bg-[#D94F3D] text-white px-3 py-1 rounded text-xs font-semibold">
              추천 루트 2
            </div>
          </div>
          <div className="p-4">
            <h3 className="font-serif text-lg font-semibold text-[#3E3A36] mb-2">화염 무기 세트</h3>
            <p className="text-sm text-[#7A7368] mb-3">고급 화속성 무기 + 반지 조합</p>
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#7A7368]">예상 투자금</span>
              <span className="font-serif text-lg font-bold text-[#8B6B4A]">420억 전</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 관심목록 최근거래 */}
        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-5">
          <h2 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-[#8B6B4A]" />
            관심목록 최근 거래
          </h2>
          <div className="space-y-4">
            {watchlist.map((item) => (
              <div key={item.id} className="border-b border-[#DDD6CA] pb-4 last:border-b-0 last:pb-0">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-medium text-[#3E3A36]">{item.name}</h3>
                  <span className="text-xs text-[#7A7368]">7일 평균</span>
                </div>
                <div className="flex items-center gap-2 mb-2">
                  {item.recentTrades.map((price, idx) => (
                    <div
                      key={idx}
                      className="flex-1 bg-[#F5F1E8] border border-[#DDD6CA] rounded px-2 py-1 text-center"
                    >
                      <div className="text-[10px] text-[#7A7368] mb-0.5">거래 {idx + 1}</div>
                      <div className="text-sm font-semibold text-[#8B6B4A]">{price}억</div>
                    </div>
                  ))}
                </div>
                <div className="flex items-center justify-between bg-[#E8DCCB]/30 rounded px-3 py-2">
                  <span className="text-xs text-[#7A7368]">평균가</span>
                  <span className="font-serif text-base font-bold text-[#8B6B4A]">{item.avg7day}억 전</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 가성비 지표 */}
        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-[#D94F3D]/30 to-[#8B6B4A]/20 border border-[#D94F3D]/40 flex items-center justify-center">
              <ImageWithFallback
                src="https://images.unsplash.com/photo-1577493340887-b7bfff550145?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwbW9uc3RlciUyMGRyYWdvbnxlbnwxfHx8fDE3NzkxNzM1MzJ8MA&ixlib=rb-4.1.0&q=80&w=1080"
                alt="몬스터"
                className="w-full h-full object-cover rounded-lg"
              />
            </div>
            <div>
              <h2 className="font-serif text-xl font-semibold text-[#3E3A36]">가성비 지표</h2>
              <p className="text-sm text-[#7A7368]">대상 몬스터: 화염 드래곤</p>
            </div>
          </div>

          <div className="space-y-3">
            <div className="bg-[#F5F1E8] border border-[#DDD6CA] rounded p-3">
              <div className="text-[10px] text-[#7A7368] uppercase tracking-wider mb-2">관심 아이템 리스트</div>
              {costEffItems.map((item) => (
                <div key={item.id} className="flex items-center justify-between py-2 border-b border-[#DDD6CA] last:border-b-0">
                  <div className="flex-1">
                    <div className="text-sm font-medium text-[#3E3A36] mb-0.5">{item.name}</div>
                    <div className="flex items-center gap-3 text-xs text-[#7A7368]">
                      <span>데미지 +{item.damageIncrease}</span>
                      <span>평균 {item.avg7day}억</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-[#7A7368] mb-0.5">가성비</div>
                    <div className={`text-base font-bold ${item.costEfficiency >= 4 ? 'text-[#4A6B3A]' : 'text-[#8B6B4A]'}`}>
                      {item.costEfficiency.toFixed(1)}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="bg-[#EBF2E8] border border-[#A8C49A] rounded p-3">
              <div className="text-xs text-[#4A6B3A] font-medium mb-1">💡 추천</div>
              <div className="text-sm text-[#3E3A36]">
                <span className="font-semibold">화염의 검</span>과 <span className="font-semibold">열화 투구</span>가 가장 높은 가성비를 보이고 있습니다.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
