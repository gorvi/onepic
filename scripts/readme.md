
# 一键卸载（所有已启动模拟器）
./scripts/uninstall_ios_batch.sh

# 一键安装（所有已启动模拟器）
./scripts/install_ios_batch.sh

# 一键三版本同时卸载+安装（所有已启动模拟器）
./scripts/reinstall_ios_batch.sh

# 不构建，直接重装现有 app
./scripts/reinstall_ios_batch.sh --no-build

# 重装后自动启动
./scripts/reinstall_ios_batch.sh --launch site.aiok.OnePic
