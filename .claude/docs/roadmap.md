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

## Phase 8 — 피드백 루프 (시그널 품질 자동 고도화)
# Last Updated: 2026-05-13

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

