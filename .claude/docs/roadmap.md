# Jurine — Phase 5+ 고도화 로드맵
# Last Updated: 2026-05-11

> **진입 조건**: Phase 1~4 (뉴스 기반 시그널)가 안정적으로 동작한 이후 진행한다.
> - [ ] 온디맨드 시그널 정상 동작
> - [ ] 모닝 브리핑 배치 안정화
> - [ ] 시그널 히스토리 1개월 이상 누적

---

## 현재 vs 다음 단계

| 구분 | 현재 (Phase 1~4) | 다음 단계 (Phase 5+) |
|---|---|---|
| 판단 근거 | 뉴스 텍스트 + 주가 흐름 | 뉴스 + 주가 + 공시 + 과거 시그널 + 거시지표 |
| AI 방식 | 단순 프롬프트 | RAG (검색 증강 생성) |
| 데이터 저장 | H2 → PostgreSQL | PostgreSQL + PGVector (벡터 확장) |
| 시그널 근거 | AI 즉흥 판단 | 유사 과거 케이스 기반 판단 |
| 주가 데이터 (KOR) | Yahoo Finance RSS | 한국투자증권 KIS API |
| 주가 데이터 (USA) | Yahoo Finance RSS | Yahoo Finance (재무제표) + Alpha Vantage (기술지표) |

---

## Phase 5 — 도메인 지식 강화 + 데이터 소스 고도화

### 5-1. 주가 데이터 소스 교체

현재 비공식 RSS에 의존하는 구조를 공식 API로 전환한다.

| 시장 | 현재 | 교체 대상 | 이유 |
|---|---|---|---|
| KOR | Yahoo Finance RSS (비공식) | **한국투자증권 KIS API** | 공식 지원, 실시간 호가·체결·잔고 조회 가능 |
| USA 재무제표 | Yahoo Finance RSS (비공식) | **Yahoo Finance API (공식)** | 공시된 재무 데이터 (PER, PBR, EPS 등) |
| USA 기술지표 | 없음 | **Alpha Vantage API** | RSI·MACD·이동평균 등 기술적 분석 지표 제공 |

```yaml
# application.yml 추가 예정
kis:
  api-key: ${KIS_API_KEY}
  base-url: https://openapi.koreainvestment.com:9443

alpha-vantage:
  api-key: ${ALPHA_VANTAGE_API_KEY}
  base-url: https://www.alphavantage.co/query
```

**크롤러 전략 패턴 확장** (OCP 준수 — 기존 코드 변경 없이 구현체 추가):
```java
// 기존
NaverFinanceCrawler  // KOR 뉴스
YahooFinanceCrawler  // USA 뉴스 (RSS)

// 추가
KisStockPriceFetcher      // KOR 주가 (KIS API)
AlphaVantageTechIndicator // USA 기술지표 (Alpha Vantage)
```

### 5-2. 시스템 프롬프트 고도화

```
너는 15년 경력의 주식 애널리스트이자 주린이 튜터다.

[판단 기준]
- 뉴스 감성 (긍정 / 부정 / 중립)
- 최근 1개월 주가 추세 (상승 / 하락 / 횡보)
- 거시경제 영향도 (금리, 환율, 원자재)
- 섹터 특성:
  - 반도체: 경기 민감, 사이클 산업, 수출 의존도 높음
  - 소비재: 경기 방어주, 내수 중심
  - 금융: 금리 민감, 대출 규제 영향
  - 바이오: 임상 결과 이벤트 드리븐
- 기술적 지표: RSI(과매수/과매도), MACD(추세 전환) ← Phase 5에서 추가

[출력 규칙]
- signal_type: 반드시 [BUY] [HOLD] [SELL] 중 하나 명시
- 판단 근거: 3줄 이내, 뉴스와 주가 흐름 모두 언급
- 마지막 줄: 면책 문구 필수

[입력 데이터]
- 종목: {ticker} / {name} / {sector}
- 뉴스: {crawledNews}
- 주가 흐름 (1개월): {priceData}
- 기술지표: {technicalIndicators}  ← Phase 5에서 추가
- 보유 수익률: {profitRate}        ← 보유 종목인 경우만
- 참고 컨텍스트: {ragContext}      ← Phase 6에서 추가
```

### 5-3. 추가 데이터 소스 (무료)

| 목적 | 소스 |
|---|---|
| 섹터/업종 분류 | KRX 종목 정보 (http://data.krx.co.kr) |
| 금리/환율/거시지표 | 한국은행 ECOS API (https://ecos.bok.or.kr) |
| 기업 공시 원문 | DART 전자공시 API (https://opendart.fss.or.kr) |

---

## Phase 6 — 벡터 DB 도입 (PGVector + RAG)

### 목표
과거 뉴스·공시·시그널 결과를 벡터로 저장하고, 현재 상황과 **유사한 과거 케이스를 검색**해 시그널 근거를 강화한다.

### 기술: PGVector
Spring AI 공식 지원. 기존 PostgreSQL에 확장만 추가하면 되므로 별도 벡터 DB 불필요.

```yaml
# application.yml 추가
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
PGVector (PostgreSQL 확장)
        ↓
[RAG 검색]
현재 뉴스 입력
→ 유사 과거 케이스 Top 3 검색
→ 검색 결과를 프롬프트 {ragContext}에 삽입
→ AI가 과거 맥락 기반으로 시그널 생성
```

### 핵심 코드 패턴

```java
// 벡터 저장
vectorStore.add(List.of(
    new Document(newsText, Map.of("ticker", "005930", "type", "news"))
));

// 유사 케이스 검색
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(currentNewsText)
                 .withTopK(3)
                 .withFilterExpression("ticker == '005930'")
);

// 검색 결과를 프롬프트에 삽입
String ragContext = results.stream()
    .map(Document::getContent)
    .collect(Collectors.joining("\n---\n"));
```

### 벡터화할 데이터 종류

| 데이터 | 소스 | 활용 방식 |
|---|---|---|
| 크롤링 뉴스 원문 | 네이버 금융 / Yahoo Finance | 유사 뉴스 케이스 검색 |
| DART 공시 청크 | DART API | 기업 재무/사업 맥락 주입 |
| 과거 시그널 + 실제 주가 결과 | 자체 DB | "이 시그널이 맞았나" 근거 |

---

## Phase 7 — 시그널 피드백 루프

### 목표
과거 시그널이 실제로 맞았는지 추적하여 AI 판단 품질을 점진적으로 개선한다.

### 흐름
```
시그널 생성 (BUY / HOLD / SELL)
        ↓
7일 후 실제 주가 변동 자동 수집
        ↓
"매수 신호 → 실제 +8%" 결과를 벡터 DB에 저장
        ↓
다음 유사 상황에서 이 케이스를 RAG로 참조
→ 시그널 신뢰도 점진적 향상
```

### 추가 테이블

```
[signal_feedback]
- id              BIGINT PK
- report_id       BIGINT FK → signal_report
- price_at_signal DECIMAL(10,2)   -- 시그널 생성 시점 주가
- price_after_7d  DECIMAL(10,2)   -- 7일 후 주가
- actual_change   DECIMAL(5,2)    -- 실제 등락률 (%)
- was_correct     BOOLEAN         -- 시그널이 맞았는지
- evaluated_at    TIMESTAMP
```

---

## 활용 시나리오

### ① RAG 기반 공시 검색
```
삼성전자 분석 요청
→ DART 최근 사업보고서 청크 벡터 검색
→ "반도체 매출 비중 60%, 중국 의존도 30%" 맥락 자동 삽입
→ 수출 규제 뉴스를 더 정확하게 해석
```

### ② 과거 유사 케이스 검색
```
현재: "반도체 수출 규제 강화" 뉴스
→ 벡터 검색: "3개월 전 동일 뉴스 → 매도 신호 → 실제 -12%"
→ 과거 결과를 근거로 시그널 신뢰도 강화
```
