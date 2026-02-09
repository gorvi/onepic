# 工作区任务说明

## 改完代码自动安装（保存时自动构建并安装到模拟器）

已配置：**保存任意 `.swift` 文件时**自动执行构建并安装到当前已启动的 iOS 模拟器。

### 方式一：Run on Save 扩展（推荐）

1. **安装扩展**：在 Cursor 左侧扩展里搜索 **Run on Save**（emeraldwalk.RunOnSave），或打开本工作区时点“安装工作区推荐扩展”。
2. **安装后重载**：安装完若未生效，按 `Cmd+Shift+P` → 输入 “Reload Window” → 回车重载窗口。
3. **确认触发**：保存任意 `.swift` 文件后，在“输出”面板里选择“Run on Save”，应能看到「改完代码自动安装中…」和「已安装到模拟器。」。若没有，说明扩展未触发，用方式二。

### 方式二：监视脚本（不依赖扩展）

若扩展不触发或不想用扩展，可在终端运行监视脚本（需先安装 fswatch：`brew install fswatch`）：

```bash
./scripts/watch_and_install_ios.sh
```

保持该终端开着；之后每次保存 `ios/OnePic` 下的 `.swift` 文件，会自动构建并安装到模拟器。

### 手动安装（保存后自己点一次）

- **快捷键**：保存代码后按 **⌘⇧B**（Mac）或 **Ctrl+Shift+B** 运行默认构建任务（构建 + 安装）。
- **终端**：先启动模拟器，再执行 `./scripts/install_ios.sh`。
