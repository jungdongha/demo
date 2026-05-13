# Jurine — Agent Memory
# Last Updated: 2026-05-13

## 현재 상태

phase: Phase 5 구현 완료 / Phase 6 설계 대기
branch: feature/phase5
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

## 다음 작업 예정 (Phase 3 → Phase 5 순)

### Phase 3 마무리 (당면 과제)
- [x] 모닝 브리핑 배치 안정화
- [x] 텔레그램 알림 포맷 개선 (3줄 요약 원칙 적용)

### Phase 4
- [x] 시그널 히스토리 조회 API 완성

### Phase 5 구현 과제 ✅ 완료 (2026-05-13)
> **전제**: 개인 프로젝트 — User 엔티티, 다중 사용자, 개인화 기능 구현하지 않음. 단일 사용자 구조 유지.
- [x] Stock 테이블 — sector, dart_corp_code 컬럼 추가
- [x] KisTokenManager 구현 (OAuth2 토큰 발급·캐싱, AtomicReference, 23h 자동 갱신)
- [x] KisKorStockPriceProvider 구현 (현재가 + 일별 시세, DB 캐싱)
- [x] KisUsaStockPriceProvider 구현 (NAS→NYS→AMS 거래소 fallback)
- [x] KisStockPriceRouter — @Primary, KOR/USA 라우팅
- [x] DartDisclosureProvider 구현 (CorporateDisclosurePort)
- [x] DartCorpCodeMapper — 앱 시작 시 corpCode.xml ZIP 파싱 + DB 자동 매핑
- [x] AI 프롬프트 개선 — sector, disclosureData 변수 추가, 판단기준 4항목

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
