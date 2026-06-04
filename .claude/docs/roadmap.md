# Quant Platform — 전체 로드맵
# Last Updated: 2026-06-02

## 설계 원칙

전략을 Phase 4 하나에 몰지 않고, **데이터가 준비되는 시점**에 바로 붙인다.
각 Phase는 독립적으로 동작 가능한 결과물을 만든다.
테스트는 선택이 아니라 각 Phase의 완료 조건이다.

---

## 프로젝트 수준 목표

현재 프로젝트를:

```text
"전략 점수 계산 서비스"
```

수준에서,

```text
"검증 가능한 설명형 퀀트 플랫폼"
```

수준으로 확장하기 위한 핵심 기능 3개를 추가한다.

### 추가 핵심 기능

| 기능                   | 목적           | 구현 Phase     |
| -------------------- | ------------ | ------------ |
| Strategy Explanation | 점수 계산 근거 설명  | Phase 2부터 내재화 |
| Backtest Engine      | 전략의 실제 성과 검증 | Phase 3.5    |
| Meta Score           | 전략 종합 평가     | Phase 7.5    |

### 전체 구조 변경

**기존 흐름:**
```text
주가 데이터 → 전략 계산 → 점수 출력
```

**변경 후:**
```text
주가 데이터 → 전략 계산 → 설명 생성 → 전략 통합 → Meta Score 계산 → 백테스트 검증 → UI 출력
```

### Phase 구조 (최종)

```text
Phase 1    인프라 정비                          ✅ 완료
Phase 2    기술적 분석 + Mean Reversion + Minervini  ✅ 완료
Phase 3    모멘텀 + Dual Momentum + Seasonality     ✅ 완료
Phase 3.5  Backtest Engine                     ← 신규 추가
Phase 4    재무 분석 + Piotroski               ✅ 완료
Phase 5    수급 분석 + CAN SLIM               ✅ 완료
Phase 6    Magic Formula (유니버스 배치)      ✅ 완료 — 7개 전략 전부 완성
Phase 7    REST API + React 프론트             🔄 진행 중 (REST API ✅, 프론트 미착수)
Phase 7.5  Meta Score + Strategy Explanation UI  ← 신규 추가
Phase 8    AI 선택적 해석
Phase 9    비교 + 필터링
```

---

## 전략별 데이터 의존도 & 구현 Phase

| 전략 | 필요 데이터 | 구현 Phase | 지원 시장 |
|---|---|---|---|
| Mean Reversion | 기술적만 (RSI/BB/이격률) | **Phase 2** | KOR + USA |
| Minervini | 기술적만 (EMA 정배열 8조건) | **Phase 2** | KOR + USA |
| Dual Momentum | 기간별 수익률 + 시장국면 | **Phase 3** | KOR + USA |
| Seasonality | 업종 계절성 테이블 | **Phase 3** | KOR + USA |
| Piotroski F-Score | 재무만 (DART) | **Phase 4** | KOR 전용 |
| CAN SLIM | 재무 + 기술 + 수급 | **Phase 5** | KOR 전용 |
| Magic Formula | 유니버스 전체 상대 순위 | **Phase 6** | KOR 전용 |

---

## Phase 1 — 인프라 정비 ✅ 완료

기존 KIS + DART 인프라 재사용 확정.

**재사용 확정**
- `KisTokenManager`, `KisKorStockPriceProvider`, `KisUsaStockPriceProvider`
- `DartDisclosureProvider`, `DartCorpCodeMapper`
- `StockPricePort`, `CorporateDisclosurePort`
- `PriceSnapshot` 엔티티, `Stock` 엔티티

**제거/보류**
- 기존 SSE 기반 BUY/HOLD/SELL 시그널 플로우
- `QuantSignal`, `QuantFeatureSnapshot` (새 전략 엔진으로 대체)
- `SignalFeedback`, `AlphaEvaluationScheduler`
- `BriefingScheduler` (Telegram 모닝 브리핑)

---

## Phase 2 — 기술적 분석 엔진 + 전략 2개 ✅ 완료

**목표**: 주가 시계열 → 기술적 지표 계산 + Mean Reversion / Minervini 전략

### 새 도메인 구조
```
domain/technical/
├── domain/
│   ├── model/TechnicalSnapshot.java
│   └── calculator/
│       ├── TechnicalCalculator.java    (interface)
│       ├── RsiCalculator.java
│       ├── MacdCalculator.java
│       ├── BollingerBandCalculator.java
│       ├── EmaCalculator.java
│       ├── SmaCalculator.java
│       ├── AtrCalculator.java
│       └── AdxCalculator.java
└── application/
    └── usecase/TechnicalAnalysisUseCase.java

domain/analysis/
└── domain/calculator/
    ├── StrategyCalculator.java         (interface)
    ├── MeanReversionCalculator.java    ← Phase 2에서 구현
    └── MinerviniCalculator.java        ← Phase 2에서 구현
```

### TechnicalSnapshot
```java
record TechnicalSnapshot(
    BigDecimal rsi14,
    BigDecimal macd, macdSignal, macdHistogram,
    BigDecimal bollingerUpper, bollingerMid, bollingerLower,
    Map<Integer,BigDecimal> emaMap,  // 20/50/150/200
    BigDecimal atr14, adx14,
    BigDecimal sma20, sma60,
    BigDecimal currentPrice, high52w, low52w,
    BigDecimal deviationFromSma20
)
```

### MeanReversionCalculator 점수 로직
```
RSI 점수    (40%): RSI<30=100 / 30~40=75 / 40~60=50 / 60~70=25 / >70=0
Bollinger   (40%): 현재가가 하단밴드 근처=100, 상단밴드 근처=0, 선형 보간
이격률       (20%): 음수(SMA 아래) → 높은 점수, ±10%p 범위 정규화
```

### Minervini 8조건 (조건 충족 수 × 12.5)
```
1. 현재가 > EMA150 > EMA200
2. EMA200 최근 1개월 우상향
3. EMA50 > EMA150 > EMA200
4. 현재가 > EMA50
5. 현재가 > EMA20
6. 현재가 ≥ 52주 저점 × 1.30
7. 현재가 ≥ 52주 고점 × 0.75
8. RS Rating ≥ 70 (없으면 나머지 7개로 비례 계산)
```

### Strategy Explanation 내재화 (Phase 2부터 적용)

Phase 2의 Calculator 반환 타입을 기존 `StrategyScore`에서 설명 가능한 구조로 확장.
모든 Calculator는 점수와 함께 판단 근거를 반환한다.

```java
// 모든 StrategyCalculator의 반환 타입
record StrategyAnalysisResult(
    StrategyType type,
    int score,
    String grade,
    Map<String, Integer> detail,
    List<String> positives,
    List<String> negatives,
    List<String> neutralFactors
)
```

**Minervini 예시:**
```text
Minervini 88점 (S)

강점:
✔ EMA150 > EMA200 정배열
✔ 현재가 > EMA50
✔ 52주 고점 근접

약점:
✘ 거래량 증가 부족
```

**Mean Reversion 예시:**
```text
Mean Reversion 72점

강점:
✔ RSI 28 (과매도)
✔ 볼린저 하단 접근

약점:
✘ 하락 추세 지속
```

### 테스트 완료 조건
- [x] `RsiCalculatorTest` — 정상값 3 + 경계값(전부 상승/전부 하락/변동없음) 3
- [x] `MacdCalculatorTest` — 크로스오버 시나리오 포함 5개
- [x] `BollingerBandCalculatorTest` — 5개
- [x] `EmaCalculatorTest` — 5개
- [x] `MeanReversionCalculatorTest` — 과매도/과매수/중립 시나리오 5개
- [x] `MinerviniCalculatorTest` — 조건 0개/4개/8개 충족 시나리오

---

## Phase 3 — 모멘텀 + 시장 국면 + 전략 2개 ✅ 완료

**목표**: 기간별 수익률 계산 + 시장 국면 판단 + Dual Momentum / Seasonality

### 새 컴포넌트
```
domain/analysis/
└── domain/
    ├── service/MarketRegimeService.java
    │   └── enum MarketRegime (STRONG_BULL/BULL/SIDEWAYS/BEAR/CRISIS)
    └── calculator/
        ├── DualMomentumCalculator.java
        └── SeasonalityCalculator.java

domain/analysis/infrastructure/
└── SeasonalityTable.java   // 업종별 월별 계절성 점수 테이블 (하드코딩)
```

### 시장 국면 판단 기준
```
STRONG_BULL : 지수 5일 수익률 > +3%
BULL        : 5d > 0%, 20d > 0%
SIDEWAYS    : -1% < 5d < +1%
BEAR        : 5d < -1% or 20d < -2%
CRISIS      : 5d < -5%
```

### Dual Momentum
```
절대 모멘텀 (50%): 기간별 수익률(1/3/6/12개월 평균) > 0 → 100점
상대 모멘텀 (50%): 시장 대비 초과수익률 → 상위 기준 0~100 정규화
```

### Seasonality — 하드코딩 테이블 전략
```java
// SeasonalityTable.java
// 실데이터(과거 5년 월별 수익률) 누적 없이 동작하는 초기 구현
// 실제 통계 기반 데이터로 교체 가능한 구조
Map<String, Map<Month, Integer>> SECTOR_MONTHLY_SCORE = Map.of(
    "반도체", Map.of(JANUARY, 65, FEBRUARY, 70, MARCH, 55, ...),
    "바이오", Map.of(JANUARY, 50, FEBRUARY, 60, ...),
    ...
);
// 섹터 매핑 안 되면 시장 평균 테이블 사용
```

### Phase 3 수정 사항
- `StrategyInput` — momentum, marketRegime, sector 필드 추가
- `MinerviniCalculator` — 조건 8 (rsRating ≥ 70) 정식 활성화

---

## Phase 3.5 — Backtest Engine ← 신규 추가

**목표**: 전략 점수 시스템을 "좋아 보이는 점수"가 아닌 "실제로 성과가 있었는지 검증 가능한 전략"으로 발전

### Backtest란?

과거 데이터를 기반으로 "이 전략대로 투자했다면?" 을 시뮬레이션하는 기능.

**예시:**
```text
2020~2025 동안
Minervini 점수 80 이상 종목을
매월 리밸런싱하며 매수
→ 실제 수익률 계산
```

### 핵심 설계 원칙

1. **실제 투자 흐름 재현**: 매수 → 보유 → 매도 → 리밸런싱 흐름 전체 시뮬레이션
2. **전략 독립 구조**: Mean Reversion, Minervini, CAN SLIM, Meta Score 등 모든 전략 재사용 가능
3. **기존 Calculator 재사용**: 실서비스 전략 계산 = 백테스트 전략 계산 (동일 유지)
4. **미래 데이터 사용 금지**: 투자 당시 시점에서 알 수 있었던 데이터만 사용

### 새 도메인 구조
```
domain/backtest/
├── domain/
│   ├── model/
│   │   ├── BacktestRequest.java
│   │   ├── BacktestResult.java
│   │   ├── PortfolioSnapshot.java
│   │   ├── Position.java
│   │   └── TradeHistory.java
│   │
│   ├── service/
│   │   ├── BacktestEngine.java
│   │   ├── PortfolioSimulator.java
│   │   ├── RebalanceService.java
│   │   ├── TradeExecutionService.java
│   │   └── PerformanceMetricService.java
│   │
│   └── calculator/
│       ├── CAGRCalculator.java
│       ├── MddCalculator.java
│       ├── SharpeRatioCalculator.java
│       └── WinRateCalculator.java
│
└── infrastructure/
    ├── HistoricalPriceRepository.java
    └── BacktestResultRepository.java
```

### BacktestRequest
```java
record BacktestRequest(
    String strategyType,
    LocalDate startDate,
    LocalDate endDate,

    int topN,
    RebalancePeriod rebalancePeriod,

    BigDecimal initialCapital,
    boolean includeCash
)
```

### BacktestResult
```java
record BacktestResult(
    BigDecimal totalReturn,
    BigDecimal cagr,
    BigDecimal maxDrawdown,
    BigDecimal sharpeRatio,
    double winRate,

    List<PortfolioSnapshot> portfolioHistory,
    List<TradeHistory> trades
)
```

### 핵심 성과 지표

| 지표           | 의미       |
| ------------ | -------- |
| Total Return | 총 수익률    |
| CAGR         | 연평균 수익률  |
| MDD          | 최대 손실폭   |
| Sharpe Ratio | 위험 대비 수익 |
| Win Rate     | 승률       |

### 리밸런싱
```java
enum RebalancePeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
```

### 거래 비용 반영
```java
commissionRate = 0.00015
slippageRate = 0.001
```

### Benchmark 비교
```text
내 전략: +148%
KOSPI:   +42%
S&P500:  +61%
```

### 반드시 필요한 히스토리 저장
```text
technical_snapshot_history
strategy_score_history
daily_price_history
```

### Backtest 제약
- 기술적 전략만 우선 (DART 과거 재무 데이터 없음)
- PriceSnapshot 200일치 이상 있는 종목만
- 비동기 실행 (@Async) + DB 저장 + polling

### 추가 API
```text
POST /api/backtest/run
GET  /api/backtest/{id}
GET  /api/backtest/{id}/trades
```

### 프론트 UI
```text
전략 선택
기간 선택
리밸런싱 선택
상위 N개 설정

[ 실행 ]

→ 결과:
- 누적 수익률 그래프
- CAGR
- MDD
- Sharpe Ratio
- 거래 내역
```

---

## Phase 4 — 재무 분석 엔진 + Piotroski ✅ 완료

**목표**: DART 재무제표 파싱 + Piotroski F-Score 전략 (한국 전용)

### DART 파싱의 현실적 어려움과 해결 전략
```
문제: XBRL 계정과목 코드가 회사마다 다름
해결: AccountCodeMapper — 계정 이름별 코드 우선순위 리스트 관리

우선순위 예시 (영업이익):
  1. ifrs-full_ProfitLossFromOperatingActivities
  2. dart_OperatingIncomeLoss
  3. 직접 명칭 매핑 ("영업이익", "영업손익")

파싱 실패 시: 해당 필드 null → FundamentalSnapshot에 null 포함
  → Calculator에서 null 항목은 50점 중립 처리
```

### 새 도메인 구조
```
domain/fundamental/
├── domain/
│   ├── model/FundamentalSnapshot.java
│   └── port/FundamentalDataPort.java
└── infrastructure/
    ├── DartFundamentalAdapter.java
    │   └── AccountCodeMapper.java   // 계정과목 매핑 테이블
    └── KisFundamentalAdapter.java   // PER/PBR/EPS (KIS inquire-price)
```

### FundamentalSnapshot
```java
record FundamentalSnapshot(
    BigDecimal per, pbr, peg,
    BigDecimal roe, roa, roic,
    BigDecimal debtRatio, currentRatio,
    BigDecimal operatingMargin, netMargin,
    BigDecimal epsGrowthYoy, epsGrowthQoq,
    BigDecimal revenueGrowthYoy,
    BigDecimal earningsYield,
    BigDecimal marketCap, eps
) {
    static FundamentalSnapshot stub() {
        return new FundamentalSnapshot(null, null, ...);  // 미국 주식용
    }
}
```

### Piotroski 9항목 (각 1점, 합산 → × 11.1 = 0~100)
```
수익성 (3점)
  F1: ROA > 0
  F2: 영업현금흐름 > 0
  F3: 전년 대비 ROA 개선

레버리지/유동성 (3점)
  F4: 부채비율 감소
  F5: 유동비율 개선
  F6: 신주 미발행 (희석 없음)

효율성 (3점)
  F7: 매출총이익률 개선
  F8: 자산회전율 개선
  F9: CFO/자산 > ROA (발생주의 vs 현금 일치)
```

### 미국 주식 처리
```
FundamentalDataPort.fetch(ticker, USA) → FundamentalSnapshot.stub() 반환
Piotroski/CAN SLIM Calculator → fundamental.isStub() 체크 → 50점 중립 반환
API 응답에 "재무 분석은 한국 상장 종목만 지원합니다" 필드 포함
```

---

## Phase 5 — 수급 분석 + CAN SLIM ✅ 완료

**목표**: 기관/외국인 매매동향 수집 + CAN SLIM 전략 (한국 전용)

### 새 도메인 구조
```
domain/flow/
├── domain/
│   ├── model/FlowSnapshot.java
│   └── port/InvestorFlowPort.java
└── infrastructure/
    └── KisInvestorFlowAdapter.java
        // KIS /uapi/domestic-stock/v1/quotations/inquire-investor
```

### FlowSnapshot
```java
record FlowSnapshot(
    long institutionalNetBuy5d,
    long institutionalNetBuy20d,
    long foreignNetBuy5d,
    long foreignNetBuy20d,
    double volumeChangeRate,   // 5일 평균 대비
    long tradingValue,
    double relativeStrength
) {
    static FlowSnapshot stub() { ... }   // 미국 주식용
}
```

### CAN SLIM 7항목
```
C (Current Earnings)   : 전분기 EPS 성장률 ≥25% = 100, 선형
A (Annual Earnings)    : 연간 EPS 성장률 ≥25% = 100, 선형
N (New High)           : 52주 신고가 여부 → 0 or 100
S (Supply/Demand)      : 거래량 증가율 + 기관/외국인 순매수 합산
L (Leader)             : RS Rating 기반 섹터 내 상위 → 0~100
I (Institutional)      : 기관 5일 연속 순매수 → 0 or 100
M (Market Direction)   : BULL=100, SIDEWAYS=50, BEAR=0
최종 점수 = 7개 항목 단순 평균
```

---

## Phase 6 — Magic Formula (유니버스 배치) ✅ 완료

**목표**: 전체 종목 상대 순위 기반 Magic Formula (한국 전용) — **이 Phase로 7개 전략 전부 완성**

### 단일 종목 계산이 불가능한 이유
Magic Formula의 핵심은 "유니버스 안에서 ROIC 순위와 Earnings Yield 순위를 합산"하는 것.
종목 하나만 입력받으면 순위를 알 수 없음.

### 구현 완료 컴포넌트
```
domain/analysis/
├── domain/
│   ├── entity/
│   │   ├── MagicFormulaUniverse.java    ← 유니버스 종목 관리 (active 플래그)
│   │   └── MagicFormulaRank.java        ← 일자별 roicRank/eyRank/combinedRank 저장
│   ├── repository/
│   │   ├── MagicFormulaUniverseRepository.java
│   │   └── MagicFormulaRankRepository.java
│   ├── calculator/
│   │   └── MagicFormulaCalculator.java  ← percentile 변환 [2,N×2]→[100,0]
│   └── service/
│       └── MagicFormulaBatchService.java  ← @Scheduled 18시 / @PostConstruct 초기화
└── presentation/
    └── MagicFormulaUniverseController.java  ← 관리자 API

domain/fundamental/ (Phase 6 보완)
├── domain/model/FundamentalSnapshot.java  ← roic/earningsYield/operatingProfit 추가
└── infrastructure/
    ├── AccountCodeMapper.java    ← operatingProfit 계정과목 추가
    ├── DartFundamentalAdapter.java  ← ROIC 계산 로직 추가
    └── FundamentalDataAdapter.java  ← Earnings Yield 병합 계산
```

### 순위 계산 로직
```
ROIC 내림차순 정렬 → roicRank 1, 2, 3 ...
EarningsYield 내림차순 정렬 → eyRank 1, 2, 3 ...
combinedRank = roicRank + eyRank (낮을수록 우수)
점수 변환: score = (maxRank - combinedRank) / (maxRank - minRank) × 100
  where maxRank = universeSize × 2, minRank = 2
null 종목 → 최하위 순위로 처리
```

### FundamentalSnapshot 보완 내용
```
roic          = 영업이익 / (총자산 - 유동부채) × 100  (DART 계산)
earningsYield = 영업이익 / (시가총액억원 × 1,000,000)  (KIS 병합 계산)
operatingProfit = DART 영업이익 원화 절대값
AccountCodeMapper ← operatingProfit 계정과목 우선순위 4종 추가
```

### 유니버스 관리
```
초기: @PostConstruct 시 한국 대형주 10종목 자동 주입 (삼성전자, SK하이닉스 등)
확장: POST /api/admin/magic-formula/universe 로 추가 가능
스케줄: @Scheduled(cron="0 0 18 * * MON-FRI") 매 거래일 18시 자동 연산
```

### 관리자 API
```
GET    /api/admin/magic-formula/universe          # 유니버스 목록
POST   /api/admin/magic-formula/universe          # 종목 추가
DELETE /api/admin/magic-formula/universe/{ticker} # 종목 비활성화
POST   /api/admin/magic-formula/run               # 수동 배치 실행
GET    /api/admin/magic-formula/ranks?date=       # 특정일 순위표
```

### 단위 테스트 (MagicFormulaCalculatorTest)
```
1. 최상위 순위 (combinedRank=2, universeSize=100) → 100점 S등급
2. 중간 순위  (combinedRank=100, universeSize=100) → 51점 B등급
3. 최하위 순위 (combinedRank=200, universeSize=100) → 0점 D등급
4. 미국 주식 (MarketType.USA) → 50점 중립
5. 순위 데이터 없음 → 50점 중립
```

---

## Phase 7 — REST API + React 프론트 (동시 진행) 🔄 진행 중

**목표**: API 완성 + UI 구축을 한 Phase에서 같이 진행

### 이유
Phase 2~6 동안 결과물이 보이지 않으면 검증이 어렵고 동기부여도 낮아짐.
API와 프론트를 같이 만들면서 바로 시각적으로 확인.

### Spring Boot API — ✅ 구현 완료

**구현된 엔드포인트 (commit: 846ffa6):**
```
GET  /api/analysis/{ticker}               ✅ 7전략 + 기술 + 재무 + 수급 통합
GET  /api/stocks/search?q=keyword         ✅ 종목 검색
GET  /api/market/regime                   ✅ 시장 국면 반환
```

**미구현 (계획):**
```
GET  /api/analysis/{ticker}/strategies    # 전략 점수 목록만
GET  /api/analysis/{ticker}/technical     # 기술적 지표만
GET  /api/analysis/{ticker}/fundamental   # 재무 지표만
GET  /api/analysis/{ticker}/flow          # 수급 분석만
POST /api/analysis/{ticker}/ai-report     # AI 해석 (Phase 8)
GET  /api/stocks/compare?tickers=A,B,C   # 비교 (Phase 9)
```

**핵심 컴포넌트:**
- `AnalysisController` — 프레젠테이션 레이어
- `AnalysisUseCase` — CompletableFuture 병렬 7전략 오케스트레이션
- `StockSearchUseCase` — 종목 검색
- Response DTOs: `AnalysisResponse`, `StrategyScoreResponse`, `TechnicalResponse`, `FundamentalResponse`, `FlowResponse`, `StockSearchResponse`
- `src/main/resources/static/api-tester.html` — 대화형 테스트 UI

### 캐싱 전략
```
기술적 지표 : 장 종료(15:30) 후 갱신, 익일 장 시작 전까지 캐시
재무 지표   : 분기 1회 (DART 공시 배치 트리거)
수급 데이터 : 장 중 1시간 간격
AI 리포트   : ticker + date 기준 24h TTL
```

### React 프론트 구조
```
frontend/
├── src/
│   ├── pages/
│   │   ├── HomePage.tsx          # 종목 검색 + 전략 필터링
│   │   ├── AnalysisPage.tsx      # 종목 분석 메인
│   │   └── ComparePage.tsx       # 종목 비교
│   ├── components/
│   │   ├── layout/Header.tsx
│   │   ├── analysis/
│   │   │   ├── StockHeader.tsx
│   │   │   ├── PriceChart.tsx
│   │   │   ├── StrategyScoreGrid.tsx
│   │   │   ├── StrategyScoreCard.tsx   ← 핵심 컴포넌트
│   │   │   ├── TechnicalSection.tsx
│   │   │   ├── FundamentalSection.tsx
│   │   │   ├── FlowSection.tsx
│   │   │   └── AiReportPanel.tsx
│   │   └── common/
│   │       ├── ScoreGauge.tsx
│   │       └── GradeChip.tsx
│   └── api/
│       └── analysisApi.ts        ← 모든 API 호출 여기서만
├── vite.config.ts                # proxy: /api → localhost:8090
└── tailwind.config.js
```

### AnalysisPage 레이아웃
```
┌──────────────────────────────────────────────┐
│  삼성전자 (005930)  ·  KOSPI                  │
│  62,800원  ▲ +1.2% (+740원)                  │
├──────────────────────────────────────────────┤
│  [주가 차트 — 6개월 라인 + EMA20/50/200]       │
├──────────────────────────────────────────────┤
│  전략 점수                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │Mean Rev. │ │Minervini │ │Dual Mom. │      │
│  │  72점  A │ │  88점  S │ │  61점  A │      │
│  └──────────┘ └──────────┘ └──────────┘      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │Seasonal  │ │Piotroski │ │CAN SLIM  │      │
│  │  55점  B │ │  78점  A │ │  69점  A │      │
│  └──────────┘ └──────────┘ └──────────┘      │
│  ┌──────────┐                                │
│  │Magic F.  │                                │
│  │  82점  S │                                │
│  └──────────┘                                │
├──────────────────────────────────────────────┤
│  기술적 지표 (RSI / MACD / Bollinger)          │
├──────────────────────────────────────────────┤
│  재무 지표  [한국 전용 배지]                    │
├──────────────────────────────────────────────┤
│  수급 분석  [한국 전용 배지]                    │
├──────────────────────────────────────────────┤
│  [🤖 AI 종합 해석 요청]  → 결과 패널           │
├──────────────────────────────────────────────┤
│  최근 공시 / 뉴스                              │
└──────────────────────────────────────────────┘
```

---

## Phase 7.5 — Meta Score + Strategy Explanation UI ← 신규 추가

**목표**: 여러 전략 결과를 하나의 종합 점수로 통합 + 설명 UI

### 왜 필요한가

현재 전략들은 서로 충돌 가능.

| 전략             | 점수 |
| -------------- | -- |
| Mean Reversion | 80 |
| Momentum       | 20 |

사용자는 "그래서 좋은 종목인가?" 를 궁금해함.

### 전략 카테고리

| 카테고리        | 전략                        |
| ----------- | ------------------------- |
| Technical   | Mean Reversion, Minervini |
| Momentum    | Dual Momentum             |
| Fundamental | Piotroski, Magic Formula  |
| Growth      | CAN SLIM                  |
| Seasonal    | Seasonality               |

### Meta Score 계산
```text
MetaScore =
  Technical   25%
+ Momentum    20%
+ Fundamental 35%
+ Growth      15%
+ Seasonal     5%
```

### 시장 국면별 동적 가중치

**Bull Market:**
```text
Momentum ↑ (+5%)
Growth ↑ (+5%)
Fundamental ↓ (-10%)
```

**Bear Market:**
```text
Fundamental ↑ (+10%)
Mean Reversion ↑ (+5%)
Momentum ↓ (-15%)
```

### 새 도메인 구조
```
domain/meta/
├── MetaScoreCalculator.java
├── StrategyWeightPolicy.java
└── MarketAdaptiveWeightPolicy.java
```

### MetaAnalysisResult
```java
record MetaAnalysisResult(
    int metaScore,
    String grade,

    String investmentStyle,

    List<String> strongestStrategies,
    List<String> weakestStrategies
)
```

### 투자 스타일 자동 분류

| 조건                | 스타일     |
| ----------------- | ------- |
| Momentum 높음       | 성장 모멘텀형 |
| Fundamental 높음    | 가치주형    |
| Mean Reversion 높음 | 반등형     |

### Strategy Explanation UI

**프론트 UI 개선:**
```text
Minervini 88점 (S)

✔ EMA 정배열
✔ EMA200 상승중
✔ 52주 고점 근접

✘ 거래량 부족
```

**Meta Score 프론트 UI:**
```text
종합 점수: 82 (S)

투자 스타일:
성장 모멘텀 우위형

강한 전략:
✔ Minervini
✔ CAN SLIM
✔ Dual Momentum

약한 전략:
✘ Mean Reversion
```

### Strategy Explanation 장점

| 장점        | 설명            |
| --------- | ------------- |
| 사용자 신뢰 상승 | 왜 점수가 나왔는지 설명 |
| AI 품질 향상  | 구조화된 근거 전달 가능 |
| 유지보수 향상   | 문자열 하드코딩 제거   |
| 다국어 대응    | 프론트 매핑 가능     |

### AI 리포트 품질 개선

**기존:**
```json
{
  "score": 72
}
```

**변경 후:**
```json
{
  "score": 72,
  "positives": [
    "RSI 과매도",
    "볼린저 하단 접근"
  ],
  "negatives": [
    "거래량 감소"
  ]
}
```

### API 추가
```text
GET /api/analysis/{ticker}/meta
```

---

## Phase 8 — AI 선택적 해석

**목표**: 7개 전략 점수 → AI 종합 해석 (선택적, 캐싱)

### 설계 원칙
- 필수 분석 경로에 AI 없음 (Rule 기반만)
- 사용자 명시적 요청(POST) 시만 Groq 호출
- 캐싱: `ai_report` 테이블 (ticker + date = unique)

### Groq 프롬프트 전략
```
시스템: 퀀트 애널리스트 역할, 데이터 기반 사실 요약
유저 입력:
  {
    "ticker": "005930",
    "strategies": [
      {"type": "CANSLIM", "score": 69, "grade": "A", "top3_positives": [...], "top2_negatives": [...]},
      ...
    ],
    "technicalSummary": "RSI 58 (중립), MACD 상승 크로스, 52주 고점 대비 -12%",
    "fundamentalSummary": "PER 12.4, ROE 15.2%, 부채비율 32%"
  }
출력 형식:
  강점: ...
  약점: ...
  종합: ...
  ※ 이 분석은 투자 권유가 아닙니다.
최대 토큰: 500
```

---

## Phase 9 — 종목 비교 + 전략 필터링

**목표**: 다종목 비교 및 조건 기반 스크리닝

### 비교 기능
```
GET /api/stocks/compare?tickers=005930,035720,000660
→ 3개 종목 7개 전략 점수 나란히 반환
→ 프론트: 레이더 차트 or 나란히 카드
```

### 필터링
```
GET /api/stocks/search?minervini_score=70&piotroski_score=7&roe=15
→ 조건에 맞는 종목 목록 반환
```

---

## 핵심 변화 요약

| 기능          | 프로젝트에 생기는 변화  |
| ----------- | ------------- |
| Backtest    | 전략 검증 가능      |
| Explanation | 설명 가능한 분석     |
| Meta Score  | 사용자 친화적 종합 평가 |

이 3개가 추가되면 프로젝트의 수준이 "주식 분석 서비스"에서 **"검증 가능한 설명형 퀀트 플랫폼"**으로 격상된다.

---

## 주요 결정사항 기록

| 날짜 | 결정 | 이유 |
|---|---|---|
| 2026-05-26 | 전략을 데이터 의존도 기준으로 Phase 분산 | Phase 4 병목 방지 |
| 2026-05-26 | Magic Formula → 유니버스 배치 구조 | 단일 종목으론 상대 순위 계산 불가 |
| 2026-05-26 | Seasonality → 하드코딩 테이블 초기 구현 | 과거 데이터 누적 필요 없이 즉시 동작 |
| 2026-05-26 | 미국 주식 재무/수급 → Stub 처리 | Alpha Vantage 무료 한계, DART 한국 전용 |
| 2026-05-26 | Phase 7에서 API + 프론트 동시 진행 | 조기 시각적 검증, 동기부여 |
| 2026-05-26 | Calculator별 단위 테스트 의무화 | 금융 계산 버그는 조용히 틀린 숫자 출력 |
| 2026-05-26 | Strategy Explanation Phase 2부터 내재화 | 나중에 바꾸면 전체 Calculator 재설계 필요 |
| 2026-05-26 | Backtest Engine Phase 3.5 추가 | 기술적 전략 완성 직후 검증 가능 |
| 2026-05-26 | Meta Score Phase 7.5 추가 | 7개 전략 완성 후 종합 평가 제공 |
