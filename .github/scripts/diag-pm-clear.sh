#!/usr/bin/env bash
# [임시 진단] Maestro launchApp clearState가 하는 일을 단계별로 반복하며 시간을 잰다.
# Maestro 2.1.0: pm list packages → pm clear → pm list packages -f → APK pull → 권한마다 pm grant
set -uo pipefail

APP_ID=com.ahn.calcmoney
ITERATIONS="${1:-20}"
OUT=build/maestro/diag
mkdir -p "$OUT"
LAUNCHER=$(adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$APP_ID" | tail -n 1 | tr -d '\r')
PERMS="android.permission.INTERNET android.permission.ACCESS_NETWORK_STATE com.google.android.gms.permission.AD_ID android.permission.ACCESS_ADSERVICES_AD_ID android.permission.ACCESS_ADSERVICES_ATTRIBUTION android.permission.ACCESS_ADSERVICES_TOPICS android.permission.WAKE_LOCK com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE android.permission.FOREGROUND_SERVICE"

# 오래 걸리는 단계가 있으면 20초마다 기기 상태를 떠 둔다.
watch_step() {
  local name=$1 pid=$2 n=0
  while kill -0 "$pid" 2>/dev/null; do
    sleep 20
    kill -0 "$pid" 2>/dev/null || break
    n=$((n + 1))
    {
      echo "=== $(date +%T) $name still running (${n}x20s)"
      adb shell cat /proc/loadavg
      adb shell ps -A -o PID,STAT,WCHAN,TIME,NAME | grep -E "adbd|artd|installd|system_server| pm$|cmd|sh$" || true
      adb shell top -b -n 1 -m 10 || true
    } >>"$OUT/stall.txt" 2>&1
  done
}

t() {
  local name=$1; shift
  local start end
  start=$(date +%s.%N)
  "$@" >/dev/null 2>&1 &
  local pid=$!
  watch_step "$name" "$pid" &
  local wpid=$!
  wait "$pid"
  kill "$wpid" 2>/dev/null; wait "$wpid" 2>/dev/null
  end=$(date +%s.%N)
  local d
  d=$(awk -v s="$start" -v e="$end" 'BEGIN { printf "%.2f", e - s }')
  echo "$ITER $name $d" >>"$OUT/timings.txt"
  if awk -v d="$d" 'BEGIN { exit !(d > 10) }'; then
    echo "::warning::iteration $ITER step '$name' took ${d}s"
  fi
}

apk_path() {
  adb shell "pm list packages -f --user 0 | grep $APP_ID | head -1" | tr -d '\r' | sed -e 's/^package://' -e "s/=$APP_ID\$//"
}

for ITER in $(seq 1 "$ITERATIONS"); do
  adb shell am start -W -n "$LAUNCHER" --ez UI_TESTING true >/dev/null 2>&1
  sleep 3
  t list adb shell pm list packages --user 0 "$APP_ID"
  t clear adb shell pm clear "$APP_ID"
  t listf apk_path
  P=$(apk_path)
  t pull adb pull "$P" "$OUT/app.apk"
  for perm in $PERMS; do
    t "grant:${perm##*.}" adb shell pm grant "$APP_ID" "$perm"
  done
  echo "iteration $ITER: $(awk -v i="$ITER" '$1 == i { printf "%s=%s ", $2, $3 }' "$OUT/timings.txt")"
done
rm -f "$OUT/app.apk"
adb shell am force-stop "$APP_ID"
echo "Slowest steps:"
sort -k3 -n -r "$OUT/timings.txt" | head -10
