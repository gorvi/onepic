# OnePic 游戏设计文档

## 1. 项目概况
**项目名称**: OnePic
**类型**: Android 拼图游戏
**核心玩法**: 将一张图片切割成若干碎片，玩家通过拖拽碎片将其复原。
**技术栈**: Kotlin, Jetpack Compose

## 2. 架构设计

### 2.1 总体架构
项目采用 MVVM (Model-View-ViewModel) 架构思想（尽管目前部分逻辑直接嵌入在 UI 或 Logic 对象中，建议后续向标准 MVVM 演进），分离关注点。

- **Model (数据层)**: 定义游戏的核心数据结构。
  - `LevelConfig`: 关卡配置（图片资源、难度、行列数）。
  - `PuzzlePiece`: 拼图碎片的状态（位置、目标位置、关联的 Bitmap、Z轴索引等）。
- **View (表现层)**: 使用 Jetpack Compose 构建 UI。
  - `MainActivity`: 程序的入口和导航容器。
  - `LevelSelectScreen`: 关卡选择界面。
  - `GameScreen`: 游戏主界面，负责渲染游戏区域。
  - `GlassComponents`: 通用 UI 组件（如玻璃拟态效果）。
- **Logic (逻辑层)**: 处理游戏核心算法。
  - `ImageSlicer`: 负责将原始图片切割成 `PuzzlePiece` 列表，并随机打乱位置。
- **Data (数据源)**:
  - `LevelRepository`: 提供关卡数据列表。

### 2.2 目录结构
```
site.aiok.onepic
├── data
│   └── LevelRepository.kt      // 关卡数据仓库
├── logic
│   └── ImageSlicer.kt          // 图片切割核心逻辑
├── model
│   ├── LevelConfig.kt          // 关卡配置模型
│   └── PuzzlePiece.kt          // 拼图碎片模型
├── ui
│   ├── components              // 通用 UI 组件
│   ├── theme                   // 主题定义 (Color, Type, Theme)
│   ├── GameScreen.kt           // 游戏界面
│   └── LevelSelectScreen.kt    // 关卡选择界面
├── view
│   ├── GameBoardView.kt        // (可能废弃或用于特定绘制) 游戏板视图
│   └── ParticleSystem.kt       // 粒子系统（用于特效）
└── MainActivity.kt             // 应用入口
```

## 3. 核心流程

### 3.1 游戏启动与导航
1. 应用启动进入 `MainActivity`。
2. 初始状态显示 `LevelSelectScreen`。
3. 用户点击关卡 -> 触发 `onLevelSelected` 回调。
4. 更新 `currentScreen` 状态为 "game"，并传递选中的 `LevelConfig`。
5. 渲染 `GameScreen`。

### 3.2 关卡加载与初始化
1. `GameScreen` 接收 `LevelConfig`。
2. 加载 `imageResId` 对应的 Bitmap。
3. 调用 `ImageSlicer.sliceImage(bitmap, rows, cols)`：
   - 根据行列数计算每个碎片的宽高。
   - 切割 Bitmap。
   - 生成目标位置 (Target X, Y)。
   - 生成随机初始位置 (Current X, Y)。
   - 返回 `List<PuzzlePiece>`。

### 3.3 游戏交互 (拖拽与吸附)
*注：此部分基于常见拼图逻辑推断，需查看 GameScreen 进一步确认实现细节*
1. 用户触摸屏幕，通过手势识别（Gestures）捕获拖拽操作。
2. 更新被拖拽碎片的 `currentX`, `currentY`。
3. 释放手指时，判断 `current` 位置是否接近 `target` 位置（吸附逻辑）。
4. 检查是否所有碎片都已归位（胜利条件）。

## 4. 数据模型详情

### LevelConfig
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| levelId | Int | 唯一标识 |
| title | String | 关卡名称 |
| difficulty | String | 难度描述 (Easy/Medium/Hard) |
| imageResId | Int | 图片资源 ID |
| rows | Int | 切割行数 |
| cols | Int | 切割列数 |

### PuzzlePiece
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | Int | 碎片唯一标识 |
| bitmap | Bitmap | 碎片图像数据 |
| currentX, currentY | Float | 当前渲染坐标 |
| targetX, targetY | Float | 正确复原坐标 |
| width, height | Float | 碎片尺寸 |
| zIndex | Int | 渲染层级 |
| groupId | Int | 组 ID (用于多块吸附组合) |
| row, col | Int | 在网格中的行列索引 |

## 5. 未来改进计划
1. **状态管理优化**: 引入 `ViewModel` 来管理 `GameScreen` 的状态（如碎片列表、计时器、步数），避免配置更改导致数据丢失。
2. **持久化**: 保存游戏进度（已通过关卡、最佳成绩）。
3. **动画效果**: 增加碎片归位动画、胜利动画、粒子特效集成。
4. **性能优化**: 针对大图切割和大量碎片的渲染性能进行优化。
5. **音效**: 添加背景音乐和操作音效。



