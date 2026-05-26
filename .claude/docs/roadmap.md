# Quant Platform — 전체 로드맵
# Last Updated: 2026-05-26

## 설계 원칙

전략을 Phase 4 하나에 몰지 않고, **데이터가 준비되는 시점**에 바로 붙인다.
각 Phase는 독립적으로 동작 가능한 결과물을 만든다.
테스트는 선택이 아니라 각 Phase의 완료 조건이다.

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

## Phase 2 — 기술적 분석 엔진 + 전략 2개

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

### 테스트 완료 조건
- [ ] `RsiCalculatorTest` — 정상값 3 + 경계값(전부 상승/전부 하락/변동없음) 3
- [ ] `MacdCalculatorTest` — 크로스오버 시나리오 포함 5개
- [ ] `BollingerBandCalculatorTest` — 5개
- [ ] `EmaCalculatorTest` — 5개
- [ ] `MeanReversionCalculatorTest` — 과매도/과매수/중립 시나리오 5개
- [ ] `MinerviniCalculatorTest` — 조건 0개/4개/8개 충족 시나리오

---

## Phase 3 — 모멘텀 + 시장 국면 + 전략 2개

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

---

## Phase 4 — 재무 분석 엔진 + Piotroski

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

## Phase 5 — 수급 분석 + CAN SLIM

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

## Phase 6 — Magic Formula (유니버스 배치)

**목표**: 전체 종목 상대 순위 기반 Magic Formula (한국 전용)

### 단일 종목 계산이 불가능한 이유
Magic Formula의 핵심은 "유니버스 안에서 ROIC 순위와 Earnings Yield 순위를 합산"하는 것.
종목 하나만 입력받으면 순위를 알 수 없음.

### 유니버스 배치 구조
```
domain/analysis/
└── infrastructure/
    ├── MagicFormulaUniverse.java  (엔티티 — 분석 대상 종목 관리)
    ├── MagicFormulaRank.java      (엔티티 — 계산된 순위 저장)
    └── MagicFormulaBatchService.java
        // @Scheduled(cron="0 0 18 * * MON-FRI") 매 거래일 18시
        // 전체 유니버스 ROIC + EarningsYield 계산 → 순위 정렬 → DB 저장

MagicFormulaCalculator:
    // DB에서 해당 ticker 순위 조회 → 점수 반환
    // 순위 없으면 → 50점 중립
```

### 유니버스 관리
```
초기: KOR 대형주 100~200종목 (@PostConstruct 초기화)
확장: 관리자 API로 추가/제거 가능
```

---

## Phase 7 — REST API + React 프론트 (동시 진행)

**목표**: API 완성 + UI 구축을 한 Phase에서 같이 진행

### 이유
Phase 2~6 동안 결과물이 보이지 않으면 검증이 어렵고 동기부여도 낮아짐.
API와 프론트를 같이 만들면서 바로 시각적으로 확인.

### Spring Boot API
```
GET  /api/analysis/{ticker}               # 7전략 + 기술 + 재무 + 수급 통합
GET  /api/analysis/{ticker}/strategies    # 전략 점수 목록
GET  /api/analysis/{ticker}/technical     # 기술적 지표
GET  /api/analysis/{ticker}/fundamental   # 재무 지표
GET  /api/analysis/{ticker}/flow          # 수급 분석
POST /api/analysis/{ticker}/ai-report     # AI 해석
GET  /api/stocks/search?q=keyword
GET  /api/stocks/compare?tickers=A,B,C
GET  /api/market/regime
```

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

## 주요 결정사항 기록

| 날짜 | 결정 | 이유 |
|---|---|---|
| 2026-05-26 | 전략을 데이터 의존도 기준으로 Phase 분산 | Phase 4 병목 방지 |
| 2026-05-26 | Magic Formula → 유니버스 배치 구조 | 단일 종목으론 상대 순위 계산 불가 |
| 2026-05-26 | Seasonality → 하드코딩 테이블 초기 구현 | 과거 데이터 누적 필요 없이 즉시 동작 |
| 2026-05-26 | 미국 주식 재무/수급 → Stub 처리 | Alpha Vantage 무료 한계, DART 한국 전용 |
| 2026-05-26 | Phase 7에서 API + 프론트 동시 진행 | 조기 시각적 검증, 동기부여 |
| 2026-05-26 | Calculator별 단위 테스트 의무화 | 금융 계산 버그는 조용히 틀린 숫자 출력 |
