#!/usr/bin/env bash
# 监视 ios/OnePic 下的 .swift 文件，保存后自动构建并安装到模拟器（不依赖 Run on Save 扩展）
# 用法：在项目根目录运行 ./scripts/watch_and_install_ios.sh，保持终端开着即可
set -e
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
INSTALL_SCRIPT="${ROOT}/scripts/install_ios.sh"

if ! command -v fswatch &>/dev/null; then
  echo "需要 fswatch。安装：brew install fswatch"
  exit 1
fi

echo "监视 ios/OnePic 下的 .swift 文件，保存后将自动安装到模拟器。按 Ctrl+C 退出。"
fswatch -o --include='.*\.swift$' ios/OnePic | while read -r; do
  echo "[$(date '+%H:%M:%S')] 检测到变更，开始构建并安装…"
  bash "$INSTALL_SCRIPT" || true
done
