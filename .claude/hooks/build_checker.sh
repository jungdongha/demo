#!/usr/bin/env bash
#
# 무결점 스프링 부트 빌드 검증 훅
# - 트리거: PostToolUse (Edit/Write 후)
# - 목적: 스프링 부트 빌드 에러를 즉시 감지하여 Claude가 자동 수정하도록 유도
#

set -euo pipefail

# Read JSON payload from stdin
payload=$(cat)

# Extract tool name and file path using jq or fallback to grep
if command -v jq &> /dev/null; then
    tool_name=$(echo "$payload" | jq -r '.tool.name // empty')
    file_path=$(echo "$payload" | jq -r '.tool.parameters.file_path // empty')
else
    # Fallback to basic grep if jq is not available
    tool_name=$(echo "$payload" | grep -o '"name":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
    file_path=$(echo "$payload" | grep -o '"file_path":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
fi

# Only run for Edit/Write/NotebookEdit tools
if [[ "$tool_name" != "Edit" && "$tool_name" != "Write" && "$tool_name" != "NotebookEdit" ]]; then
    exit 0
fi

# Check if modified file is Java source
if [[ ! "$file_path" == *.java ]]; then
    exit 0
fi

# Navigate to project root (3 levels up from hooks directory)
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$project_root"

# Check if gradlew exists
if [[ ! -f "./gradlew" ]]; then
    echo "⚠️  gradlew를 찾을 수 없습니다. 프로젝트 루트에서 실행 중인지 확인하세요."
    exit 0
fi

# Run Spring Boot build with optimizations
echo "🔍 스프링 부트 빌드 검증 중..." >&2

build_output=""
exit_code=0

# Run build and capture output (timeout handled by settings.json)
build_output=$(./gradlew classes testClasses --parallel --no-daemon --quiet 2>&1) || exit_code=$?

if [[ $exit_code -ne 0 ]]; then
    # Build failed - inject error into Claude's context
    cat <<EOF

================================================================================
⚠️  SPRING BOOT BUILD FAILED - 즉시 수정이 필요합니다
================================================================================

수정된 파일: $file_path

【 빌드 에러 로그 】
$build_output

================================================================================
🔧 액션: 위 에러를 분석하고 코드를 즉시 수정하세요.
================================================================================
EOF
    exit 0
else
    # Build successful - silent pass
    echo "✓ 스프링 부트 빌드 검증 통과" >&2
    exit 0
fi
