# Jurine Agent Context
> **Boot:** Read `.claude/manifest.md` first.

## Index
### 1. Core (Must Read)
- `core/essential-rules.yaml`: Non-negotiable Rules & Tech Stack
- `memory.md`: Current Context & Status

### 2. Design (Read on Demand)
- `core/system-design.yaml`: Architecture, API, Entity Rules (Core + Quant 모듈 분리 구조 포함)

### 3. References (Lazy Load)
- `references/conventions/coding-style.yaml`: Java idioms, naming, DI basics
- `references/conventions/testing-guide.yaml`: JUnit 5 practices
- `references/data-model/`: Domain Entities (Core)
- `references/data-model/quant/quant_signal.yaml`: Quant 도메인 모델 — **Phase 9+ 작업 시에만 읽을 것**

### 4. Docs (Read when writing/reviewing specs)
- `docs/spec-writing-guide.yaml`: Spec document format, output location, writing principles
- `docs/templates/feature-spec.yaml`: Feature spec template
- `docs/roadmap.md`: Phase 5~11 전체 로드맵
  - Phase 5~8: Core 고도화 (KIS+DART 통합, RAG, Feedback Loop)
  - Phase 9~11: Quant Signal Engine (Feature Engineering → Scoring → Alpha Tracking)
  - **Quant 작업 시에만 읽을 것**
- `features/{domain}/`: Feature specs by domain

## Skills
- `/feature`: New Feature (Immutable Spec)
- `/fix`: Bug Fix (Root Cause Analysis)
- `/refactor`: Code Improvement (No logic change)
- `/docs`: Documentation Maintenance

## Agent Behavior (Protocol)
- **Ask Protocol**: If requirements are ambiguous, risky (e.g., deletion), or deviate from conventions, **STOP and ASK** the user immediately.
- **Module Guard**: Core 도메인(`domain/signal`, `domain/portfolio`, `domain/price`, `domain/stock`)과 Quant 도메인(`domain/quant`)은 서로 직접 의존하지 않는다. 공유 인프라(RAG, Feedback, Telegram)만 양쪽에서 사용 가능.
