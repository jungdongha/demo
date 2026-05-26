# Quant Platform Agent Context
> **Boot:** Read `.claude/manifest.md` first.

## Index

### 1. Core (Must Read)
- `core/essential-rules.yaml`: Non-negotiable Rules & Tech Stack
- `memory.md`: Current Context, Phase Status, Reuse Map

### 2. Design (Read on Demand)
- `core/system-design.yaml`: Architecture, 7-Strategy Engine, API Design, Frontend Structure

### 3. References (Lazy Load)
- `references/conventions/coding-style.yaml`: Java idioms, naming, DI basics
- `references/conventions/testing-guide.yaml`: JUnit 5 practices
- `references/data-model/`: Domain Entities

### 4. Docs (Read when writing/reviewing specs)
- `docs/spec-writing-guide.yaml`: Spec document format
- `docs/templates/feature-spec.yaml`: Feature spec template
- `docs/roadmap.md`: Phase 1~9 전체 로드맵
  - Phase 1: 인프라 정비 (KIS + DART 재사용) ✅
  - Phase 2~3: 기술적·재무적 분석 엔진
  - Phase 4: 7개 전략 Score 계산기
  - Phase 5: REST API
  - Phase 6: React(Vite) 프론트
  - Phase 7~9: 수급 분석, AI 보조, 비교/필터링

## Skills
- `/feature`: New Feature (Immutable Spec)
- `/fix`: Bug Fix (Root Cause Analysis)
- `/refactor`: Code Improvement (No logic change)
- `/docs`: Documentation Maintenance

## Agent Behavior (Protocol)

### Ask Protocol
요구사항이 모호하거나 위험(삭제 등)하거나 규칙 위반 시 → 즉시 STOP하고 사용자에게 확인

### 도메인 분리 원칙
- `domain/technical`, `domain/fundamental`, `domain/flow`, `domain/analysis`: 독립 도메인
- 도메인 간 직접 의존 금지 → `StrategyInput` record로만 데이터 교환
- `domain/price` (KIS 연동): 기존 코드 그대로 재사용, 수정 최소화

### 전략 계산기 원칙
- `StrategyCalculator` 인터페이스 구현 필수
- null 데이터 → 50점 중립 (0점 패널티 금지)
- 각 항목 점수 `Map<String, Integer> detail`에 반드시 기록 (설명 가능한 퀀트)

### Frontend 원칙
- 위치: 프로젝트 루트 `/frontend` 디렉토리
- Spring Boot와 별도 실행 (dev: port 5173, Vite proxy → 8090)
- API 호출: `frontend/src/api/analysisApi.ts` 집중 관리
- 컴포넌트: 전략 카드 1개 = `StrategyScoreCard.tsx` (재사용)

### AI 사용 원칙
- 분석 필수 경로에 AI 금지 (Rule 기반만)
- `POST /api/analysis/{ticker}/ai-report` 요청 시만 Groq 호출
- 캐시 먼저 확인, 없으면 Groq 호출 후 저장
