#!/usr/bin/env bash
# 批量安装 TravelerPuzzle 到所有已启动(iOS Booted)模拟器
# 用法:
#   ./scripts/install_ios_batch.sh
#   ./scripts/install_ios_batch.sh --no-build
#   ./scripts/install_ios_batch.sh --app /abs/path/TravelerPuzzle.app
#   ./scripts/install_ios_batch.sh --launch site.aiok.OnePic

set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$(pwd)"

PROJECT_PATH="ios/OnePic/OnePic.xcodeproj"
SCHEME="TravelerPuzzle"
DERIVED_DATA_PATH="ios/OnePic/build"
CONFIGURATION="Debug"
APP_PATH_DEFAULT="${ROOT}/ios/OnePic/build/Build/Products/Debug-iphonesimulator/TravelerPuzzle.app"

DO_BUILD=1
APP_PATH="${APP_PATH_DEFAULT}"
LAUNCH_BUNDLE_ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      DO_BUILD=0
      shift
      ;;
    --app)
      APP_PATH="$2"
      shift 2
      ;;
    --launch)
      LAUNCH_BUNDLE_ID="$2"
      shift 2
      ;;
    *)
      echo "未知参数: $1"
      exit 1
      ;;
  esac
done

if [[ "${DO_BUILD}" -eq 1 ]]; then
  echo "==> 构建 ${SCHEME} (${CONFIGURATION})..."
  xcodebuild -project "${PROJECT_PATH}" -scheme "${SCHEME}" \
    -destination 'generic/platform=iOS Simulator' \
    -derivedDataPath "${DERIVED_DATA_PATH}" \
    -configuration "${CONFIGURATION}" build >/dev/null
fi

if [[ ! -d "${APP_PATH}" ]]; then
  echo "未找到 .app: ${APP_PATH}"
  exit 1
fi

BOOTED_UDIDS="$(xcrun simctl list devices | awk -F '[()]' '/\(Booted\)/ {print $2}')"
if [[ -z "${BOOTED_UDIDS}" ]]; then
  echo "没有已启动的模拟器，请先启动设备。"
  exit 1
fi

echo "==> 批量安装: ${APP_PATH}"
OK_COUNT=0
FAIL_COUNT=0

while IFS= read -r udid; do
  [[ -z "${udid}" ]] && continue
  if xcrun simctl install "${udid}" "${APP_PATH}"; then
    OK_COUNT=$((OK_COUNT + 1))
    echo "  ✅ ${udid} 安装成功"
    if [[ -n "${LAUNCH_BUNDLE_ID}" ]]; then
      xcrun simctl launch "${udid}" "${LAUNCH_BUNDLE_ID}" >/dev/null || true
    fi
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    echo "  ❌ ${udid} 安装失败"
  fi
done <<< "${BOOTED_UDIDS}"

echo "==> 完成: 成功 ${OK_COUNT}，失败 ${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -gt 0 ]]; then
  exit 2
fi
