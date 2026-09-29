#!/usr/bin/env bash
# [임시 진단] Maestro로 launch_clean을 반복하면서 30초마다 Maestro JVM 스레드 덤프를 남긴다.
# clearState가 멈추면 덤프에서 어느 호출(pm clear, APK pull, pm grant)에 묶였는지 보인다.
set -uo pipefail

OUT=build/maestro/diag
mkdir -p "$OUT"

maestro test .maestro/diag/clear_loop.yaml -e APP_ID="$APP_ID" \
  --debug-output="$OUT/debug" --flatten-debug-output >"$OUT/maestro-stdout.txt" 2>&1 &
MAESTRO_PID=$!

n=0
while kill -0 "$MAESTRO_PID" 2>/dev/null; do
  sleep 30
  n=$((n + 1))
  for pid in $(jps -l | awk '/maestro/ { print $1 }'); do
    jstack "$pid" >"$OUT/jstack-$(printf %03d "$n")-$pid.txt" 2>&1 || true
  done
done
wait "$MAESTRO_PID"
echo "diag maestro exit: $?"
tail -5 "$OUT/maestro-stdout.txt"
