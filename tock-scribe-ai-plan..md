# StockScribe AI: 지능형 주식 투자 인사이트 서비스 기획안

이 문서는 AI 코딩 어시스턴트(Claude Code, Cursor 등)가 주식 뉴스 분석 및 학습 서비스를 개발할 수 있도록 작성된 가이드라인이다.

## 1. 프로젝트 개요
- **목표**: 실시간 뉴스 크롤링과 Spring AI(Groq)를 결합하여, 초보 투자자에게 전문적인 분석과 용어 해설을 제공하는 백엔드 서비스 구축.
- **타겟**: 주식 초보자 및 본인 (나홀로 사용 서비스).
- **핵심 가치**: 전문적인 분석 리포트 제공 + 금융 문해력 향상(용어 사전).

## 2. 기술 스택 (Tech Stack)
- **Language**: Java 21
- **Framework**: Spring Boot 3.4+
- **AI Library**: Spring AI (OpenAI 호환 API 사용)
- **LLM Engine**: Groq (Llama 3 모델 등 활용)
- **Crawling**: Jsoup (정적 웹 파싱)
- **Database**: MySQL 또는 PostgreSQL (JPA/Hibernate)
- **Communication**: SSE (Server-Sent Events)를 통한 실시간 스트리밍 응답

## 3. 핵심 시스템 아키텍처

### 3.1. 종목 매핑 (Stock Dictionary)
- 사용자가 입력한 종목명(예: 구글, 삼전)을 고유 티커(Ticker)와 시장 타입(KOR/USA)으로 변환하는 사전(Dictionary) 계층.
- **Data Structure**: 초기 MVP 단계에서는 Spring Boot 내부의 In-memory Map(`@PostConstruct` 활용)으로 간단히 구현.

### 3.2. 크롤러 전략 패턴 (Crawler Strategy Pattern)
- `NewsCrawlerStrategy` 인터페이스를 정의하고 각 시장별 구현체 작성 (OCP 개방-폐쇄 원칙 준수).
- `NaverFinanceCrawler`: 한국 주식 뉴스 수집 (네이버 금융 타겟).
- `YahooFinanceCrawler`: 미국 주식 뉴스 수집 (야후 파이낸스 타겟).

### 3.3. AI 분석 파이프라인 (AI Pipeline)
- **Input**: 크롤러가 수집한 뉴스 원문 텍스트.
- **System Prompt**: 15년 차 애널리스트이자 친절한 튜터 페르소나 부여. 전문가용 분석 리포트 작성 후, 마크다운 구분선(`---`) 아래에 본문에 사용된 어려운 금융 용어 해설 포함 지시.
- **Output**: Markdown 형식의 스트리밍 텍스트 데이터.

## 4. 데이터베이스 설계 (ERD)
- `stock`: `stock_id`(PK), `ticker`(Unique), `name`, `market_type`
- `news_report`: `report_id`(PK), `stock_id`(FK), `content`(전체 AI 응답 본문), `created_at`

## 5. 단계별 개발 로직 (Roadmap)

### Step 1: 도메인 및 DB 세팅
- JPA Entity 클래스(`Stock`, `NewsReport`) 및 Repository 인터페이스 생성.

### Step 2: 크롤링 모듈 구현
- 전략 패턴을 활용하여 Jsoup 기반의 `NaverFinanceCrawler` 및 `YahooFinanceCrawler` 뼈대 코드 작성.

### Step 3: 온디맨드 검색 및 SSE 스트리밍 (핵심)
- 사용자가 종목 검색 시 즉시 타겟 사이트 뉴스를 크롤링하고 Groq API로 분석 시작.
- Spring AI `ChatClient`의 `.stream()` 메서드를 활용하여 응답을 한 글자씩 SSE로 클라이언트에게 전송.
- **Controller Endpoint**: `GET /api/stock/analyze/{ticker}` (반드시 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` 설정)
- 스트리밍 완료(`doOnComplete`) 시 전체 결과값을 조립하여 DB에 영구 저장.

### Step 4: 배치 시스템 (오전 9시)
- Spring `@Scheduled`를 활용하여 매일 오전 9시, 주요 관심 종목(삼성전자, SK하이닉스, 알파벳 등)을 자동으로 크롤링 및 분석하여 DB에 사전 적재.

## 6. 백엔드 개발 원칙
- **Clean Architecture**: 엔드포인트(Controller), 비즈니스 로직(Service), 데이터 접근(Repository) 계층을 엄격히 분리한다.
- **Error Handling**: 뉴스 검색 실패 또는 AI API 연동(Groq 타임아웃 등) 실패 시 적절한 Fallback 예외 처리 로직 추가.
- **Logging**: 나홀로 사용하는 서비스이므로 디버깅이 쉽도록 크롤링 상태, AI 응답 시간 등을 상세히 로깅(SLF4J)할 것.