# Quant Platform — AI 기반 퀀트 주식 분석 플랫폼

주식 투자자를 위한 다전략 퀀트 분석 서비스.
CAN SLIM, 미네르비니, 가치투자 등 7가지 검증된 투자 전략을 Rule 기반으로 수치화하여
종목별 전략 점수 및 기술적·재무적 분석을 제공한다.

> 상세 규칙·아키텍처·데이터모델은 `.claude/` 디렉토리 참고.

---

## 핵심 목표

- **Explainable Quant**: AI 블랙박스 최소화 → Rule 기반 점수 + 근거 제시
- **7개 투자 전략** 동시 점수화 (0~100점)
- **기술적 + 재무적 + 수급** 3축 분석
- AI는 선택적 보조 (버튼 클릭 시만 호출, 캐싱 기반)
- React(Vite) 프론트 + Spring Boot API 구조

---

## 기술 스택

```yaml
backend:
  java: 21
  spring_boot: 3.4+
  db: H2 (dev) / PostgreSQL (prod)
  ai: Spring AI + Groq (선택적 보조만)
  stock_price: KIS API (KOR + USA)
  disclosure: DART API (KOR)
  news_kr: Naver News API
  news_us: Yahoo Finance RSS
  scheduler: Spring @Scheduled

frontend:
  framework: React (Vite)
  language: TypeScript
  styling: Tailwind CSS
  http: Axios
  charts: Recharts
```

---

## 7가지 투자 전략

| 전략 | 핵심 데이터 | 출력 |
|---|---|---|
| **CAN SLIM** | EPS/매출 성장, 거래량, 신고가, 수급 | CAN SLIM Score (0~100) |
| **듀얼 모멘텀** | 기간별 수익률, 상대강도 | Momentum Score (0~100) |
| **시즌성** | 월별 수익률, 업종 사이클 | Seasonality Score (0~100) |
| **미네르비니** | 이동평균 정배열, RS, 신고가, 거래량 | Trend Score (0~100) |
| **Magic Formula** | ROIC, Earnings Yield, PER | Value Rank (0~100) |
| **Piotroski F-Score** | ROA, 현금흐름, 부채비율, 유동성 | F-Score (0~9 → 0~100) |
| **Mean Reversion** | RSI, Bollinger Band, 이격률 | Reversion Score (0~100) |

---

## 분석 3축

```
기술적 분석          재무 분석           수급 분석
─────────────        ─────────────        ─────────────
RSI                  PER / PBR            기관 순매수
MACD                 ROE / ROA            외국인 순매수
EMA / SMA            부채비율             거래량 변화
Bollinger Band       영업이익률           거래대금
ATR / ADX            EPS 성장률           상대강도(RS)
```

---

## 시스템 구조

```text
[KIS API / DART API / 뉴스 API]
            ↓
      데이터 수집 레이어
            ↓
    7개 전략 엔진 (Rule 기반)
            ↓
    기술적·재무·수급 계산기
            ↓
    선택적 AI 해석 (캐싱)
            ↓
    Spring Boot REST API
            ↓
    React (Vite) 프론트
```

---

## Provider 전략 패턴 (OCP 준수)

```java
// 주가 시세
public interface StockPricePort {
    PriceSnapshot fetchCurrentPrice(String ticker, MarketType market);
    List<PriceSnapshot> fetchDailyPrices(String ticker, MarketType market, int days);
}
// KOR: KisKorStockPriceProvider / USA: KisUsaStockPriceProvider

// 기업 공시
public interface CorporateDisclosurePort {
    List<DisclosureItem> fetchRecentDisclosures(String dartCorpCode, int limit);
}
// KOR: DartDisclosureProvider

// 전략 엔진
public interface StrategyCalculator {
    StrategyScore calculate(StrategyInput input);
    StrategyType getSupportedStrategy();
}
// 7개 전략별 Calculator 구현체 (CANSLIM / DUAL_MOMENTUM / SEASONALITY /
//   MINERVINI / MAGIC_FORMULA / PIOTROSKI / MEAN_REVERSION)
```

---

## 로드맵 현황

| Phase | 목표 | 상태 |
|---|---|---|
| Phase 1 | 데이터 수집 인프라 정비 (KIS + DART 재활용) | 완료 |
| Phase 2 | 기술적 분석 엔진 (RSI / MACD / Bollinger 등) | 대기 |
| Phase 3 | 재무 분석 엔진 (PER / ROE / EPS 등) | 대기 |
| Phase 4 | 7개 전략 Score 계산기 구현 | 대기 |
| Phase 5 | Spring Boot REST API 설계 및 구현 | 대기 |
| Phase 6 | React(Vite) 프론트 종목 분석 페이지 | 대기 |
| Phase 7 | 수급 분석 (기관 / 외국인 + 거래량) | 대기 |
| Phase 8 | AI 선택적 해석 (캐싱, 최소 호출) | 대기 |
| Phase 9 | 종목 비교 + 전략 필터링 | 대기 |
