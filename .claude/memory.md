# Quant Platform — Agent Memory
# Last Updated: 2026-05-26

## 프로젝트 전환 요약

**이전 프로젝트**: Jurine (AI BUY/HOLD/SELL 시그널 서비스)
**현재 프로젝트**: Quant Platform (다전략 퀀트 분석 플랫폼)
**결정 사항**:
- 인프라 재사용: KIS API 연동, DART API, Layered DDD 아키텍처
- 도메인 로직 재설계: 전략 엔진 중심
- 프론트 추가: React (Vite) + TypeScript + Tailwind CSS - 프론트는 따로 진행
- AI 역할 축소: 선택적 보조 (Phase 8, 버튼 클릭 시만)

---

## 현재 상태

```yaml
phase: 문서 재설계 완료 → Phase 2 (기술적 분석 엔진) 대기
branch: main
base_package: com.obigo.demodong
```

---

## 재사용 가능한 기존 인프라

| 컴포넌트 | 위치 | 재사용 여부 |
|---|---|---|
| KisTokenManager | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| KisKorStockPriceProvider | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| KisUsaStockPriceProvider | domain/price/infrastructure/kis | ✅ 그대로 사용 |
| DartDisclosureProvider | domain/price/infrastructure/dart | ✅ 그대로 사용 |
| DartCorpCodeMapper | domain/price/infrastructure/dart | ✅ 그대로 사용 |
| StockPricePort | domain/price/domain/port | ✅ 그대로 사용 |
| CorporateDisclosurePort | domain/price/domain/port | ✅ 그대로 사용 |
| PriceSnapshot | domain/price/domain/entity | ✅ 그대로 사용 |
| Stock | domain/stock | ✅ 그대로 사용 |

---

## 핵심 설계 결정 (재설계 반영)

### 1. 전략을 데이터 의존도 기준으로 분산 배치
7개를 한 Phase에 몰지 않고, 데이터가 준비되는 시점에 바로 붙임.

| 전략 | 필요 데이터 | 구현 Phase |
|---|---|---|
| Mean Reversion | 기술적만 (RSI/BB/이격률) | Phase 2 |
| Minervini | 기술적만 (EMA 정배열, RS) | Phase 2 |
| Dual Momentum | 수익률 + 시장국면 | Phase 3 |
| Seasonality | 업종 계절성 테이블 (하드코딩) | Phase 3 |
| Piotroski F-Score | 재무만 (DART) | Phase 4 |
| CAN SLIM | 재무 + 기술 + 수급 | Phase 5 |
| Magic Formula | 유니버스 전체 상대 순위 | Phase 6 |

### 2. Magic Formula는 유니버스 배치로 처리
단일 종목 입력으로는 순위를 못 매김. Phase 6에서 배치 스케줄러로
사전 계산된 순위를 DB에 저장 → 조회 시 캐시 반환.

### 3. Seasonality는 사전 테이블 기반
과거 데이터 누적 없이도 동작하도록 업종별 계절성 점수를 
하드코딩 테이블로 관리. 나중에 실데이터로 교체 가능한 구조.

### 4. 미국 주식 범위 명확히 제한
- 기술적 지표 기반 전략만 지원 (Mean Reversion, Minervini, Dual Momentum, Seasonality)
- 재무 의존 전략 (Piotroski, CAN SLIM, Magic Formula)은 한국만
- 이유: DART = 한국 전용, Alpha Vantage 무료 = 분당 5회 제한

### 5. 프론트를 Phase 7에서 API와 동시 진행
Phase 2~6 결과물을 빠르게 확인하려면 API + 프론트를 한 Phase에서 묶는 게 현실적.

### 6. 각 Phase에 단위 테스트 의무화
금융 계산 버그는 틀린 숫자를 조용히 보여줌. Calculator 1개당 최소 5개 케이스.

---

## Phase 로드맵

### Phase 1 ✅ 완료 (인프라)
KIS API + DART API + Stock 엔티티

### Phase 2 — 기술적 분석 엔진 + 전략 2개 (다음 작업)
- TechnicalSnapshot record
- RSI / MACD / Bollinger / EMA / SMA / ATR / ADX Calculator
- **Mean Reversion** 전략 (RSI+BB+이격률 → 점수)
- **Minervini** 전략 (EMA 정배열 8조건)
- 단위 테스트 Calculator별 5개 이상

### Phase 3 — 모멘텀 + 시장 국면 + 전략 2개
- 기간별 수익률 (1/3/6/12개월)
- 상대강도(RS) 계산
- Market Regime (BULL/SIDEWAYS/BEAR)
- **Dual Momentum** 전략
- **Seasonality** 전략 (업종 계절성 하드코딩 테이블)

### Phase 4 — 재무 분석 엔진 + 전략 1개 (한국 완전지원)
- DART 재무제표 파싱 (계정과목 매핑 테이블 관리)
- FundamentalSnapshot record
- **Piotroski F-Score** 전략 (재무 준비 즉시 구현)
- 미국: FundamentalSnapshot.stub() 반환

### Phase 5 — 수급 분석 + 전략 1개
- KIS 투자자별 매매동향 API 연동
- FlowSnapshot record
- **CAN SLIM** 전략 (재무 + 기술 + 수급 통합)

### Phase 6 — Magic Formula (유니버스 배치)
- QuantUniverse 관리 (KOR 최소 100종목)
- 배치 스케줄러로 전체 ROIC/EarningsYield 순위 사전 계산
- **Magic Formula** 전략 (상대 순위 DB 조회)
- 이 시점에 7개 전략 전부 완성

### Phase 7 — REST API + React 프론트 (동시)
- Spring Boot API 7개 전략 전부 노출
- React(Vite) 종목 분석 페이지 UI
- API + 프론트 같이 진행

### Phase 8 — AI 선택적 해석
- POST 요청 시만 Groq 호출
- ai_report 테이블 캐싱 (ticker + date, TTL 24h)
- 7개 전략 점수 기반 종합 해석 생성

### Phase 9 — 비교 + 필터링
- 최대 3개 종목 비교
- 전략 점수 기반 필터링 스크리닝

---

## 현재 환경

```yaml
db: PostgreSQL (prod)
ai: Groq API (llama-3.1-8b-instant) — Phase 8에서만 사용
port: 8090 (backend) / 5173 (frontend dev)
news_kr: Naver News API
news_us: Yahoo Finance RSS
stock_price: KIS API (KOR + USA)
corporate_disclosure: DART API (KOR 전용)
```

---

## 주요 원칙

- KIS API: 실전/모의 전환은 application.yml kis.base-url로만
- DART API: dart_corp_code는 Stock 테이블 저장, KOR만
- Soft Delete: BaseEntity.softDelete() 항상 사용
- 전략 점수: 0~100 정규화, null → 50점 중립 (패널티 없음)
- API 기본경로: /api
- CORS: 개발환경 localhost:5173 허용
- 미국 주식: 기술적 전략만 지원 (Phase 4 이후 재무 의존 전략 제외)
