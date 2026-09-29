#!/usr/bin/env bash
# debug APK를 설치하고 한 번 띄워 첫 실행 비용을 치른 뒤 Maestro smoke Flow를 돌린다.
# android-emulator-runner의 script는 한 줄씩 따로 실행돼서 여러 줄짜리 로직은 이 파일에 둔다.
#
# 필요한 환경 변수: APP_ID, MAESTRO_TAGS
set -euo pipefail

APK=artifacts/app-debug.apk
OUT=build/maestro
mkdir -p "$OUT"

# 실패를 되짚을 수 있게 테스트 내내 logcat을 남긴다(ActivityManager의 Displayed 줄에 실행 시간이 찍힌다).
adb logcat -v threadtime >"$OUT/logcat.txt" 2>&1 &
LOGCAT_PID=$!
trap 'kill "$LOGCAT_PID" 2>/dev/null || true' EXIT

adb install -r "$APK"

# 설치 직후 첫 실행은 dex 검증·파일 캐시 적재 때문에 유난히 느리다. Flow가 아니라 여기서 그 비용을 치른다.
# -W는 첫 프레임이 그려질 때까지 기다린다. UI_TESTING은 Flow와 같게 광고 동의 흐름을 끈다.
echo "Warming up $APP_ID..."
timeout 120 adb shell am start -W \
  -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
  -p "$APP_ID" --ez UI_TESTING true \
  || echo "::warning::Warm-up launch did not finish in time"
adb shell am force-stop "$APP_ID"

maestro test .maestro \
  --include-tags="$MAESTRO_TAGS" \
  -e APP_ID="$APP_ID" \
  --format=JUNIT \
  --output="$OUT/report.xml" \
  --test-output-dir="$OUT/output" \
  --debug-output="$OUT/debug" \
  --flatten-debug-output
