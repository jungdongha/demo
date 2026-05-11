# Jurine — AI 주식 투자 시그널 서비스

주린이(주식 초보자)를 위한 AI 기반 투자 시그널 서비스.
뉴스 크롤링 + 주가 흐름 데이터를 Groq AI로 분석하여 **매수 / 관망 / 매도** 시그널을 제공한다.

> 상세 규칙·아키텍처·데이터모델은 `.claude/` 디렉토리 참고.

---

## 현재 진행 단계

| Phase | 목표 | 상태 |
|---|---|---|
| Phase 1 | 온디맨드 시그널 (SSE 스트리밍) | 완료 |
| Phase 2 | 종목 관리 (보유/관심 CRUD) | 완료 |
| Phase 3 | 모닝 브리핑 (배치 + 텔레그램) | 진행 중 |
| Phase 4 | 시그널 히스토리 고도화 | 대기 |

---

## 시그널 출력 포맷

AI 응답은 반드시 아래 구조를 따른다. `signal_type`은 `BUY | HOLD | SELL` 중 하나로 파싱하여 DB 저장.

```
## {종목명} ({ticker}) — {날짜} {시간}

### 🟢 매수 신호 | 🟡 관망 신호 | 🔴 매도 신호

**판단 근거**
- {뉴스/주가 근거 1}
- {뉴스/주가 근거 2}
- {뉴스/주가 근거 3}

---
⚠️ 본 시그널은 AI 참고 정보입니다. 투자 판단과 책임은 전적으로 사용자에게 있습니다.
```

---

## 크롤러 전략 패턴

```java
// OCP 준수 — 새 크롤러 추가 시 구현체만 추가
public interface NewsCrawlerStrategy {
    List<String> crawl(String ticker);
    MarketType getSupportedMarket();
}
// KOR: NaverFinanceCrawler (Naver News API)
// USA: YahooFinanceCrawler (Yahoo Finance RSS)
```

---

## Phase 5+ 고도화 로드맵

RAG, PGVector, KIS API 도입 계획은 `.claude/docs/roadmap.md` 참고.
