#!/usr/bin/env bash
# 설치된 iOS 런타임 중 가장 최신 것에서 이름이 $1인 시뮬레이터의 UDID를 출력한다. 없으면 아무것도 출력하지 않는다.
# ios-ci.yml의 DESTINATION(name=…,OS=latest)과 같은 기기를 고른다.
set -euo pipefail

xcrun simctl list devices available -j | jq -r --arg name "$1" '
  [.devices | to_entries[]
   | select(.key | test("SimRuntime\\.iOS-"))
   | {ver: (.key | capture("iOS-(?<v>[0-9-]+)$").v | split("-") | map(tonumber)),
      dev: (.value[] | select(.name == $name))}]
  | sort_by(.ver) | last | .dev.udid // empty'
