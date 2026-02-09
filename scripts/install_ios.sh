#!/usr/bin/env bash
# 构建并安装 OnePic 到当前已启动的 iOS 模拟器（每次自动安装可运行此脚本）
set -e
cd "$(dirname "$0")/.."
xcodebuild -project ios/OnePic/OnePic.xcodeproj -scheme OnePic \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath ios/OnePic/build -configuration Debug build
xcrun simctl install booted \
  "$(pwd)/ios/OnePic/build/Build/Products/Debug-iphonesimulator/OnePic.app"
echo "已安装到模拟器。"
