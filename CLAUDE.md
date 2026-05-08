# Jurine — AI 주식 투자 시그널 서비스

이 문서는 Claude Code(CLI)가 Jurine 프로젝트를 개발할 수 있도록 작성된 컨텍스트 가이드다.

---

## 프로젝트 개요

주린이(주식 초보자)를 위한 AI 기반 투자 시그널 서비스.  
뉴스 크롤링 + 주가 흐름 데이터를 AI로 종합 분석하여 **🟢 매수 / 🟡 관망 / 🔴 매도** 시그널을 직관적으로 제공한다.

---

## 기술 스택

| 영역 | 선택 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4+ |
| AI Library | Spring AI (OpenAI 호환 API) |
| LLM Engine | Groq (Llama 3) |
| 뉴스 크롤링 | Jsoup |
| 주가 데이터 | Yahoo Finance API (최근 1개월) |
| Database | PostgreSQL + JPA/Hibernate |
| 실시간 통신 | SSE (Server-Sent Events) |
| 알림 | 텔레그램 봇 |
| 스케줄링 | Spring `@Scheduled` |
| 빌드 | Gradle |

---

## 핵심 기능

### 1. 온디맨드 시그널 분석
- 사용자가 종목명/티커 검색 시 즉시 실행
- 네이버 금융(KOR) 또는 Yahoo Finance(USA) 뉴스 10건 크롤링
- Yahoo Finance API로 최근 1개월 주가 흐름 수집
- Groq AI가 뉴스 + 주가를 종합 분석 → 시그널 생성
- SSE 스트리밍으로 실시간 응답
- 분석 완료 후 DB 저장

### 2. 모닝 브리핑 (자동 배치)
- 08:50 (KST): 한국 장 시작 전 — 보유/관심 종목 전체 분석
- 22:20 (KST): 미국 장 시작 전 — 미국 종목만 분석
- 분석 완료 후 텔레그램 봇으로 결과 자동 전송

### 3. 종목 관리
- **보유 종목**: 종목명, 수량, 평균 매수가 (수익률 계산용)
- **관심 종목**: 종목명만
- 한국(KOR) / 미국(USA) 시장 구분
- 보유 종목 수익률을 시그널 판단 맥락에 반영

---

## 시그널 출력 포맷

AI가 생성하는 시그널은 아래 구조를 따른다.

```
## 삼성전자 (005930) — 2025.05.07 08:50

### 🔴 매도 신호

**판단 근거**
- 미국 반도체 수출 규제 강화 뉴스 2건 확인
- 중국 매출 비중 높아 직접 타격 예상
- 최근 1개월 주가 -8% 하락 추세 지속

---
⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.
```

`signal_type`은 반드시 `BUY | HOLD | SELL` 중 하나로 파싱하여 DB에 저장한다.

---

## 패키지 구조

```
com.jurine
├── domain
│   ├── stock           # 종목 관련 (entity, repository)
│   ├── portfolio       # 보유/관심 종목 관리
│   ├── signal          # 시그널 분석 결과
│   └── price           # 주가 스냅샷
├── crawler
│   ├── NaverFinanceCrawler.java   # 한국 주식 뉴스 (KOR)
│   └── YahooFinanceCrawler.java   # 미국 주식 뉴스 (USA)
├── ai
│   └── SignalAnalysisService.java # Groq AI 분석 파이프라인
├── scheduler
│   └── MorningBriefingScheduler.java
├── telegram
│   └── TelegramNotifier.java
└── api
    ├── StockAnalysisController.java
    └── PortfolioController.java
```

---

## 데이터 모델 (ERD)

```
[stock]
- stock_id      BIGINT PK
- ticker        VARCHAR UNIQUE
- name          VARCHAR
- market_type   ENUM('KOR', 'USA')
- is_watchlist  BOOLEAN  -- 관심 종목 여부
- created_at    TIMESTAMP

[portfolio_detail]
- id            BIGINT PK
- stock_id      BIGINT FK → stock
- quantity      INT
- avg_price     DECIMAL(10,2)  -- 평균 매수가
- created_at    TIMESTAMP

[signal_report]
- report_id     BIGINT PK
- stock_id      BIGINT FK → stock
- signal_type   ENUM('BUY', 'HOLD', 'SELL')
- reason        TEXT           -- AI 판단 근거 3줄
- content       TEXT           -- AI 응답 전문 (마크다운)
- raw_news_text TEXT           -- 크롤링 원문 (재분석 대비 보존)
- source_type   ENUM('SCHEDULED', 'ON_DEMAND')
- created_at    TIMESTAMP

[price_snapshot]
- id            BIGINT PK
- stock_id      BIGINT FK → stock
- close_price   DECIMAL(10,2)
- recorded_date DATE
```

---

## API 엔드포인트

### 종목 분석
```
GET  /api/stock/analyze/{ticker}         # 온디맨드 분석 (SSE 스트리밍)
GET  /api/reports/today                  # 오늘 배치 시그널 목록
GET  /api/reports/{stockId}/history      # 종목별 시그널 히스토리
```

### 종목 관리
```
POST   /api/portfolio                    # 보유 종목 등록 { ticker, quantity, avgPrice }
GET    /api/portfolio                    # 보유 종목 목록 + 현재 수익률
DELETE /api/portfolio/{id}              # 보유 종목 삭제
POST   /api/watchlist                   # 관심 종목 등록 { ticker }
DELETE /api/watchlist/{id}             # 관심 종목 삭제
```

---

## 크롤러 전략 패턴

```java
// OCP 준수 — 새 크롤러 추가 시 기존 코드 수정 없이 구현체만 추가
public interface NewsCrawlerStrategy {
    List<String> crawl(String ticker);
    MarketType getSupportedMarket();
}

// 구현체
NaverFinanceCrawler  implements NewsCrawlerStrategy  // market_type = KOR
YahooFinanceCrawler  implements NewsCrawlerStrategy  // market_type = USA
```

---

## AI 분석 파이프라인

```
Input  : 뉴스 원문 텍스트 + 최근 1개월 주가 흐름 데이터
Model  : Groq (Llama 3) via Spring AI ChatClient
Output : Markdown 형식 시그널 리포트 (SSE 스트리밍)

System Prompt 핵심 지시사항:
1. signal_type을 반드시 [BUY], [HOLD], [SELL] 중 하나로 명시
2. 판단 근거를 3줄 이내로 간결하게 작성
3. 주가 흐름과 뉴스 감성을 모두 고려하여 종합 판단
4. 마지막 줄에 반드시 면책 문구 포함
```

---

## 스케줄러

```java
@Scheduled(cron = "0 50 8 * * MON-FRI")   // 평일 08:50 KST — 한국 장
@Scheduled(cron = "0 20 22 * * MON-FRI")  // 평일 22:20 KST — 미국 장 (서머타임 기준)
```

- 배치 실행 시 전체 보유 + 관심 종목 대상
- 22:20 배치는 `market_type = USA` 종목만 처리
- 분석 완료 후 텔레그램 봇으로 결과 전송

---

## 개발 원칙

- **Clean Architecture**: Controller → Service → Repository 계층 엄격 분리
- **전략 패턴**: 크롤러는 `NewsCrawlerStrategy` 인터페이스로 추상화 (OCP 준수)
- **에러 처리**: 크롤링 실패, Groq 타임아웃(30초) 시 Fallback 예외 처리 필수
- **재시도**: Groq API 실패 시 1회 재시도 후 실패 메시지 반환
- **Rate Limiting**: Yahoo Finance API 중복 요청 방지를 위한 캐싱 적용
- **로깅**: SLF4J 기반으로 크롤링 상태, AI 응답 시간, 배치 실행 결과 상세 로깅
- **원문 보존**: `raw_news_text` 반드시 저장 (프롬프트 개선 시 재분석 가능하도록)
- **면책 문구**: 모든 시그널 응답 마지막에 반드시 포함

---

## 개발 로드맵

| Phase | 목표 | 핵심 작업 |
|---|---|---|
| Phase 1 | 온디맨드 시그널 | 크롤링 → 주가 수집 → AI 분석 → SSE 스트리밍 |
| Phase 2 | 종목 관리 | 보유/관심 종목 CRUD, 평균 매수가 입력 |
| Phase 3 | 모닝 브리핑 | 배치 스케줄러 + 텔레그램 봇 알림 |
| Phase 4 | 고도화 | 수익률 연동 시그널, 시그널 히스토리 조회 |

**현재 목표: Phase 1부터 시작**

---

## 환경변수 (application.yml 참고)

```yaml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai
      chat:
        options:
          model: llama3-8b-8192

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  chat-id: ${TELEGRAM_CHAT_ID}
```

---

## 다음 단계

Phase 1~4 완료 후 벡터 DB(PGVector) 및 RAG 기반 고도화 계획은 `CLAUDE_NEXT.md` 참고.
