#!/bin/bash
# ============================================================
#  RDS SSH Tunnel — 로컬 개발용
#  회사망에서 포트 5432가 막혀 있을 때 SSH 터널을 통해 RDS에 접속
#
#  사용법:
#    1. EC2_HOST 변수에 EC2 퍼블릭 IP 입력
#    2. chmod +x scripts/tunnel.sh  (최초 1회)
#    3. ./scripts/tunnel.sh
#
#  터널 실행 후 .env 에서 DB_HOST=localhost 로 변경하여 Spring 실행
#  종료: Ctrl+C
# ============================================================

EC2_HOST="${EC2_HOST:-13.125.227.244}"
EC2_USER="${EC2_USER:-ubuntu}"
PEM_KEY="${PEM_KEY:-$HOME/Downloads/demo-dong-keyPair.pem}"

RDS_HOST="demodongdb.cxk24yia8paw.ap-northeast-2.rds.amazonaws.com"
RDS_PORT=5432
LOCAL_PORT=5432

# ── Validation ──────────────────────────────────────────────
if [[ -z "$EC2_HOST" ]]; then
  echo "EC2_HOST 가 비어 있습니다."
  exit 1
fi

if [[ ! -f "$PEM_KEY" ]]; then
  echo "❌  PEM 키 파일을 찾을 수 없습니다: $PEM_KEY"
  echo "    export PEM_KEY=<PEM 파일 경로>"
  exit 1
fi

chmod 400 "$PEM_KEY"

# ── Tunnel (자동 재연결 루프) ────────────────────────────────
echo "SSH 터널 시작 (끊기면 5초 후 자동 재연결)"
echo "  localhost:$LOCAL_PORT -> EC2($EC2_HOST) -> RDS($RDS_HOST:$RDS_PORT)"
echo "  종료: Ctrl+C"
echo ""

trap 'echo "터널 종료"; exit 0' INT TERM

while true; do
    ssh -i "$PEM_KEY" \
        -L "${LOCAL_PORT}:${RDS_HOST}:${RDS_PORT}" \
        -N \
        -o StrictHostKeyChecking=no \
        -o ServerAliveInterval=30 \
        -o ServerAliveCountMax=3 \
        -o ConnectTimeout=10 \
        -o ExitOnForwardFailure=yes \
        "${EC2_USER}@${EC2_HOST}"

    EXIT_CODE=$?
    if [[ $EXIT_CODE -eq 0 ]]; then
        echo "터널 정상 종료"
        break
    fi
    echo "[$(date '+%H:%M:%S')] 터널 끊김 (exit=$EXIT_CODE) — 5초 후 재연결..."
    sleep 5
done
