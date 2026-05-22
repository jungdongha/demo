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

## Phase 7 — RAG + PGVector (금융 특화 에이전트로 진화)
# Last Updated: 2026-05-13

### 목표

DART 공시·뉴스·과거 시그널을 벡터로 저장하고, 현재 상황과 **유사한 과거 케이스를 검색**하여 시그널 근거를 강화한다.
단순 텍스트 유사도 검색을 넘어, **[과거 공시 + 당시 시그널 + 결과 Alpha]** 세트를 AI에게 주입하는 '비교 분석' 방식으로 고도화한다.

---

### 7-1. 기술 스택

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536   # text-embedding-3-small 기준
        # HNSW 정밀도 파라미터 (데이터 수 증가 시 조정)
        # m: 16             # 연결 수 (기본 16, 높을수록 정밀·느림)
        # ef-construction: 64  # 인덱스 빌드 정밀도

# 한국어 금융 특화 임베딩 검토
# - OpenAI text-embedding-3-small: 범용, API 비용 발생
# - HuggingFace BGE-M3: 한국어 금융 용어 정확도 우수, 로컬 실행 가능
#   → DART 공시/뉴스는 BGE-M3, 범용 텍스트는 text-embedding-3-small 이중 전략 고려
```

---

### 7-2. 하이브리드 검색 (Hybrid Search) 도입

> **배경**: 공시 텍스트는 "제3자배정 유상증자", "수시공시" 등 정형화된 키워드가 많아
> 벡터 유사도만으로는 특정 공시 종류를 놓칠 수 있다.

**Vector Search (Cosine Similarity)** — 문맥·의미 유사도
**Full-text Search (PostgreSQL tsvector)** — 공시 종류·키워드 정확 매칭

두 결과를 RRF(Reciprocal Rank Fusion) 방식으로 합산하여 최종 Top-K 추출.

```sql
-- PGVector 확장 + 전문 검색 인덱스 동시 생성
CREATE INDEX ON rag_document USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON rag_document USING gin (to_tsvector('korean', content));
```

```java
// 하이브리드 검색 패턴 (Spring AI + 직접 JPQL 병합)
// 1단계: 벡터 검색 (Top-10)
List<Document> vectorResults = vectorStore.similaritySearch(
    SearchRequest.query(currentText)
        .withTopK(10)
        .withFilterExpression("sector == 'semiconductor' AND type == 'dart'")
);

// 2단계: 키워드 FTS 검색 (PostgreSQL 직접)
// SELECT * FROM rag_document WHERE to_tsvector('korean', content) @@ plainto_tsquery('유상증자')

// 3단계: RRF 스코어 병합 → 상위 3개 선택
```

---

### 7-3. 메타데이터 필터링 정교화

ticker 외에 **sector** 필터를 필수 적용한다.
"반도체 수주 공시"와 "바이오 임상 결과"는 문장 구조가 비슷해도
주가 Alpha 패턴이 완전히 다르기 때문이다.

```java
// 벡터 저장 시 메타데이터 구조
vectorStore.add(List.of(
    new Document(disclosureText, Map.of(
        "ticker",        "005930",
        "sector",        "semiconductor",   // ← 신규
        "type",          "dart",            // dart | news | signal
        "signal_result", "BUY",             // 당시 시그널
        "alpha_7d",      "+4.5",            // 결과 Alpha (Phase 8 연동 후 업데이트)
        "is_failure",    "false"            // 실패 케이스 태그 (Negative Sampling용)
    ))
));
```

| 메타데이터 키 | 값 예시 | 용도 |
|---|---|---|
| `ticker` | `005930` | 동일 종목 필터 |
| `sector` | `semiconductor` | 섹터 패턴 학습 |
| `type` | `dart` / `news` / `signal` | 데이터 종류 구분 |
| `signal_result` | `BUY` / `HOLD` / `SELL` | 당시 판단 |
| `alpha_7d` | `+4.5` | 7일 후 결과 (Phase 8 연동) |
| `is_failure` | `true` / `false` | Negative Sampling 태그 |

---

### 7-4. RAG Context 재구성 — [공시 + 시그널 + Alpha] 세트 주입

검색된 과거 케이스를 텍스트만 넘기지 않고, **결과까지 포함한 세트**로 주입한다.

```java
// RAG 컨텍스트 포맷터
String ragContext = results.stream().map(doc -> {
    String type       = doc.getMetadata().getOrDefault("type", "").toString();
    String signal     = doc.getMetadata().getOrDefault("signal_result", "?").toString();
    String alpha      = doc.getMetadata().getOrDefault("alpha_7d", "미측정").toString();
    String isFailure  = doc.getMetadata().getOrDefault("is_failure", "false").toString();
    String prefix     = "true".equals(isFailure) ? "⚠️ [실패 사례]" : "✅ [성공 사례]";
    return prefix + " (" + type + ")\n"
        + doc.getContent() + "\n"
        + "→ 당시 시그널: " + signal + " / 7일 Alpha: " + alpha + "%";
}).collect(Collectors.joining("\n\n---\n\n"));
```

**프롬프트 주입 예시:**
```
[유사 과거 케이스]
✅ [성공 사례] (dart)
삼성전자 — 3,000억 규모 HBM 공급계약 체결 (매출 대비 12%)
→ 당시 시그널: BUY / 7일 Alpha: +4.5%

⚠️ [실패 사례] (dart)
SK하이닉스 — 300억 운영자금 조달 CB 발행
→ 당시 시그널: HOLD / 7일 Alpha: -6.2%

이 맥락을 참고하되, 현재 데이터와의 차이점을 반드시 고려하여 시그널을 확정하라.
```

---

### 7-5. Self-Correction — Negative Sampling (자아 성찰 루프)

> **핵심**: "스스로 실수를 교정하는 에이전트"로 진화시키는 핵심 메커니즘.

Alpha가 크게 음수(-)인 '실패 시그널'을 `is_failure=true` 태그로 분리 관리한다.
새 시그널 생성 시 **성공 사례(Positive)** 와 **실패 사례(Negative)** 를 함께 주입하여
AI가 과거 실수 패턴을 스스로 인식하게 만든다.

```
검색 전략:
- Positive Recall: 유사 상황에서 Alpha > 0이었던 케이스 Top 2
- Negative Recall: 유사 상황에서 Alpha < -5%였던 케이스 Top 1

→ 프롬프트: "과거에 비슷한 상황에서 BUY를 냈다가 Alpha -10%를 기록한 사례가 있습니다.
             이번에도 그때와 같은 패턴은 아닌지 검증하고 시그널을 확정하세요."
```

---

### 7-6. 전체 흐름 (개선 버전)

```
[신규 뉴스/공시 입력]
        ↓
[Hybrid Search: Vector(Cosine) + Keyword(FTS)]
  필터: sector + type
        ↓
[유사 케이스 분류]
  ├── Positive (Alpha > 0) → ✅ 성공 사례 프롬프트
  └── Negative (Alpha < -5%) → ⚠️ 실패 경고 프롬프트
        ↓
[AI 시니어 애널리스트]
  입력: 현재 데이터 + RAG Context (성공+실패 세트)
  출력: 시그널 + 확신 지수 + 판단 근거
        ↓
[시그널 저장 + Phase 8 피드백 루프]
        ↓
[벡터 DB 메타데이터 업데이트 (Alpha 반영)]
   ↑___________________________________↓ (자기강화 루프)
```

---

## Phase 8 — 피드백 루프 (시그널 품질 자동 고도화) ✅ 구현 완료 (2026-05-15)
# Last Updated: 2026-05-15

> **구현 범위**: RAG(Phase 7) 미구현으로 벡터 DB 메타데이터 업데이트는 생략.
> 나머지 Alpha 추적·평가·실패 판정 루프 전체 구현 완료.
>
> **구현된 컴포넌트**:
> - `SignalFeedback` 엔티티 (signal_feedback 테이블) — T+3/T+10/T+20 Alpha
> - `AlphaEvaluationScheduler` — 매 거래일 18:00 자동 평가
> - `FeedbackController` — /api/feedback/stats, /recent, /{reportId}
> - `SignalReport.expectedReasonCategory` — 키워드 기반 판단 근거 카테고리 자동 저장

### 목표

과거 시그널이 실제로 맞았는지 추적하고, 그 결과를 벡터 DB에 피드백하여
**AI가 자신의 역사적 실수를 참고해 판단을 개선하는** 자기강화 루프를 구축한다.

---

### 8-1. 다중 타임프레임 Alpha 평가

> 7일 단일 지표는 "7일 후 올랐지만 3일 차에 -15% MDD"인 위험 시그널을 놓칠 수 있다.

| 타임프레임 | 의미 | 수집 스케줄 |
|---|---|---|
| T+3 | 단기 모멘텀 (재료 즉시 반응) | 시그널 D+3 영업일 |
| T+10 | 재료 반영 완료 기간 | 시그널 D+10 영업일 |
| T+20 | 추세 형성 여부 | 시그널 D+20 영업일 |
| MDD | 기간 내 최대 낙폭 | T+3 ~ T+20 중 최저가 추적 |

Alpha 공식:
```
Alpha(N) = 종목 수익률(N일) - 벤치마크 지수 수익률(N일)
MDD = (기간 내 최저가 - 시그널 시점 주가) / 시그널 시점 주가 × 100
```

---

### 8-2. signal_feedback 테이블 (확장 버전)

```sql
CREATE TABLE signal_feedback (
    id                       BIGINT        PRIMARY KEY,
    report_id                BIGINT        NOT NULL REFERENCES signal_report(id),

    -- 시그널 시점
    price_at_signal          DECIMAL(12,2) NOT NULL,
    expected_reason_category VARCHAR(50),   -- 실적개선 | 수급집중 | 저평가해소 | 공시호재 | 섹터모멘텀

    -- 다중 타임프레임 주가
    price_after_3d           DECIMAL(12,2),
    price_after_10d          DECIMAL(12,2),
    price_after_20d          DECIMAL(12,2),
    price_mdd                DECIMAL(12,2), -- 기간 내 최저가

    -- 벤치마크 수익률 (코스피 or S&P500)
    index_return_3d          DECIMAL(6,2),
    index_return_10d         DECIMAL(6,2),
    index_return_20d         DECIMAL(6,2),

    -- Alpha (타임프레임별)
    alpha_3d                 DECIMAL(6,2),
    alpha_10d                DECIMAL(6,2),
    alpha_20d                DECIMAL(6,2),
    mdd_pct                  DECIMAL(6,2), -- 최대 낙폭 %

    -- 평가 결과
    was_correct_3d           BOOLEAN,      -- T+3 기준 방향성 일치
    was_correct_10d          BOOLEAN,      -- T+10 기준 (Primary 지표)
    is_failure               BOOLEAN       -- alpha_10d < -5% → Negative Sampling 대상

    evaluated_at             TIMESTAMP
);
```

---

### 8-3. expected_reason_category — "예상한 이유가 맞았는가"

시그널 저장 시 **어떤 시나리오로 판단했는지 카테고리**를 함께 기록한다.
나중에 카테고리별 Alpha 통계를 내어 "어떤 이유의 시그널이 가장 신뢰할 수 있는가"를 도출한다.

| 카테고리 | 예시 시나리오 |
|---|---|
| `실적개선` | 매출·영업이익 성장 전망 뉴스 |
| `공시호재` | 자사주 소각, 대규모 수주 공시 |
| `수급집중` | 외국인/기관 순매수 + 거래량 급증 |
| `저평가해소` | PER·PBR 역사적 저점 + 섹터 회복 |
| `섹터모멘텀` | 동종업계 전반 상승 흐름 |

```java
// SignalReport 저장 시 카테고리 자동 추론 (Phase 8 구현)
// AI 출력에서 파싱하거나, 별도 분류 프롬프트로 추출
signalReport.setExpectedReasonCategory("공시호재");
```

---

### 8-4. 자기강화 루프 전체 흐름

```
시그널 생성 (BUY / HOLD / SELL)
  + expected_reason_category 저장
        ↓
T+3 / T+10 / T+20 스케줄러
  → KIS API로 주가 + 벤치마크 지수 수집
  → Alpha(N) + MDD 계산
  → signal_feedback 저장
  → is_failure 태그 결정 (alpha_10d < -5%)
        ↓
벡터 DB 메타데이터 업데이트
  → 해당 Document의 alpha_7d, is_failure 갱신
        ↓
다음 분석 시 Hybrid RAG 검색
  → 실패 케이스가 Negative Warning으로 프롬프트에 주입
  → AI가 과거 실수를 참고하여 시그널 교정
        ↓
카테고리별 Alpha 통계 (대시보드 Phase 6 연동)
  → "공시호재 카테고리의 T+10 Alpha 평균 +3.2%"
  → 이 통계를 시스템 프롬프트에 주기적으로 반영
```

---

### 8-5. 기술적 주의사항

**HNSW 파라미터 튜닝:**
```yaml
# 데이터 1만 건 이하: 기본값으로 충분
# 실시간 공시 처리로 데이터 증가 시
m: 16                  # 높일수록 정밀도↑, 인덱스 크기↑
ef_construction: 64    # 높일수록 빌드 정밀도↑, 속도↓
ef_search: 40          # 검색 시 후보 수 (runtime 조정 가능)
```

**임베딩 모델 전략:**
```
1순위 (권장): HuggingFace BGE-M3 (로컬)
  - 한국어 금융 용어 정확도 우수
  - DART 공시, 네이버 뉴스 처리에 유리
  - 비용 없음, GPU 있으면 빠름

2순위 (fallback): OpenAI text-embedding-3-small
  - 범용, 영문 해외 뉴스 처리 시 사용
  - API 비용 발생
  - dimensions: 1536

→ 이중 전략: KOR 데이터 → BGE-M3, USA 뉴스 → text-embedding-3-small
```


---

## ════════════════════════════════════════════════════
## 전체 확장 구조 — Core + Quant Signal Engine
## ════════════════════════════════════════════════════

```
Jurine
├── Core Analysis          (Phase 1~8)   ← 기존 로드맵 유지
│   └── 사용자 관심 종목 AI 분석
│       ├── KIS + DART 공식 데이터
│       ├── RAG + Hybrid Search (Phase 7)
│       └── 피드백 루프 + Self-Correction (Phase 8)
│
├── Quant Signal Engine    (Phase 9~11)  ← NEW — 독립 모듈
│   └── 한국/미국 시총 TOP10 실시간 탐지
│       ├── Feature Engineering + Scoring Engine
│       ├── Market Regime Analysis + RAG 통합
│       └── Alpha Tracking + 가중치 Self-Correction
│
└── 공유 인프라
    ├── PGVector RAG (Phase 7 구축 → Phase 10에서 Quant도 활용)
    ├── Feedback Loop (signal_feedback.feedback_source 필드로 Core/Quant 구분)
    └── Telegram Bot (공통 알림)
```

> **Core와 Quant는 도메인을 완전히 분리한다.**
> RAG(PGVector)와 Feedback Loop는 공유 인프라로 `source_module` 메타데이터 필드로 구분한다.

---

## Quant 전체 비교

| 구분 | Core (Phase 1~8) | Quant Signal (Phase 9~11) |
|---|---|---|
| 분석 대상 | 사용자 관심 종목 | 시총 TOP20 (KOR 10 + USA 10) |
| 판단 근거 | 뉴스 + 공시 + 기술지표 + RAG | 정량 Quant Score + RAG 보강 |
| AI 역할 | 최종 판단 + 과거 사례 비교 | 해석/리포트 생성만 (선정은 Quant Score) |
| 후보 선정 | 사용자가 등록한 종목 | Quant Scoring Engine → TOP3 자동 선별 |
| 시그널 주기 | 온디맨드 + 모닝 배치 | 실시간 (장중) + 장전 브리핑 |
| 데이터 | KIS + DART + 뉴스 | KIS 수급/거래량 + Alpha Vantage + 섹터 데이터 |

---

## ════════════════════════════════════════════════════
## Phase 9~11 — Jurine Quant Signal Engine
## "관심 종목 분석"에서 "시장 강자 실시간 탐지 플랫폼"으로 확장
## ════════════════════════════════════════════════════

### 핵심 철학

> **LLM은 "해석 엔진"**, **Quant Scoring Engine은 "선정 엔진"**
>
> - **후보 선정**: Quant Scoring Engine이 정량 지표 기반으로 TOP3 자동 선별
> - **AI 역할**: 이유 분석, 리스크 설명, 시장 해석, 최종 리포트 생성만 담당
> - LLM이 "어떤 종목을 고를지"를 판단하지 않는다 — **숫자가 판단하고 LLM이 설명한다**

### 분석 대상 유니버스

| 시장 | 대상 | 선정 기준 |
|---|---|---|
| KOR | 시총 TOP 10 | KOSPI 시가총액 상위 10개 (삼성전자, SK하이닉스 등) |
| USA | 시총 TOP 10 | S&P 500 시가총액 상위 10개 (AAPL, MSFT, NVDA 등) |
| 합계 | 20개 대형주 | 고정 유니버스 (분기별 리밸런싱) |

### 전체 데이터 파이프라인

```
실시간 데이터 수집 (KIS API + Alpha Vantage + News Crawler)
        ↓
Feature Engineering Layer  — 10개 핵심 지표 계산
        ↓
Quant Scoring Engine       — 가중 점수 산출 (0~100점)
        ↓
Risk Filtering Layer       — 선반영/하락장/유동성 필터
        ↓
Market Regime Analysis     — 시장 국면 판단
        ↓
상위 3개 후보 선별
        ↓
RAG 기반 유사 패턴 검색    — Phase 7 인프라 재활용 (source_module="quant")
        ↓
LLM 분석 (Groq)            — 해석 + 리스크 + 최종 리포트
        ↓
Quant Alert 발송           — 텔레그램 + API 응답
```

---

## Phase 9 — [Quant] MVP: Feature Engineering + Scoring Engine ✅ 구현 완료 (2026-05-15)
# Last Updated: 2026-05-21

> **의존성**: Phase 7 (RAG 인프라) 완료 후 착수 권장. Phase 5 KIS API는 필수 선행 조건 (완료됨 ✅).
>
> **구현 완료**: 4개 MVP Feature (volume_ratio_5d, price_momentum_5d, news_freshness, market_regime),
> Quant Scoring Engine, Risk Filter, QuantUniverse, 5개 API, 일배치 스케줄러
>
> **Phase 9.5 추가 구현** (2026-05-21): 3개 신규 Feature (valuation_score, target_price_upside, sector_relative_strength),
> QuantFundamentalPort + KisFundamentalAdapter, 가중치 재조정 (총합 1.00), QuantEngineUseCase 2-pass 배치 구조

### 목표
KOR/USA 시총 TOP20 대상으로 정량 점수를 산출하고, 상위 3개 후보를 AI가 해석하는 최소 동작 파이프라인을 완성한다.

### 9-1. Feature Engineering Layer

10개 핵심 Feature를 실시간으로 수집·계산한다.

| Feature | 설명 | 데이터 소스 |
|---|---|---|
| `volume_ratio_5d` | 현재 거래량 / 5일 평균 거래량 (배수) | KIS API (이미 연동됨) |
| `foreign_buy_velocity` | 외국인 순매수 가속도 (3일 변화율, %) | KIS API |
| `institution_buy_velocity` | 기관 순매수 가속도 (3일 변화율, %) | KIS API |
| `sector_strength_score` | 동일 섹터 평균 대비 종목 강도 (0~100) | KRX 섹터 데이터 |
| `short_covering_score` | 공매도 잔고 감소율 — 숏커버링 가능성 (0~100) | KIS API / 금융위 |
| `market_regime` | 시장 국면 (BULL / BEAR / SIDEWAYS) | 지수 + VIX 기반 |
| `news_freshness_score` | 뉴스 신선도 점수 (최신 뉴스 가중치) | News Crawler |
| `price_in_risk` | 선반영 리스크 (급등 이후 뉴스인지, 0~1) | 주가 + 뉴스 타임스탬프 |
| `volume_acceleration` | 거래량 가속도 (분봉 증가 기울기) | KIS 실시간 |
| `price_momentum_15m` | 15분 단위 가격 모멘텀 (%) | KIS 실시간 |

```java
// Feature Engineering 인터페이스 (전략 패턴 — OCP 준수, Core의 NewsPort 패턴과 동일 원칙)
public interface QuantFeatureCalculator {
    QuantFeatureResult calculate(String ticker, MarketType market);
    FeatureType getFeatureType();
}

// 구현체 예시
VolumeRatio5dCalculator        // volume_ratio_5d  (KIS API 재활용)
ForeignBuyVelocityCalculator   // foreign_buy_velocity
MarketRegimeAnalyzer           // market_regime
```

### 9-2. Quant Scoring Engine

Feature 값에 가중치를 부여하여 0~100점 종합 Quant Score를 산출한다.

```java
public record QuantScore(
    String ticker,
    double totalScore,                    // 0 ~ 100
    Map<String, Double> featureScores,
    String marketRegime,
    LocalDateTime calculatedAt
) {}
```

**가중치 (MVP 기본값 — Phase 11에서 데이터 기반 자동 조정)**:

| Feature | 가중치 |
|---|---|
| volume_ratio_5d | 20% |
| foreign_buy_velocity | 18% |
| institution_buy_velocity | 15% |
| price_momentum_15m | 12% |
| volume_acceleration | 10% |
| sector_strength_score | 10% |
| short_covering_score | 8% |
| news_freshness_score | 5% |
| price_in_risk | -2% (패널티) |
| market_regime | 보정 계수 적용 |

### 9-3. Risk Filtering Layer

Quant Score가 높아도 다음 조건에 해당하면 후보에서 제외한다.

```yaml
risk_filters:
  - name: 선반영 필터
    condition: price_in_risk > 0.7 AND 최근_3일_상승률 > 15%
    action: EXCLUDE

  - name: 하락장 필터
    condition: market_regime == BEAR AND sector_strength_score < 40
    action: EXCLUDE

  - name: 거래정지 필터
    condition: 거래정지 or 관리종목
    action: EXCLUDE

  - name: 유동성 필터
    condition: volume_ratio_5d < 0.5
    action: EXCLUDE
```

### 9-4. 데이터 모델 (신규 테이블)

```
[quant_signal]                             -- Quant 시그널 결과 (TOP3)
- id                BIGINT PK
- ticker            VARCHAR(20)
- stock_name        VARCHAR(100)
- market_type       VARCHAR(10)            -- KOR | USA
- total_score       DECIMAL(5,2)           -- 0~100
- signal_rank       INT                   -- 1~3
- market_regime     VARCHAR(20)            -- STRONG_BULL | BULL | SIDEWAYS | BEAR | CRISIS
- llm_report        TEXT                   -- AI 해석 리포트
- disclaimer        TEXT                   -- 면책 문구 (필수)
- signal_date       DATE
+ BaseEntity 상속 (createdAt, updatedAt, deleted, deletedAt)

[quant_feature_snapshot]                   -- Feature 값 스냅샷
- id                        BIGINT PK
- quant_signal_id           BIGINT FK → quant_signal
- volume_ratio_5d           DECIMAL(6,2)
- foreign_buy_velocity      DECIMAL(6,2)
- institution_buy_velocity  DECIMAL(6,2)
- sector_strength_score     DECIMAL(5,2)
- short_covering_score      DECIMAL(5,2)
- news_freshness_score      DECIMAL(5,2)
- price_in_risk             DECIMAL(5,2)
- volume_acceleration       DECIMAL(6,2)
- price_momentum_15m        DECIMAL(6,2)
- raw_feature_json          TEXT           -- 원시값 전체 JSON (디버깅용)
- snapshot_at               TIMESTAMP

[quant_universe]                           -- 분석 대상 유니버스 (KOR+USA TOP20)
- id                BIGINT PK
- ticker            VARCHAR(20)
- stock_name        VARCHAR(100)
- market_type       VARCHAR(10)
- market_cap_rank   INT                   -- 1~10
- sector            VARCHAR(50)
- active            BOOLEAN DEFAULT true
- added_at          DATE
- removed_at        DATE (nullable)
```

### 9-5. MVP API 엔드포인트

```
GET  /api/quant/top-signals          # 오늘의 TOP3 Quant 시그널 조회
GET  /api/quant/scores               # 전체 유니버스 Quant Score 목록
GET  /api/quant/scores/{ticker}      # 특정 종목 Quant Score 상세 (Feature 값 포함)
GET  /api/quant/universe             # 현재 분석 유니버스 목록 조회
GET  /api/quant/regime               # 현재 시장 국면(Market Regime) 조회
```

### 9-6. Alpha Vantage 추가 연동

```yaml
# application.yml 추가
alpha-vantage:
  api-key: ${ALPHA_VANTAGE_API_KEY}
  base-url: https://www.alphavantage.co/query
```

```java
// 신규 Port (기존 StockPricePort와 독립)
public interface QuantDataPort {
    VolumeData fetchVolumeData(String ticker, MarketType market);
    InstitutionalFlowData fetchInstitutionalFlow(String ticker);
    ShortData fetchShortData(String ticker);
}

// 구현체
KisQuantDataAdapter          // KOR (KIS API — 기존 KisTokenManager 재활용)
AlphaVantageQuantAdapter     // USA (Alpha Vantage)
```

---

---

## Phase 9.5 — [Quant] Feature Engine 고도화 ✅ 구현 완료 (2026-05-21)

> **목표**: Phase 9 MVP의 Feature 3개 추가 (밸류에이션 / 목표주가 / 섹터 상대강도) 및 가중치 재조정

### 구현 내용

| Feature | 가중치 | 데이터 소스 | 상태 |
|---|---|---|---|
| `valuation_score` | 20% | KIS inquire-price (PER/PBR) | ✅ 구현됨 (KOR 전용) |
| `target_price_upside` | 15% | Stub (목표주가 null → 50점) | ✅ Stub 구현됨 |
| `sector_relative_strength` | 15% | QuantEngineUseCase 섹터 평균 사전 집계 | ✅ 구현됨 |

**가중치 재조정 (총합 0.85 → 1.00)**:

| Feature | Phase 9 | Phase 9.5 |
|---|---|---|
| volume_ratio_5d | 35% | 20% |
| price_momentum_5d | 30% | 20% |
| news_freshness | 20% | 10% |
| valuation_score | — | **20%** |
| target_price_upside | — | **15%** |
| sector_relative_strength | — | **15%** |

**핵심 신규 컴포넌트**:
- `QuantFundamentalData` record — PER/PBR/목표주가/현재가 (nullable, stub() 팩토리)
- `QuantFundamentalPort` + `KisFundamentalAdapter` — KIS inquire-price 기반 KOR 실시간 펀더멘털
- `ValuationScoreCalculator` / `TargetPriceUpsideCalculator` / `SectorRelativeStrengthCalculator`
- `QuantEngineUseCase` 2-pass 리팩토링: 섹터 평균 사전 집계 후 상대강도 계산

**Phase 10 연동 예정**:
- 목표주가: 네이버금융/증권사 API 스크래핑 (현재 null Stub)
- USA 펀더멘털: Alpha Vantage 실제 연동 (현재 Stub)

---

## Phase 10 — [Quant] Market Regime + RAG 통합

### 목표
시장 국면을 정교하게 분석하고, Phase 7에서 구축한 RAG 인프라와 Quant를 연결한다.

### 10-1. Market Regime Analysis

```java
public enum MarketRegime {
    STRONG_BULL,   // 강세장 (지수 +2% 이상, VIX 낮음)
    BULL,          // 상승장
    SIDEWAYS,      // 횡보
    BEAR,          // 하락장
    CRISIS         // 폭락장 (VIX 30 이상) → Quant 시그널 중지
}
```

| Regime | Quant 전략 | 가중치 조정 |
|---|---|---|
| STRONG_BULL | 모멘텀 추종 | volume_acceleration, momentum 가중치 ↑ |
| BULL | 기본 전략 | 기본값 유지 |
| SIDEWAYS | 수급 중심 | foreign_buy_velocity, institution ↑ |
| BEAR | 방어적 | risk_filter 강화, price_in_risk 패널티 ↑ |
| CRISIS | 시그널 중지 | 전체 유니버스 시그널 발송 중단 |

### 10-2. Time-Series Momentum Analysis

```java
public record MomentumResult(
    String ticker,
    double momentum_5m,   // 5분 모멘텀
    double momentum_15m,  // 15분 모멘텀 (핵심 지표)
    double momentum_60m,  // 60분 모멘텀
    TrendDirection trend  // UP | DOWN | FLAT
) {}
```

### 10-3. RAG 통합 — Phase 7 인프라 재활용

Phase 7에서 구축한 PGVector + Hybrid Search를 Quant에 연결한다.
`source_module: "quant"` 메타데이터 필터로 Core RAG와 완전히 분리.

```java
// Quant 패턴 벡터 저장 (Phase 7의 vectorStore 재활용)
vectorStore.add(List.of(
    new Document(featurePatternText, Map.of(
        "source_module",   "quant",          // ← Core와 구분하는 핵심 필터
        "ticker",          "005930",
        "sector",          "semiconductor",
        "signal_rank",     "1",
        "alpha_7d",        "+9.2",
        "is_failure",      "false",          // Phase 7 Negative Sampling 패턴 동일 적용
        "market_regime",   "BULL"
    ))
));

// Quant 유사 패턴 검색
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.query(currentFeatureText)
        .withTopK(3)
        .withFilterExpression("source_module == 'quant' AND sector == 'semiconductor'")
);
```

**Quant 전용 RAG 데이터 종류**:

| 데이터 | 설명 | Phase 7 연계 |
|---|---|---|
| 과거 급등 패턴 | Feature 조합 + 실제 결과 (Positive) | is_failure=false |
| 실패한 BUY 패턴 | 선반영, 허매수 등 Negative 사례 | is_failure=true |
| 외인 수급 성공/실패 | 외인 매수 → 실제 등락 결과 | alpha_7d 메타데이터 |
| 공시 급등 패턴 | DART 공시 유형 + 실제 반응 | Phase 7 dart 타입 연계 |

### 10-4. Price-In Detection (선반영 탐지)

```
뉴스 발생 타임스탬프 vs 주가 급등 타임스탬프 비교
→ 주가가 뉴스보다 먼저 움직인 경우: price_in_risk 높음
→ Quant Score 차감 + RAG에 "선반영 패턴 (Negative)" 저장
→ 다음 유사 패턴에서 자동 경고
```

---

## Phase 11 — [Quant] Feedback Loop + Alpha Tracking + Self-Correction

### 목표
Quant 시그널 정확도를 추적하고, Phase 8의 Feedback Loop 설계를 Quant에 확장 적용한다.

### 11-1. Quant Alpha Tracking

Phase 8의 `signal_feedback` 테이블 설계를 그대로 확장 활용한다.

```
[quant_alpha_result]                       -- Phase 8 signal_feedback과 동일 구조
- id               BIGINT PK
- quant_signal_id  BIGINT FK → quant_signal
- price_at_signal  DECIMAL(12,2)
- price_after_3d   DECIMAL(12,2)
- price_after_10d  DECIMAL(12,2)
- price_after_20d  DECIMAL(12,2)
- price_mdd        DECIMAL(12,2)           -- 기간 내 최저가
- alpha_3d         DECIMAL(6,2)
- alpha_10d        DECIMAL(6,2)            -- Primary 평가 지표
- alpha_20d        DECIMAL(6,2)
- mdd_pct          DECIMAL(6,2)
- benchmark_return DECIMAL(6,2)            -- KOR: KOSPI / USA: S&P500
- was_successful   BOOLEAN                 -- alpha_10d > 0
- is_failure       BOOLEAN                 -- alpha_10d < -5%
- rag_updated      BOOLEAN DEFAULT false
- evaluated_at     TIMESTAMP
```

스케줄러: Phase 8과 동일하게 T+3 / T+10 / T+20 영업일 후 자동 수집.

### 11-2. 가중치 Self-Correction

```java
// 월별 가중치 재조정 (50회 이상 누적 데이터 기반)
public interface WeightOptimizer {
    Map<FeatureType, Double> optimize(List<QuantAlphaResult> history);
}
```

- 성공 시그널에서 높게 기여한 Feature → 가중치 소폭 증가
- 실패 시그널에서 높게 기여한 Feature → 가중치 소폭 감소
- **50회 미만**: 수동 설정값 유지 / **50회 이상**: 자동 최적화 활성화

### 11-3. Quant Alert System

```yaml
alert_conditions:
  - name: 실시간 급등 경보
    trigger: volume_ratio_5d > 3.0 AND price_momentum_15m > 2%
    channel: Telegram
    priority: HIGH

  - name: 외인 대량 매수 경보
    trigger: foreign_buy_velocity > 2.0  # 전일 대비 2배 이상
    channel: Telegram
    priority: HIGH

  - name: 장전 TOP3 브리핑
    trigger: cron("30 8 * * MON-FRI")   # 매 거래일 08:30
    channel: Telegram
    priority: NORMAL

  - name: 숏커버링 포착
    trigger: short_covering_score > 80
    channel: Telegram
    priority: HIGH
```

---

## 활용 시나리오 (Quant)

### ① 장중 실시간 급등 탐지
```
09:30 장 개시
→ KIS API 거래량 수집 (기존 KisTokenManager 재활용)
→ volume_ratio_5d = 4.2 감지 (삼성전자)
→ foreign_buy_velocity = STRONG (외인 대규모 유입)
→ Quant Score: 87점 / 시장 1위
→ Risk Filter 통과 (price_in_risk 낮음)
→ RAG 검색: "2024-11 유사 패턴 → D+10 Alpha +9.2%" (source_module=quant)
→ LLM: "외인 수급 + 거래량 급증. 선반영 리스크 낮음. 단기 모멘텀 유효."
→ 텔레그램 알림 발송
```

### ② 선반영 패턴 자동 차단
```
뉴스: "XX 바이오 FDA 승인 소식"
→ 주가 이미 3일 전부터 +18% 상승
→ price_in_risk = 0.91 (선반영 고위험)
→ Risk Filter: EXCLUDE
→ Quant 후보에서 제외 (긍정 뉴스에도 시그널 미발생)
→ RAG에 "선반영 패턴 (Negative)" 저장 → 다음 유사 케이스에 경고
```

### ③ 섹터 로테이션 감지
```
반도체 섹터 전체 volume_ratio_5d 급등
→ sector_strength_score 상위 반도체 종목 특정
→ market_regime = BULL → 기본 가중치 유지
→ Quant Score 계산 → TOP3 선별
→ RAG: "섹터 로테이션 유사 패턴 → 평균 D+10 Alpha +7.3%" 삽입
→ LLM 리포트 + 텔레그램 발송
```
