#!/usr/bin/env bash
# 批量卸载 App（默认 OnePic）: 从所有已启动(Booted)模拟器卸载
# 用法:
#   ./scripts/uninstall_ios_batch.sh
#   ./scripts/uninstall_ios_batch.sh --bundle-id site.aiok.OnePic

set -euo pipefail

cd "$(dirname "$0")/.."

BUNDLE_ID="site.aiok.OnePic"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bundle-id)
      BUNDLE_ID="$2"
      shift 2
      ;;
    *)
      echo "未知参数: $1"
      exit 1
      ;;
  esac
done

BOOTED_UDIDS="$(xcrun simctl list devices | awk -F '[()]' '/\(Booted\)/ {print $2}')"
if [[ -z "${BOOTED_UDIDS}" ]]; then
  echo "没有已启动的模拟器，请先启动设备。"
  exit 1
fi

echo "==> 批量卸载 ${BUNDLE_ID}"
OK_COUNT=0
MISS_COUNT=0
FAIL_COUNT=0

while IFS= read -r udid; do
  [[ -z "${udid}" ]] && continue
  if xcrun simctl uninstall "${udid}" "${BUNDLE_ID}" >/dev/null 2>&1; then
    OK_COUNT=$((OK_COUNT + 1))
    echo "  ✅ ${udid} 卸载成功"
  else
    # 如果未安装也会返回失败，这里当作可接受结果
    if xcrun simctl get_app_container "${udid}" "${BUNDLE_ID}" >/dev/null 2>&1; then
      FAIL_COUNT=$((FAIL_COUNT + 1))
      echo "  ❌ ${udid} 卸载失败"
    else
      MISS_COUNT=$((MISS_COUNT + 1))
      echo "  ℹ️ ${udid} 未安装该 App"
    fi
  fi
done <<< "${BOOTED_UDIDS}"

echo "==> 完成: 成功 ${OK_COUNT}，未安装 ${MISS_COUNT}，失败 ${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -gt 0 ]]; then
  exit 2
fi

