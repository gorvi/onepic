#!/usr/bin/env bash
# 一键批量“卸载 + 安装”（所有已启动模拟器）
# 默认 bundle id: site.aiok.OnePic
#
# 用法:
#   ./scripts/reinstall_ios_batch.sh
#   ./scripts/reinstall_ios_batch.sh --bundle-id site.aiok.OnePic
#   ./scripts/reinstall_ios_batch.sh --no-build
#   ./scripts/reinstall_ios_batch.sh --launch site.aiok.OnePic
#   ./scripts/reinstall_ios_batch.sh --app /abs/path/TravelerPuzzle.app

set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$(pwd)"

UNINSTALL_SCRIPT="${ROOT}/scripts/uninstall_ios_batch.sh"
INSTALL_SCRIPT="${ROOT}/scripts/install_ios_batch.sh"

BUNDLE_ID="site.aiok.OnePic"
FORWARD_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bundle-id)
      BUNDLE_ID="$2"
      FORWARD_ARGS+=("$1" "$2")
      shift 2
      ;;
    --no-build|--app|--launch)
      if [[ "$1" == "--no-build" ]]; then
        FORWARD_ARGS+=("$1")
        shift
      else
        FORWARD_ARGS+=("$1" "$2")
        shift 2
      fi
      ;;
    *)
      echo "未知参数: $1"
      exit 1
      ;;
  esac
done

echo "==> 第一步: 批量卸载 ${BUNDLE_ID}"
bash "${UNINSTALL_SCRIPT}" --bundle-id "${BUNDLE_ID}" || true

echo "==> 第二步: 批量安装"
if [[ ${#FORWARD_ARGS[@]} -gt 0 ]]; then
  bash "${INSTALL_SCRIPT}" "${FORWARD_ARGS[@]}"
else
  bash "${INSTALL_SCRIPT}"
fi

echo "==> 批量重装完成"
