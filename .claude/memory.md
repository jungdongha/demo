# Jurine — Agent Memory
# Last Updated: 2026-05-21

## 현재 상태

phase: Phase 9.5 구현 완료 (Quant Feature Engine 고도화 — 3개 신규 Feature)
branch: feature/quant-logic
base_package: com.obigo.demodong

## 완료된 도메인

| 도메인 | 위치 | 상태 |
|---|---|---|
| signal | domain/signal | 구현 중 (SSE 분석, 히스토리, 스케줄러) |
| portfolio | domain/portfolio | 구현 중 (보유/관심 종목 CRUD) |
| price | domain/price | 구현 중 (주가 스냅샷) |
| stock | domain/stock | 구현 중 (종목 엔티티) |
| telegram | domain/telegram | 구현 중 (봇 알림) |

## 주요 변경 이력

- 2026-05-21: **Phase 9.5 Quant Feature Engine 고도화** (branch: feature/quant-logic)
  - 3개 신규 Feature Calculator 추가 (총 7개 Feature)
  - [신규] `QuantFundamentalData` record — PER/PBR/목표주가/현재가 (nullable 허용, `stub()` 팩토리)
  - [신규] `QuantFundamentalPort` 인터페이스 — DIP 적용, KOR/USA 분기 표준화
  - [신규] `KisFundamentalAdapter` — KIS `/uapi/domestic-stock/v1/quotations/inquire-price` 연동 (per, pbr, stck_prpr)
    - USA: `QuantFundamentalData.stub(BigDecimal.ZERO)` 반환 (Phase 10에서 Alpha Vantage 연동 예정)
  - [신규] `ValuationScoreCalculator` — PER·PBR 저평가도 합산 점수 (가중치 20%)
    - PER ≤10→100점, 10~30→선형, ≥30→0점; PBR ≤1→100점, 1~3→선형, ≥3→0점; 적자(PER=null)→50점 중립
  - [신규] `TargetPriceUpsideCalculator` — 증권사 목표주가 대비 상승 여력 (가중치 15%)
    - targetPrice=null(Stub) → 50점 중립; 30% 상승여력 = 100점 만점; 음수 = 0점
  - [신규] `SectorRelativeStrengthCalculator` — 섹터 평균 대비 종목 상대 강도 (가중치 15%)
    - selfMomentum - sectorAvgMomentum5d → ±10%p 범위 정규화; 데이터 부족 시 50점 중립
  - [수정] `QuantFeatureInput` — 2개 필드 추가: `QuantFundamentalData fundamentals`, `double sectorAvgMomentum5d`
  - [수정] `FeatureType` enum — 3개 값 추가: `VALUATION_SCORE`, `TARGET_PRICE_UPSIDE`, `SECTOR_RELATIVE_STRENGTH`
  - [수정] `QuantScore` record — 4개 필드 추가: `valuationScore`, `targetPriceUpsideScore`, `sectorRelativeScore`, `fundamentals`
  - [수정] `QuantScoringService` — 가중치 재조정: VOLUME 35%→20%, MOMENTUM 30%→20%, NEWS 20%→10%, +VALUATION 20%, +TARGET 15%, +SECTOR 15%
    - 총 가중치 합 0.85 → 1.00; featureRawValues `Map.of()` → `new HashMap<>()` (7개 항목); missing feature 기본값 0.0 → 50.0
  - [수정] `QuantFeatureSnapshot` — 7개 컬럼 추가: `perValue`, `pbrValue`, `valuationScore`, `targetPrice`, `targetPriceUpsidePct`, `targetPriceScore`, `sectorRelativeScore`; `create()` 시그니처 단순화 → `create(QuantSignal, QuantScore, String)`
  - [수정] `QuantEngineUseCase` — 2-pass 배치 구조: Pass1(데이터 수집) → 섹터 평균 사전 집계 → Pass2(Score 계산); `QuantFundamentalPort` 주입; LLM 리포트 프롬프트에 PER/PBR/목표주가/섹터강도 추가
  - [수정] `QuantFeatureSnapshotResponse` DTO — 7개 신규 필드 추가
  - [수정] `QuantScoringServiceTest` — 8→10개 케이스 확장; 밸류에이션/섹터 강도 테스트 추가
  - [신규] `ValuationScoreCalculatorTest` — Happy Path 3케이스 + Edge Cases 3케이스
  - [신규] `TargetPriceUpsideCalculatorTest` — Happy Path 3케이스 + Edge Cases 4케이스
  - [신규] `SectorRelativeStrengthCalculatorTest` — Happy Path 3케이스 + Edge Cases 3케이스
  - 빌드: BUILD SUCCESSFUL, 전체 퀀트 테스트 통과

- 2026-05-15: **Phase 9 Quant Signal Engine MVP 구현** (branch: feature/phase9)
  - [신규] `QuantUniverse` 엔티티 — 분석 대상 유니버스 (quant_universe 테이블, KOR+USA TOP20)
  - [신규] `QuantSignal` 엔티티 — TOP3 시그널 결과 (quant_signal 테이블)
  - [신규] `QuantFeatureSnapshot` 엔티티 — Feature 값 스냅샷 (quant_feature_snapshot 테이블)
  - [신규] `QuantFeatureCalculator` 인터페이스 (Strategy Pattern) + 4개 MVP 구현체
    - `VolumeRatio5dCalculator` — 거래량 배수 (5일 평균 대비, 가중치 35%)
    - `PriceMomentum5dCalculator` — 5일 가격 모멘텀 % (가중치 30%)
    - `NewsFreshnessCalculator` — 뉴스 신선도 점수 0~100 (가중치 20%)
    - `MarketRegimeScoreCalculator` — 시장 국면 보정 계수 -10~+10
  - [신규] `MarketRegimeService` — 지수 5일 변동률 기반 국면 판단 (STRONG_BULL/BULL/SIDEWAYS/BEAR/CRISIS)
  - [신규] `QuantScoringService` — 가중치 합산 → 0~100점 Quant Score 산출 + TOP3 선별
  - [신규] `RiskFilterService` — 유동성/급등선반영/하락장 3개 필터
  - [신규] `QuantDataPort` + `KisQuantDataAdapter` — KOR 일별 시세 + KOSPI 지수 조회 (KisTokenManager 재활용)
  - [신규] `QuantNewsPort` + `QuantNewsCrawlerAdapter` — 기존 NewsCrawlerStrategy Wrapper (Module Guard 준수)
  - [신규] `QuantEngineUseCase` — 배치 실행 (Feature 수집 → Score → Filter → TOP3 → LLM 리포트 → 저장)
  - [신규] `QuantSignalUseCase` — 조회 Facade (5개 API)
  - [신규] `QuantController` — `/api/quant/top-signals`, `/scores`, `/scores/{ticker}`, `/universe`, `/regime`
  - [신규] `QuantSignalScheduler` — KOR 09:10 / USA 22:30 일배치
  - [신규] `QuantUniverseInitializer` — @PostConstruct KOR10+USA10 초기 데이터 삽입
  - [신규] `QuantErrorCode` (405xx), `QuantResponseCode`
  - [수정] `application.yaml` — `alpha-vantage` 설정 추가 (MVP: Stub, Phase 10에서 실제 연동)
  - 패키지: Core 패턴 준수 — `domain.quant.*` (quant_signal.yaml의 분리 구조 대신 일관성 우선)
  - 테스트: `QuantScoringServiceTest` 8개 케이스 (Score 계산, Risk Filter, TOP3 선별) — 8/8 PASS

- 2026-05-15: **Phase 8 피드백 루프 구현** (Phase 6 설계 보류, Phase 7 RAG 스킵)
  - [신규] `SignalFeedback` 엔티티 — T+3/T+10/T+20 Alpha 추적 테이블 (signal_feedback)
  - [신규] `SignalFeedbackRepository` — 피드백 조회/통계 쿼리
  - [신규] `SignalFeedbackReader` / `SignalFeedbackWriter` — 도메인 서비스
  - [신규] `AlphaEvaluationScheduler` — 매 거래일 18:00 T+3/T+10/T+20 자동 평가
    - PriceSnapshot DB에서 실제 영업일 기반 주가 조회
    - RAW 수익률 계산 (벤치마크 미차감 — Phase 9+에서 확장)
    - is_failure 판정: BUY alpha10d < -5%, SELL alpha10d > +5%
  - [신규] `FeedbackStatsUseCase` — 카테고리별 Alpha 통계
  - [신규] `FeedbackController` — `/api/feedback/stats`, `/api/feedback/recent`, `/api/feedback/{reportId}`
  - [수정] `SignalReport` — `expectedReasonCategory VARCHAR(50)` 필드 추가
  - [수정] `SignalReportRepository` — 피드백 미평가 시그널 조회 JPQL 쿼리 3개 추가
  - [수정] `SignalReportReader` — `findSignalsWithoutFeedback()`, `findSignalsNeedingT10()`, `findSignalsNeedingT20()` 추가
  - [수정] `StockAnalysisUseCase` — `inferReasonCategory()` 키워드 기반 카테고리 자동 추론 + saveReport() 반영

- 2026-05-14: **Jurine Quant Signal Engine 로드맵 추가**
  - `docs/roadmap.md`: Phase 9~11 (Quant Signal Engine) 추가 (기존 Phase 5~8 유지)
    - Phase 9: Feature Engineering + Scoring Engine MVP
    - Phase 10: Market Regime Analysis + RAG 통합 (Phase 7 인프라 재활용)
    - Phase 11: Alpha Tracking + Feedback Loop + Self-Correction
  - `references/data-model/quant/quant_signal.yaml`: Quant 전용 도메인 모델 신규 생성
    - QuantSignal, QuantFeatureSnapshot, QuantAlphaResult, QuantUniverse
  - `core/system-design.yaml`: Quant 모듈 아키텍처 추가
    - Core vs Quant 분리 원칙, Cross-Module 의존 금지, shared infra 개념
    - /api/quant/** 엔드포인트 6개, 에러코드 405xx
  - `.claude/CLAUDE.md`: Quant 참조 파일 인덱스 추가, Module Guard 프로토콜 추가

- 2026-05-14: **버그 수정** — `parseReason` 헤더 탐색 불일치 수정
  - [수정] `StockAnalysisUseCase.parseReason()` — `"판단 근거"` → `"핵심 요약"` (프롬프트 2차 고도화 포맷 반영)

- 2026-05-13: **AI 프롬프트 2차 고도화** (퀀트 애널리스트 관점 보완)
  - [수정] `PriceSnapshot` — `volume` 컬럼 추가 (nullable)
  - [수정] `KisKorStockPriceProvider` — `acml_vol` 파싱, `fetch52WeekRange()` (w52_hgpr/w52_lwpr)
  - [수정] `KisUsaStockPriceProvider` — `tvol` 파싱 추가
  - [수정] `StockPricePort` — `fetch52WeekRange()` 메서드 추가
  - [수정] `KisStockPriceRouter` — KOR 52주 데이터 라우팅, USA Optional.empty()
  - [수정] `PriceAnalysisHelper` — 거래량 5일평균 대비 증감, 52주 고/저 위치, 이격률 과열 경고 레이블
  - [수정] `stock-analysis-system.st` — CB/BW 목적 구분, 수주 임팩트 기준(매출 10%), 역발상/이격률/거래량/52주 체크리스트, 확신 지수 1~10점
  - [수정] `stock-analysis-user.st` — CoT 각 단계에 구체적 조건 명시, 데이터 모순 탐지 지시

- 2026-05-13: **AI 프롬프트 1차 고도화** (branch: feature/phase5)
  - [신규] `PriceAnalysisHelper` (domain/price/domain/service/) — MA20, 추세, 변동성, 5일 모멘텀 계산 후 AI에 주입
  - [수정] `stock-analysis-system.st` — 분석 철학, 데이터 신뢰도 우선순위, 공시 해석기준(호재/악재/중립), 섹터별 분석 포인트 추가
  - [수정] `stock-analysis-user.st` — CoT 4단계 지시(공시→뉴스→주가→종합 판정) 추가
  - [수정] `StockAnalysisUseCase` — `fetchPriceData` → `fetchPriceContext` 교체 (PriceAnalysisHelper 사용), 뉴스/공시 빈값 fallback 메시지 개선

- 2026-05-12: **기획 전면 전환** — 단순 뉴스 기반 → KIS+DART 전문 데이터 통합 MVP로 재정의
  - **개인 프로젝트 결정**: User 엔티티 / 다중 사용자 / 개인화 기능 불필요. 단일 사용자 구조 유지.
  - [업데이트] `.claude/docs/roadmap.md` 전면 재작성 (Phase 5~8)
  - [업데이트] `.claude/core/system-design.yaml` — KIS/DART Provider 패턴, 신규 API 엔드포인트 추가
  - [업데이트] `.claude/references/data-model/stock/stock.yaml` — dart_corp_code, sector 추가
  - [업데이트] 루트 `CLAUDE.md` — Phase 테이블, 포맷, Provider 패턴 반영

- 2026-05-12: 아키텍처 규칙 위반 전수 수정 (branch: fix/essential-rules)
  - [신규] StockReader, StockWriter (domain/stock/domain/service/)
  - [신규] SignalReportReader, SignalReportWriter (domain/signal/domain/service/)
  - [신규] StockPricePort 인터페이스 (domain/price/domain/port/) — DIP 적용
  - [신규] StockErrorCode (domain/stock/application/exception/, 403xx)
  - [수정] SignalController: SignalReportRepository 직접 의존 제거
  - [수정] StockAnalysisUseCase: Repository 3개 직접 의존 → 도메인 서비스로 교체
  - [수정] PortFolioUseCase: StockRepository → StockReader/StockWriter, StockPriceFetcher → StockPricePort
  - [수정] BriefingScheduler: StockRepository/PortfolioDetailRepository → StockReader/PortfolioReader

- 2026-05-11: .claude 에이전트 컨텍스트 전면 정비 완료

## 현재 환경

```yaml
db: PostgreSQL (prod) / H2 (dev)
ai: Groq API (llama-3.1-8b-instant)
port: 8090
news_kr: Naver News API
news_us: Yahoo Finance RSS
notification: Telegram Bot
stock_price: KIS API (KOR + USA)
corporate_disclosure: DART API (KOR 종목 공시)
```

## 다음 작업 예정 (Phase 9.5 완료 → Phase 10 대기)

### Phase 3 ~ 5 ✅ 완료
- [x] 모닝 브리핑 배치 (BriefingScheduler — KOR 08:50, USA 22:20)
- [x] 시그널 히스토리 조회 API
- [x] KIS API (KOR+USA) + DART API 통합 (Phase 5)
- [x] AI 프롬프트 고도화 (공시 해석, 52주 고저, 거래량, 확신지수)

### Phase 6 (보류)
- [ ] 웹 대시보드 — 기획 보류 (Phase 5 안정화 후 재검토)

### Phase 7 (RAG — 스킵)
- [ ] PGVector + Hybrid Search — 부담스러워 스킵, Phase 9 이후 재검토

### Phase 8 ✅ 완료 (2026-05-15)
- [x] signal_feedback 테이블 (SignalFeedback 엔티티)
- [x] expectedReasonCategory 자동 추론 + SignalReport 저장
- [x] AlphaEvaluationScheduler — T+3/T+10/T+20 RAW 수익률 자동 평가
- [x] FeedbackController — /api/feedback/stats, /recent, /{reportId}

### Phase 9 ✅ 완료 (2026-05-15)
- [x] Feature Engineering Layer (4개 MVP: volume_ratio_5d, price_momentum_5d, news_freshness, market_regime)
- [x] Quant Scoring Engine (가중치 합산 0~100점)
- [x] Risk Filtering (유동성/선반영/하락장 3개 필터)
- [x] QuantUniverse 관리 (KOR 10 + USA 10 초기 데이터)
- [x] QuantSignal + QuantFeatureSnapshot 저장
- [x] /api/quant/** 5개 API 엔드포인트
- [x] 일배치 스케줄러 (KOR 09:10 / USA 22:30)

### Phase 9.5 ✅ 완료 (2026-05-21)
- [x] `QuantFundamentalPort` + `KisFundamentalAdapter` — KIS 실시간 PER/PBR 조회 (KOR 전용)
- [x] `QuantFundamentalData` 도메인 모델 (nullable PER/PBR/targetPrice/currentPrice)
- [x] `ValuationScoreCalculator` (가중치 20%) — PER·PBR 저평가도 합산 점수
- [x] `TargetPriceUpsideCalculator` (가중치 15%) — 목표주가 상승여력 (Stub: 50점 중립)
- [x] `SectorRelativeStrengthCalculator` (가중치 15%) — 섹터 평균 대비 상대 강도
- [x] 가중치 재조정 (총합 1.00): VOLUME 20% / MOMENTUM 20% / NEWS 10% / VALUATION 20% / TARGET 15% / SECTOR 15%
- [x] QuantFeatureSnapshot 7개 신규 컬럼 추가
- [x] QuantEngineUseCase 2-pass 배치 구조 (섹터 평균 사전 집계)
- [x] 신규 Calculator 단위 테스트 3종 (총 퀀트 테스트 20+ 케이스)
- USA 목표주가: Stub (Phase 10에서 Naver/Alpha Vantage 연동 예정)
- USA 펀더멘털: Stub (Phase 10에서 Alpha Vantage 실제 연동 예정)
- RAG 통합: Phase 7 미구현으로 생략 (Phase 10+에서 재검토)

### Phase 10 ~ 11 (대기)
- [ ] Phase 10: Market Regime 고도화 + Alpha Vantage 실제 연동 + RAG 통합
- [ ] Phase 11: Quant Alpha Tracking + 가중치 Self-Correction

## 참고

- Phase 5+ 로드맵: `.claude/docs/roadmap.md` 참조
- API 기본경로: /api (v1 없음)
- 시그널 타입: 반드시 BUY | HOLD | SELL 파싱
- SSE 엔드포인트: GET /api/stock/analyze/{ticker}
- Soft Delete: BaseEntity.softDelete() 제공 — DELETE API에서 반드시 사용
- KIS API: 실전/모의 환경 전환은 application.yml `kis.base-url`로만 분리 (vts 포함 여부로 trId 자동 분기)
- DART API: dart_corp_code는 Stock 테이블에 저장, KOR 종목만 해당
- KisStockPriceRouter: @Primary StockPricePort — KOR/USA 라우팅. KisKorStockPriceProvider / KisUsaStockPriceProvider는 직접 StockPricePort 미구현 (Spring 빈 충돌 방지)
- DartCorpCodeMapper: @PostConstruct + @Scheduled(cron="0 30 8 * * MON") 주 1회 갱신
- Phase 8 Alpha: RAW 수익률 기준 (벤치마크 미차감). T+3≈5일, T+10≈14일, T+20≈28일 캘린더 기준 평가.
- Phase 8 카테고리: 공시호재 | 공시악재 | 수급집중 | 실적개선 | 저평가해소 | 섹터모멘텀 | 뉴스모멘텀
- Phase 8 실패 판정: BUY alpha10d < -5%, SELL alpha10d > +5%
- Phase 8 피드백 API: /api/feedback/stats (카테고리 통계), /api/feedback/recent, /api/feedback/{reportId}
- Quant 도메인 모델: `.claude/references/data-model/quant/quant_signal.yaml` (Phase 9.5 구현 반영됨)
- Quant 에러코드: 405xx (주의: 404xx는 watchlist 도메인이 사용 중)
- Module Guard: Core ↔ Quant 직접 의존 금지. QuantNewsPort/QuantNewsCrawlerAdapter로 신호 도메인 Crawler 격리.
- KisTokenManager: Quant KisQuantDataAdapter에서 재활용 (Spring Bean 공유)
- QuantDataPort: KOR=KisQuantDataAdapter(구현), USA=Stub(Phase 10에서 AlphaVantage 실제 연동)
- QuantFundamentalPort: KOR=KisFundamentalAdapter(KIS inquire-price), USA=Stub (Phase 10 예정)
- Quant 가중치 (Phase 9.5): VOLUME 20% / MOMENTUM 20% / NEWS 10% / VALUATION 20% / TARGET_PRICE 15% / SECTOR_RELATIVE 15% + MARKET_REGIME 직접 보정
- KisFundamentalAdapter: per≤0 또는 pbr≤0 → null 처리 (적자 기업 대응)
- QuantFeatureInput 8개 파라미터: ticker, stockName, market, priceHistory, recentNews, indexReturn5d, fundamentals(nullable), sectorAvgMomentum5d
- QuantEngineUseCase 2-pass: collectAllData() → computeSectorAvgMomentums() → calculateScore()
- TargetPriceUpside Stub: targetPrice=null → 50점 중립 (Phase 10에서 네이버금융 스크래핑 예정)
- Quant 패키지: domain.quant.domain.*, domain.quant.application.*, domain.quant.presentation.*, domain.quant.infrastructure.*
- QuantUniverse 초기화: QuantUniverseInitializer @PostConstruct — 비어있을 때만 KOR10+USA10 삽입
- Quant 스케줄러: KOR 09:10 / USA 22:30 (CRISIS 국면 시 자동 건너뜀)
- Quant API: GET /api/quant/top-signals, /scores, /scores/{ticker}, /universe, /regime
- Alpha Vantage 설정: application.yaml에 추가됨 (api-key: ${ALPHA_VANTAGE_API_KEY:stub})
