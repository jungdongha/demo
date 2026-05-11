#!/usr/bin/env bash
#
# 스킬 자동 활성화 훅
# - 트리거: UserPromptSubmit
# - 목적: 사용자 프롬프트 키워드를 분석하여 적절한 스킬 규칙 준수 유도
#

set -euo pipefail

main() {
    # Read JSON payload from stdin
    local payload
    payload=$(cat)

    # Extract user prompt using jq or fallback to grep
    local prompt

    if command -v jq &> /dev/null; then
        prompt=$(echo "$payload" | jq -r '.prompt // empty' | tr '[:upper:]' '[:lower:]')
    else
        # Fallback to basic grep if jq is not available
        prompt=$(echo "$payload" | grep -o '"prompt":"[^"]*"' | cut -d'"' -f4 | tr '[:upper:]' '[:lower:]')
    fi

    # Check if prompt is empty
    if [[ -z "$prompt" ]]; then
        exit 0
    fi

    # Check for fix skill keywords
    if [[ "$prompt" == *"버그"* ]] || [[ "$prompt" == *"에러"* ]] || \
       [[ "$prompt" == *"오류"* ]] || [[ "$prompt" == *"고쳐"* ]] || \
       [[ "$prompt" == *"수정해"* ]] || [[ "$prompt" == *"픽스"* ]] || \
       [[ "$prompt" == *"fix"* ]] || [[ "$prompt" == *"bug"* ]] || \
       [[ "$prompt" == *"error"* ]]; then
        cat <<'EOF'

🎯 SKILL ACTIVATION CHECK - [fix] 스킬 규칙 준수 필수

【 FIX 스킬 필수 절차 】
1. 문제 재현 및 근본 원인(Root Cause) 분석
2. 최소 침습적 수정 (Over-engineering 금지)
3. 회귀 테스트 작성/실행
4. 관련 문서 업데이트 (.claude/memory.md)

⚠️  주의: 증상만 제거하지 말고 원인을 제거할 것
EOF
        exit 0
    fi

    # Check for refactor skill keywords
    if [[ "$prompt" == *"리팩토링"* ]] || [[ "$prompt" == *"리팩터링"* ]] || \
       [[ "$prompt" == *"정리"* ]] || [[ "$prompt" == *"개선"* ]] || \
       [[ "$prompt" == *"refactor"* ]] || [[ "$prompt" == *"clean"* ]]; then
        cat <<'EOF'

🎯 SKILL ACTIVATION CHECK - [refactor] 스킬 규칙 준수 필수

【 REFACTOR 스킬 필수 원칙 】
1. 외부 동작(External Behavior) 불변 보장
2. 테스트 먼저 실행 (Green 상태 확인)
3. 단계별 리팩토링 + 각 단계마다 테스트
4. 불필요한 추상화 금지 (YAGNI 원칙)

⚠️  주의: 기능 추가 없이 구조 개선만 수행
EOF
        exit 0
    fi

    # Check for docs skill keywords
    if [[ "$prompt" == *"문서"* ]] || [[ "$prompt" == *"문서화"* ]] || \
       [[ "$prompt" == *"동기화"* ]] || [[ "$prompt" == *"readme"* ]] || \
       [[ "$prompt" == *"doc"* ]]; then
        cat <<'EOF'

🎯 SKILL ACTIVATION CHECK - [docs] 스킬 규칙 준수 필수

【 DOCS 스킬 필수 작업 】
1. 코드와 문서의 불일치 검증
2. Entity/Spec/README 동기화
3. 예제 코드 정확성 검증
4. 변경 이력 기록 (.claude/memory.md)

⚠️  주의: 코드를 읽고 이해한 후 문서 작성
EOF
        exit 0
    fi

    # Check for feature skill keywords
    if [[ "$prompt" == *"기능"* ]] || [[ "$prompt" == *"추가"* ]] || \
       [[ "$prompt" == *"구현"* ]] || [[ "$prompt" == *"feature"* ]] || \
       [[ "$prompt" == *"새로운"* ]] || [[ "$prompt" == *"신규"* ]]; then
        cat <<'EOF'

🎯 SKILL ACTIVATION CHECK - [feature] 스킬 규칙 준수 필수

【 FEATURE 스킬 필수 절차 】
1. 불변 스펙(Immutable Spec) 작성 및 승인
2. 기존 패턴 준수 (conventions/ 참조)
3. 테스트 우선 작성 (JUnit 5)
4. 단계별 구현 (Controller → UseCase → Repository)

⚠️  주의: 스펙 승인 전 구현 금지, Over-engineering 금지
EOF
        exit 0
    fi

    # No skill matched - exit silently
    exit 0
}

# Run main function
main
