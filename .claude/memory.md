# Quant Platform — Agent Memory
# Last Updated: 2026-06-02 (Phase 7 REST API 진행 중)

## 프로젝트 전환 요약

**이전 프로젝트**: Jurine (AI BUY/HOLD/SELL 시그널 서비스)
**현재 프로젝트**: Quant Platform (다전략 퀀트 분석 플랫폼)
**결정 사항**:
- 인프라 재사용: KIS API 연동, DART API, Layered DDD 아키텍처
- 도메인 로직 재설계: 전략 엔진 중심
- 프론트 추가: React (Vite) + TypeScript + Tailwind CSS - 프론트는 따로 진행
- AI 역할 축소: 선택적 보조 (Phase 8, 버튼 클릭 시만)

---

## 현재 상태

```yaml
phase: Phase 7 진행 중 (REST API 완료, React 프론트 미착수)
branch: feature/phase6
base_package: com.obigo.demodong
```

---

## 재사용 가능한 기존 인프라

| 컴포넌트 | 위치 | 재사용 여부 |
|---|---|---|
| KisTokenManager | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| KisKorStockPriceProvider | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| KisUsaStockPriceProvider | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| DartDisclosureProvider | domain/price/infrastructure/dart | ✅ 그대로 사용 |
| DartCorpCodeMapper | domain/price/infrastructure/dart | ✅ 그대로 사용 |
| StockPricePort | domain/price/domain/port | ✅ 그대로 사용 |
| CorporateDisclosurePort | domain/price/domain/port | ✅ 그대로 사용 |
| PriceSnapshot | domain/price/domain/entity | ✅ 그대로 사용 |
| Stock | domain/stock | ✅ 그대로 사용 |

---

## 핵심 설계 결정 (재설계 반영)

### 1. 전략을 데이터 의존도 기준으로 분산 배치
7개를 한 Phase에 몰지 않고, 데이터가 준비되는 시점에 바로 붙임.

| 전략 | 필요 데이터 | 구현 Phase |
|---|---|---|
| Mean Reversion | 기술적만 (RSI/BB/이격률) | Phase 2 |
| Minervini | 기술적만 (EMA 정배열, RS) | Phase 2 |
| Dual Momentum | 수익률 + 시장국면 | Phase 3 |
| Seasonality | 업종 계절성 테이블 (하드코딩) | Phase 3 |
| Piotroski F-Score | 재무만 (DART) | Phase 4 |
| CAN SLIM | 재무 + 기술 + 수급 | Phase 5 |
| Magic Formula | 유니버스 전체 상대 순위 | Phase 6 |

### 2. Magic Formula는 유니버스 배치로 처리
단일 종목 입력으로는 순위를 못 매김. Phase 6에서 배치 스케줄러로
사전 계산된 순위를 DB에 저장 → 조회 시 캐시 반환.

### 3. Seasonality는 사전 테이블 기반
과거 데이터 누적 없이도 동작하도록 업종별 계절성 점수를 
하드코딩 테이블로 관리. 나중에 실데이터로 교체 가능한 구조.

### 4. 미국 주식 범위 명확히 제한
- 기술적 지표 기반 전략만 지원 (Mean Reversion, Minervini, Dual Momentum, Seasonality)
- 재무 의존 전략 (Piotroski, CAN SLIM, Magic Formula)은 한국만
- 이유: DART = 한국 전용, Alpha Vantage 무료 = 분당 5회 제한

### 5. 프론트를 Phase 7에서 API와 동시 진행
Phase 2~6 결과물을 빠르게 확인하려면 API + 프론트를 한 Phase에서 묶는 게 현실적.

### 6. 각 Phase에 단위 테스트 의무화
금융 계산 버그는 틀린 숫자를 조용히 보여줌. Calculator 1개당 최소 5개 케이스.

---

## Phase 로드맵

### Phase 1 ✅ 완료 (인프라)
KIS API + DART API + Stock 엔티티

### Phase 2 ✅ 완료 (기술적 분석 엔진 + 전략 2개)
- `TechnicalSnapshot` record (17 필드)
- `RSI/MACD/Bollinger/EMA/SMA/ATR/AdxCalculator` (ADX는 null stub)
- `TechnicalAnalysisUseCase` (지표 계산 오케스트레이션)
- **MeanReversionCalculator** (RSI 40% + BB 40% + 이격률 20%)
- **MinerviniCalculator** (7조건 비례 계산, 조건 8은 Phase 3에서 활성화)
- `StrategyCalculator` 인터페이스, `StrategyInput/Score` record
- 단위 테스트 Calculator별 5개 이상 완료

### Phase 3 ✅ 완료 (모멘텀 + 시장 국면 + 전략 2개)
**신규 컴포넌트:**
- `MomentumSnapshot` record (return1m/3m/6m/12m, rsRating)
- `ReturnCalculator` (거래일 기준 기간별 수익률) — domain/technical
- `RsRatingCalculator` (초과수익률 ±20% → 0~100 정규화) — domain/technical
- `MarketRegimeService` (STRONG_BULL/BULL/SIDEWAYS/BEAR/CRISIS)
- `SeasonalityTable` (infrastructure, 업종별 월별 점수 하드코딩)
- **DualMomentumCalculator** (절대 50% + 상대 50%, 시장국면 보정)
- **SeasonalityCalculator** (SeasonalityTable 조회)

**수정:**
- `StrategyInput` — momentum, marketRegime, sector 필드 추가
- `MinerviniCalculator` — 조건 8 (rsRating ≥ 70) 정식 활성화

**구현 노트:**
- 설계 문서의 `StrategyAnalysisResult` 대신 `StrategyScore` record 사용
  (동일 구조: type/score/grade/detail/positives/negatives, neutralFactors는 미포함)

### Phase 4 ✅ 완료 (재무 분석 엔진 + 전략 1개, 한국 완전지원)
- DART 재무제표 파싱 (AccountCodeMapper — 계정과목 매핑 테이블)
- `FundamentalSnapshot` record (Phase 6에서 roic/earningsYield/operatingProfit 필드 추가)
- **PiotroskiCalculator** (9항목 × 11.1 = 0~100, 재무만 DART)
- 미국: FundamentalSnapshot.stub() 반환
- DartFundamentalAdapter + KisFundamentalAdapter + FundamentalDataAdapter

### Phase 5 ✅ 완료 (수급 분석 + 전략 1개)
- KIS 투자자별 매매동향 API 연동 (`KisInvestorFlowAdapter`)
- `FlowSnapshot` record (institutionalNetBuy/foreignerNetBuy/volumeChangeRate 등)
- **CANSLIMCalculator** (7항목 가중 평균: C/A/N/S/L/I/M, 재무+기술+수급 통합)
- 미국: FlowSnapshot.stub() → S/I 항목 50점 중립

### Phase 6 ✅ 완료 (Magic Formula — 유니버스 배치)
**구현 완료 컴포넌트:**
- `MagicFormulaUniverse` 엔티티 (분석 대상 종목 관리, active 플래그)
- `MagicFormulaRank` 엔티티 (일자별 roicRank/eyRank/combinedRank 저장)
- `MagicFormulaUniverseRepository`, `MagicFormulaRankRepository`
- `MagicFormulaBatchService` (@Scheduled 매 거래일 18시, @PostConstruct 대형주 10종목 초기화)
- **MagicFormulaCalculator** (DB 순위 조회 → percentile 변환 [2,universeSize×2] → [100,0]점)
- `MagicFormulaUniverseController` (관리자 API: universe CRUD + 수동 배치 실행 + 순위표 조회)
- `MagicFormulaCalculatorTest` (5개 시나리오: 최상위/중간/최하위/미국중립/순위없음중립)

**FundamentalSnapshot 보완 (Phase 6 이슈):**
- `operatingProfit` (DART 영업이익 원화 절대값)
- `roic` (DART: 영업이익 / (총자산 - 유동부채) × 100)
- `earningsYield` (영업이익 / (KIS시가총액 × 1,000,000) — merge 시 계산)
- `AccountCodeMapper` — `operatingProfit` 계정과목 추가 (영업이익/영업손익 등 4종)

**7개 전략 전부 완성 확인:**
```
MEAN_REVERSION ✅  MINERVINI ✅  DUAL_MOMENTUM ✅  SEASONALITY ✅
PIOTROSKI ✅  CANSLIM ✅  MAGIC_FORMULA ✅
```

### Phase 7 — REST API + React 프론트 (동시) ← 진행 중

**REST API 완료 (commit: 846ffa6):**
- `AnalysisController` — 3개 엔드포인트
  - `GET /api/analysis/{ticker}` → 7전략 + 기술/재무/수급 통합
  - `GET /api/stocks/search?q=` → 종목 검색
  - `GET /api/market/regime` → 시장 국면 반환
- `AnalysisUseCase` — 7전략 병렬 계산 오케스트레이터
  - Stock 해결(없으면 생성), 200일 가격 로드, 기술/재무/수급 조합
  - `CompletableFuture` 병렬 Calculator 실행
- `StockSearchUseCase` — 주식 검색
- Response DTOs:
  - `AnalysisResponse` (통합), `StrategyScoreResponse` (점수+근거)
  - `TechnicalResponse`, `FundamentalResponse`, `FlowResponse`
  - `StockSearchResponse`
- `AnalysisResponseCode` — API 응답 코드
- `src/main/resources/static/api-tester.html` — 대화형 API 테스터 UI (다크 테마)

**미착수:**
- React(Vite) 프론트엔드 (`/frontend` 디렉토리 없음)
- 포트폴리오/watchlist API (api-tester.html에 섹션은 있음)

### Phase 8 — AI 선택적 해석
- POST 요청 시만 Groq 호출
- ai_report 테이블 캐싱 (ticker + date, TTL 24h)
- 7개 전략 점수 기반 종합 해석 생성

### Phase 9 — 비교 + 필터링
- 최대 3개 종목 비교
- 전략 점수 기반 필터링 스크리닝

---

## 현재 환경

```yaml
db: PostgreSQL (prod)
ai: Groq API (llama-3.1-8b-instant) — Phase 8에서만 사용
port: 8090 (backend) / 5173 (frontend dev)
news_kr: Naver News API
news_us: Yahoo Finance RSS
stock_price: KIS API (KOR + USA)
corporate_disclosure: DART API (KOR 전용)
```

## ⚠️ Agent 실행 제약

### Gradle/테스트 실행 금지
DB = PostgreSQL (prod). 테스트/빌드 실행 시 DB 터널 필수.
Agent가 직접 `./gradlew test` 또는 `./gradlew bootRun` 실행 금지.
필요 시 사용자에게 명령:

```
[사용자 실행 필요]
1. scripts/tunnel.sh 실행 (SSH → RDS 터널 오픈)
2. ./gradlew test 실행
```

터널 스크립트: `scripts/tunnel.sh`
env 필요: EC2_HOST, EC2_USER, PEM_KEY (또는 기본값 사용)

---

## 버그 수정 이력

### 2026-05-28
- **currentPrice 실시간화**: `AnalysisUseCase.analyze()`에서 `fetchCurrentPrice()` 호출 추가.
  `TechnicalResponse.from()`에 `realTimePrice` 파라미터 추가, null 시 마지막 종가 fallback.
- **"0001" USA 오분류 수정**: `resolveStock()`의 MarketType 판별 `\d{6}` → `\d+` (4자리 KOSPI 지수 등 포함).
  영향 파일: `AnalysisUseCase.java`, `TechnicalResponse.java`

## 주요 원칙

- KIS API: 실전/모의 전환은 application.yml kis.base-url로만
- DART API: dart_corp_code는 Stock 테이블 저장, KOR만
- Soft Delete: BaseEntity.softDelete() 항상 사용
- 전략 점수: 0~100 정규화, null → 50점 중립 (패널티 없음)
- API 기본경로: /api
- CORS: 개발환경 localhost:5173 허용
- 미국 주식: 기술적 전략만 지원 (Phase 4 이후 재무 의존 전략 제외)

---

## 2026-05-26 추가 결정사항

### 신규 기능 3개 추가 확정

| 기능 | Phase | 핵심 결정 |
|---|---|---|
| Strategy Explanation | Phase 2부터 내재화 | StrategyScore → StrategyAnalysisResult |
| Backtest Engine | Phase 3.5 | 기술적 전략 완성 직후, 비동기 실행 |
| Meta Score | Phase 7.5 | 7개 전략 완성 후, 동적 가중치 적용 |

### StrategyAnalysisResult — 핵심 변경
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

// StrategyCalculator interface
StrategyAnalysisResult calculate(StrategyInput input);
```
→ 이 타입은 Phase 2 첫 Calculator부터 적용. 나중에 바꾸면 전체 재설계.

### 전체 Phase (최종)
```
Phase 1:   인프라 ✅
Phase 2:   기술적 분석 + Mean Reversion + Minervini ✅
Phase 3:   모멘텀 + Dual Momentum + Seasonality ✅
Phase 3.5: Backtest Engine
Phase 4:   재무 분석 + Piotroski ✅
Phase 5:   수급 분석 + CAN SLIM ✅
Phase 6:   Magic Formula (유니버스 배치) ✅
Phase 7:   REST API + React 프론트 ← 다음 작업
Phase 7.5: Meta Score + Strategy Explanation UI
Phase 8:   AI 선택적 해석
Phase 9:   비교 + 필터링
```

### Backtest 제약
- 기술적 전략만 우선 (DART 과거 재무 데이터 없음)
- PriceSnapshot 200일치 이상 있는 종목만
- 비동기 실행 (@Async) + DB 저장 + polling

### Meta Score 가중치
- 기본: Technical 25% / Momentum 20% / Fundamental 35% / Growth 15% / Seasonal 5%
- BULL: Momentum+5%, Growth+5%, Fundamental-10%
- BEAR: Fundamental+10%, MeanReversion+5%, Momentum-15%
