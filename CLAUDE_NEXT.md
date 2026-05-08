# Jurine — Phase 5+ 고도화 로드맵 (벡터 DB & RAG)

이 문서는 뉴스 기반 시그널(Phase 1~4) 완성 이후의 다음 단계 목표다.  
현재 프로젝트와 분리하여 별도로 관리한다.

---

## 현재 vs 다음 단계

| 구분 | 현재 (Phase 1~4) | 다음 단계 (Phase 5+) |
|---|---|---|
| 판단 근거 | 뉴스 텍스트 + 주가 흐름 | 뉴스 + 주가 + 공시 + 과거 시그널 + 거시지표 |
| AI 방식 | 단순 프롬프트 | RAG (검색 증강 생성) |
| 데이터 저장 | PostgreSQL (관계형) | PostgreSQL + PGVector (벡터 확장) |
| 시그널 근거 | AI 즉흥 판단 | 유사 과거 케이스 기반 판단 |

---

## Phase 5 — 도메인 지식 강화 (프롬프트 고도화)

### 목표
뉴스만으로 판단하던 AI에게 **섹터 특성, 거시경제 맥락**을 주입해 시그널 품질을 높인다.

### System Prompt 구조 고도화

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

[출력 규칙]
- signal_type: 반드시 [BUY] [HOLD] [SELL] 중 하나 명시
- 판단 근거: 3줄 이내, 뉴스와 주가 흐름 모두 언급
- 마지막 줄: 면책 문구 필수

[입력 데이터]
- 종목: {ticker} / {name} / {sector}
- 뉴스: {crawledNews}
- 주가 흐름 (1개월): {priceData}
- 보유 수익률: {profitRate} (보유 종목인 경우만)
- 참고 컨텍스트: {ragContext}  ← Phase 5에서 추가
```

### 데이터 소스 (무료)

| 목적 | 소스 | URL |
|---|---|---|
| 섹터/업종 분류 | KRX 종목 정보 | http://data.krx.co.kr |
| 금리/환율/거시지표 | 한국은행 ECOS API | https://ecos.bok.or.kr |
| 기업 공시 원문 | DART 전자공시 API | https://opendart.fss.or.kr |

---

## Phase 6 — 벡터 DB 도입 (PGVector + RAG)

### 목표
과거 뉴스, 공시, 시그널 결과를 벡터로 저장하고, 현재 상황과 **유사한 과거 케이스를 검색**해 시그널 근거를 강화한다.

### 기술 선택: PGVector

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
| DART 공시 (사업보고서 청크) | DART API | 기업 재무/사업 맥락 주입 |
| 과거 시그널 + 실제 주가 결과 | 자체 DB | "이 시그널이 맞았나" 근거 |
| 금융 용어 사전 | 직접 구축 | 용어 해설 자동 삽입 |

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

## 활용 시나리오 요약

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

### ③ 금융 용어 자동 해설
```
AI 응답에 "PBR", "어닝 서프라이즈" 등장
→ 용어 사전 벡터에서 유사 용어 검색
→ 응답 하단에 용어 해설 자동 추가
```

---

## 진입 조건

> **Phase 1~4 (뉴스 기반 시그널)가 안정적으로 동작한 이후 진행한다.**

- [ ] 온디맨드 시그널 정상 동작
- [ ] 모닝 브리핑 배치 안정화
- [ ] 시그널 히스토리 1개월 이상 누적
- [ ] 위 조건 충족 시 Phase 5 착수
