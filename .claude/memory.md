# Jurine — Agent Memory
# Last Updated: 2026-05-12

## 현재 상태

phase: Phase 1~3 구현 중
branch: feat
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

- 2026-05-12: 아키텍처 규칙 위반 전수 수정 (branch: fix/essential-rules)
  - [신규] StockReader, StockWriter (domain/stock/domain/service/)
  - [신규] SignalReportReader, SignalReportWriter (domain/signal/domain/service/)
  - [신규] StockPricePort 인터페이스 (domain/price/domain/port/) — DIP 적용
  - [신규] StockErrorCode (domain/stock/application/exception/, 403xx)
  - [수정] SignalController: SignalReportRepository 직접 의존 제거
  - [수정] StockAnalysisUseCase: Repository 3개 직접 의존 → 도메인 서비스로 교체, @Transactional(readOnly=true) on class 추가, getTodayReports()/getHistoryByStockId() 이관
  - [수정] PortFolioUseCase: StockRepository → StockReader/StockWriter, StockPriceFetcher → StockPricePort
  - [수정] BriefingScheduler: StockRepository/PortfolioDetailRepository → StockReader/PortfolioReader
  - [수정] Stock, SignalReport, PortfolioDetail: @AllArgsConstructor(PRIVATE) 추가
  - [수정] Stock.ticker(length=20), Stock.name(length=100) 지정
  - [수정] StockAnalysisResponse: static factory of() 추가

- 2026-05-11: .claude 에이전트 컨텍스트 전면 정비 완료
  - 타 프로젝트(bangjjack/Dawa-BE) 설정 → Jurine 전용으로 교체
  - manifest.md: Dawa-BE-Agent → Jurine-Agent
  - essential-rules.yaml: Java 21, Spring Boot 3.4+, H2(dev), Groq AI, SSE, soft delete 규칙 반영
  - system-design.yaml: 패키지명(com.obigo.demodong), API base_url(/api), 엔드포인트 목록 반영
  - coding-style.yaml / testing-guide.yaml: Kotlin → Java, Kotest → JUnit 5
  - build_checker.sh: .kt → .java 수정
  - 도메인 모델: user → stock / portfolio_detail / signal_report / price_snapshot 교체
  - soft delete: BaseEntity 구현 확인 후 규칙 및 도메인 모델 전체 반영
  - features/: auth, example 제거
  - CLAUDE.md(루트): 252줄 → 47줄 경량화 (기술스택·ERD·API 목록 제거)
  - CLAUDE_NEXT.md: 정리 후 .claude/docs/roadmap.md 으로 이동 (루트 파일 삭제)
    - 용어해설 기능 제거, KIS API(KOR) / Alpha Vantage(USA) 전환 계획 반영

## 현재 환경

```yaml
db: H2 in-memory (개발용)
ai: Groq API (llama-3.1-8b-instant)
port: 8090
news_kr: Naver News API
news_us: Yahoo Finance RSS
notification: Telegram Bot
```

## 다음 작업 예정

- [ ] Phase 3 모닝 브리핑 배치 안정화
- [ ] 텔레그램 알림 포맷 개선
- [ ] PostgreSQL 전환 (프로덕션)
- [ ] Phase 4: 시그널 히스토리 조회 API 완성

## 참고

- Phase 5+ 로드맵: `.claude/docs/roadmap.md` 참조 (RAG, PGVector, KIS API)
- API 기본경로: /api (v1 없음)
- 시그널 타입: 반드시 BUY | HOLD | SELL 파싱
- SSE 엔드포인트: GET /api/stock/analyze/{ticker}
- Soft Delete: BaseEntity.softDelete() 제공 — DELETE API에서 반드시 사용
