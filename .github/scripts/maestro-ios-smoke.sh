#!/usr/bin/env bash
# 시뮬레이터용 debug 앱을 설치하고 Maestro smoke Flow를 돌린다.
#
# 필요한 환경 변수: UDID, APP_PATH, APP_ID, MAESTRO_TAGS
set -euo pipefail

OUT=build/maestro
mkdir -p "$OUT"

# 실패를 되짚을 수 있게 테스트 내내 앱 로그를 남긴다(크래시·UI_TESTING 인식 여부 확인용).
xcrun simctl spawn "$UDID" log stream --style compact --predicate 'process == "CalcMoney"' >"$OUT/app.log" 2>&1 &
LOG_PID=$!
trap 'kill "$LOG_PID" 2>/dev/null || true' EXIT

xcrun simctl install "$UDID" "$APP_PATH"

maestro --device "$UDID" test .maestro \
  --include-tags="$MAESTRO_TAGS" \
  -e APP_ID="$APP_ID" \
  --format=JUNIT \
  --output="$OUT/report.xml" \
  --test-output-dir="$OUT/output" \
  --debug-output="$OUT/debug" \
  --flatten-debug-output
