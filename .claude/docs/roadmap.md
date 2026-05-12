# Jurine — 고도화 로드맵
# Last Updated: 2026-05-12

> **기획 전환 (2026-05-12)**: 단순 뉴스 기반 분석 → 증권사 수준 공식 데이터(KIS + DART) 통합으로 방향 재정의.
> Phase 1~4 (뉴스 기반 시그널)는 기반 인프라로 유지하되, Phase 5부터 데이터 소스와 서비스 구조를 전면 재설계한다.

---

## 현재 vs 다음 단계

| 구분 | 현재 (Phase 1~4) | Phase 5 (MVP) | Phase 6~8 (고도화) |
|---|---|---|---|
| 데이터 소스 | 네이버/야후 비공식 RSS | KIS API + DART API (공식) | + PGVector 임베딩 |
| 분석 대상 | 관심 종목 등록 (is_watchlist 플래그) | 동일 구조 유지 | 동일 |
| AI 방식 | 단순 프롬프트 | 프롬프트 + 전문 데이터 강화 | RAG (유사 사례 검색) |
| 알림 채널 | 텔레그램 (기본) | 텔레그램 (3줄 요약 + 미국장 브리핑) | + 웹 대시보드 |
| DB | H2 → PostgreSQL | PostgreSQL | + PGVector 확장 |
| 주가 데이터 | Yahoo Finance RSS | KIS API (국내·해외 통합) | 동일 |
| 공시 데이터 | 없음 | DART API (전자공시) | + RAG 임베딩 |

---

## Phase 5 — MVP: 전문 데이터 통합 + 사용자 중심 서비스

> **진입 조건**: Phase 3 모닝 브리핑 배치 안정화 완료

### 5-1. 데이터 소스 일원화

파편화된 비공식 RSS를 공식 API로 전환하여 데이터 신뢰도를 확보한다.

| 데이터 종류 | 현재 | 변경 | 이유 |
|---|---|---|---|
| 국내 주가·시세 | Yahoo Finance RSS (비공식) | **KIS API** | 공식 지원, 실시간 호가·체결 조회 |
| 해외 주가·시세 | Yahoo Finance RSS (비공식) | **KIS API (해외주식)** | 한투 API로 국내·해외 일원화 |
| 기업 공시 | 없음 | **DART API** | 사업보고서·수시공시 원문 수집 |
| 국내 뉴스 | Naver News API | Naver News API (유지) | 뉴스는 현행 유지 |
| 해외 뉴스 | Yahoo Finance RSS | Yahoo Finance RSS (유지) | 뉴스는 현행 유지 |

```yaml
# application.yml 추가
kis:
  app-key: ${KIS_APP_KEY}
  app-secret: ${KIS_APP_SECRET}
  base-url: https://openapi.koreainvestment.com:9443
  mock-base-url: https://openapivts.koreainvestment.com:29443  # 모의투자 테스트용

dart:
  api-key: ${DART_API_KEY}
  base-url: https://opendart.fss.or.kr/api
```

### 5-2. Provider 패턴 — KOR/USA 통합 아키텍처

도메인 인터페이스(Port)를 유지하고 구현체만 KIS로 교체한다. OCP 준수.

```
StockPricePort (domain interface)
  ├── KisKorStockPriceProvider   implements StockPricePort  // 국내주식 시세
  └── KisUsaStockPriceProvider   implements StockPricePort  // 해외주식 시세

CorporateDisclosurePort (domain interface — 신규)
  └── DartDisclosureProvider     implements CorporateDisclosurePort

NewsPort (domain interface — 기존 유지)
  ├── NaverFinanceCrawler        implements NewsPort  // KOR
  └── YahooFinanceCrawler        implements NewsPort  // USA
```

**KIS Provider 주요 연동 API:**

| 기능 | KIS API Endpoint |
|---|---|
| 국내 현재가 | `/uapi/domestic-stock/v1/quotations/inquire-price` |
| 국내 기간별 시세 | `/uapi/domestic-stock/v1/quotations/inquire-daily-price` |
| 해외 현재가 | `/uapi/overseas-price/v1/quotations/price` |
| 해외 기간별 시세 | `/uapi/overseas-price/v1/quotations/dailyprice` |
| OAuth 토큰 발급 | `/oauth2/tokenP` |

**DART Provider 주요 연동 API:**

| 기능 | DART API |
|---|---|
| 기업 공시 목록 | `/list.json` (최근 수시공시) |
| 사업보고서 재무제표 | `/fnlttSinglAcnt.json` |
| 주요사항 보고 | `/list.json?pblntf_detail_ty=C` |

### 5-3. 텔레그램 알림 고도화

> **개인 프로젝트**: User 엔티티 / 다중 사용자 / 개인화 기능 구현하지 않음.
> 단일 텔레그램 봇으로 고정 시각(08:50)에 브리핑 발송. `is_watchlist=true` 종목 대상.

매일 **08:50** (장 시작 10분 전) 고정 발송.

**알림 구성:**

```
[Jurine 모닝 브리핑] 2026-05-12 (화) 08:50

━━━━━━ 🌏 미국장 요약 ━━━━━━
어제 나스닥 +1.2% / S&P500 +0.8%
달러-원 환율 1,320원 (전일비 +3원)
주목 이슈: 엔비디아 실적 발표 예정

━━━━━━ 📊 관심 종목 시그널 ━━━━━━

🟢 삼성전자 (005930) — BUY
① HBM 수주 확대로 반도체 업황 회복 기대
② 52주 신고가 근접, 외국인 순매수 3일 연속
③ 최근 공시: 자사주 소각 결정 (주주가치 제고)

🟡 카카오 (035720) — HOLD
① 플랫폼 규제 리스크 상존, 방향성 불명확
② 주가 횡보 구간, 뚜렷한 모멘텀 부재
③ 공시 이슈 없음

─────────────────────────
⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.
```

**알림 원칙:**
- AI 판단 근거는 반드시 **3줄 요약** (바쁜 아침 배려)
- 미국장 야간 동향 + 환율 정보 포함 (시초가 예측 맥락 제공)
- 종목별 최신 DART 공시 이슈 자동 첨부

### 5-4. 시스템 프롬프트 개선 (Phase 5 버전)

```
너는 15년 경력의 주식 애널리스트이자 주린이 튜터다.

[판단 기준]
- 뉴스 감성 (긍정 / 부정 / 중립)
- 최근 주가 추세 (KIS API 제공 시세 기반)
- 최근 공시 이벤트 (DART 수시공시 — 자사주, 실적, 경영진 변동 등)
- 거시경제 영향도 (금리, 환율, 원자재)
- 섹터 특성 (반도체/소비재/금융/바이오 등 섹터별 민감도)

[출력 규칙]
- signal_type: 반드시 [BUY] [HOLD] [SELL] 중 하나 명시
- 판단 근거: 반드시 3줄 이내 (뉴스 + 주가 + 공시 중 핵심 2~3개)
- 마지막 줄: 면책 문구 필수

[입력 데이터]
- 종목: {ticker} / {name} / {market} / {sector}
- 뉴스: {crawledNews}
- 주가 흐름 (최근 20거래일, KIS): {priceData}
- 최근 공시 (DART): {disclosureData}
- 참고 컨텍스트: {ragContext}  ← Phase 7에서 추가
```

---

## Phase 6 — 웹 대시보드 (설계 우선, 구현 후순위)

> 현재 단계에서는 설계만 고려. 텔레그램 알림 이력 아카이빙 및 성과 시각화가 목표.

### 목표

- 텔레그램으로 발송된 시그널 이력을 브라우저에서 조회
- 종목별 시그널 히트율 및 수익률 차트 시각화
- 사용자 관심 종목 CRUD (웹 UI)

### 설계 방향

```
[Frontend] React SPA
  └── 시그널 히스토리 테이블
  └── 종목별 수익률 차트 (Chart.js)
  └── 관심 종목 등록/삭제 UI

[Backend] 기존 Spring Boot API 재활용
  └── GET /api/reports/history        — 전체 이력
  └── GET /api/users/{id}/watchlist   — 사용자 관심 종목
```

> **구현 시점**: Phase 5 안정화 후 별도 기획 진행.

---

## Phase 7 — RAG + PGVector (벡터 DB 도입)

### 목표

DART 공시·뉴스·과거 시그널을 벡터로 저장하고, 현재 상황과 **유사한 과거 케이스를 검색**하여 시그널 근거를 강화한다.

### 기술 스택 추가

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
```

### 전체 흐름

```
[데이터 수집]
DART 공시 + 크롤링 뉴스 + 과거 시그널 결과
        ↓
[임베딩 변환]
Spring AI EmbeddingClient → 텍스트를 벡터로 변환
        ↓
[벡터 저장]
PGVector (PostgreSQL 확장 — 별도 DB 불필요)
        ↓
[RAG 검색]
현재 뉴스·공시 입력
→ 유사 과거 케이스 Top 3 검색
→ 검색 결과를 프롬프트 {ragContext}에 삽입
→ AI가 과거 맥락 기반으로 시그널 생성
```

### 벡터화 대상 데이터

| 데이터 | 소스 | 활용 방식 |
|---|---|---|
| DART 공시 청크 | DART API | 기업 재무/사업 맥락 주입 |
| 크롤링 뉴스 | 네이버/야후 | 유사 뉴스 케이스 검색 |
| 과거 시그널 + 실제 주가 결과 | 자체 DB | "이 시그널이 맞았나" 근거 주입 |

### 핵심 코드 패턴

```java
// 공시 임베딩 저장
vectorStore.add(List.of(
    new Document(disclosureText, Map.of("ticker", "005930", "type", "dart"))
));

// 유사 케이스 검색 (현재 뉴스 기준)
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(currentNewsText)
                 .withTopK(3)
                 .withFilterExpression("ticker == '005930'")
);

// 검색 결과 → 프롬프트 삽입
String ragContext = results.stream()
    .map(Document::getContent)
    .collect(Collectors.joining("\n---\n"));
```

---

## Phase 8 — 피드백 루프 (시그널 품질 고도화)

### 목표

과거 시그널이 실제로 맞았는지 추적하여 AI 판단 품질을 점진적으로 개선한다.
단순 등락률이 아닌 **시장 대비 초과 수익률(Alpha)** 지표를 사용하여 시장 노이즈를 제거한다.

### Alpha 기반 평가 공식

```
Alpha = 종목 수익률(N일) - 벤치마크 지수 수익률(N일)

예시)
BUY 시그널 → 7일 후 종목 +5%, 코스피 +3% → Alpha +2%  (긍정: 시장 대비 초과)
BUY 시그널 → 7일 후 종목 +2%, 코스피 +4% → Alpha -2%  (부정: 시장도 못 따라감)
SELL 시그널 → 7일 후 종목 -5%, 코스피 -1% → Alpha -4% (긍정: 시장보다 더 빠짐)
```

### 추가 테이블

```sql
CREATE TABLE signal_feedback (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    report_id        BIGINT       NOT NULL REFERENCES signal_report(id),
    price_at_signal  DECIMAL(12,2) NOT NULL,   -- 시그널 생성 시점 주가
    price_after_7d   DECIMAL(12,2),            -- 7일 후 주가 (스케줄러 수집)
    index_return_7d  DECIMAL(6,2),             -- 7일간 벤치마크 지수 수익률 (%)
    stock_return_7d  DECIMAL(6,2),             -- 7일간 종목 수익률 (%)
    alpha_7d         DECIMAL(6,2),             -- Alpha = stock_return - index_return
    was_correct      BOOLEAN,                  -- 시그널 방향성과 Alpha 부호 일치 여부
    evaluated_at     TIMESTAMP
);
```

### 흐름

```
시그널 생성 (BUY / HOLD / SELL)
        ↓
7일 후 KIS API로 실제 주가 + 벤치마크 지수 자동 수집
        ↓
Alpha 계산 후 signal_feedback 저장
        ↓
Phase 7 RAG에 "이 시그널의 Alpha = +2% (적중)" 맥락 임베딩
→ 유사 상황 재발 시 과거 성과 기반 시그널 신뢰도 강화
```
