# Jurine — Agent Memory
# Last Updated: 2026-05-15

## 현재 상태

phase: Phase 8 구현 완료 (Phase 6 보류, Phase 7 RAG 스킵)
branch: feature/quant
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

## 다음 작업 예정 (Phase 8 완료 → Phase 9 대기)

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

### Phase 9 ~ (대기)
- [ ] Quant Signal Engine MVP (Phase 9 — RAG 인프라 없으면 9-1~9-3만 착수 가능)

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
- Quant 도메인 모델: `.claude/references/data-model/quant/quant_signal.yaml`
- Quant 에러코드: 405xx (주의: 404xx는 watchlist 도메인이 사용 중)
- Module Guard: Core ↔ Quant 직접 의존 금지. 공유는 RAG/Feedback/Telegram만 허용.
- KisTokenManager: Quant에서도 재활용 가능 (Spring Bean 공유)
- Alpha Vantage API: USA Quant 지표용 별도 연동 필요 (Phase 9에서 추가)
