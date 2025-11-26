# OnePic 游戏设计文档

## 1. 项目概况
**项目名称**: OnePic
**类型**: Android 拼图游戏
**核心玩法**: 将一张图片切割成若干碎片，支持单块拖动、多块成组移动、网格吸附、非对称交换等高级交互。
**技术栈**: Kotlin, Jetpack Compose, Canvas (用于高性能游戏板绘制)

## 2. 架构设计

### 2.1 总体架构
项目采用 MVVM (Model-View-ViewModel) 架构思想。
- **Model**: 数据定义 (`LevelConfig`, `PuzzlePiece`, `GridCell`)。
- **View**: UI 呈现 (`GameScreen`, `LevelSelectScreen`, `GameBoardView`)。
- **Logic/Repo**: 业务逻辑 (`LevelRepository`, `ImageSlicer`, `GridEngine`)。

### 2.2 目录结构
```
site.aiok.onepic
├── data
│   └── LevelRepository.kt      // 关卡数据仓库 (支持多模式)
├── logic
│   └── ImageSlicer.kt          // 图片切割逻辑
├── model
│   ├── LevelConfig.kt          // 关卡配置 (含 ImageSource)
│   └── PuzzlePiece.kt          // 碎片模型 (含 GridCell, GroupId)
├── ui
│   ├── components              // 通用组件
│   ├── GameScreen.kt           // 游戏容器
│   └── LevelSelectScreen.kt    // 关卡选择 (含模式切换)
├── view
│   ├── GameBoardView.kt        // 核心游戏板 (Canvas绘制, 手势处理)
│   └── ParticleSystem.kt       // 粒子特效
└── MainActivity.kt             // 入口
```

## 3. 游戏模式设计 (新功能)

### 3.1 模式定义
游戏将包含三种核心模式：

#### 模式 A: 经典模式 (Classic)
- **来源**: 预设的固定关卡（目前为生成的几何图形）。
- **数量**: 5 关。
- **配置**: 固定难度和行列数。
- **存储**: 硬编码在 `LevelRepository`。

#### 模式 B: 图库模式 (Gallery)
- **来源**: 应用内置资源包 (`assets/gallery_levels/`)。
- **数量**: 50 关。
- **机制**: 自动扫描文件夹中的图片文件。
- **难度曲线**:
  - 1-10关: 3x3 (Easy)
  - 11-20关: 3x4 (Easy+)
  - 21-30关: 4x5 (Medium)
  - 31-40关: 5x6 (Hard)
  - 41-50关: 6x8 (Expert)
- **资源管理**: 图片存放于 `assets`，运行时动态加载。

#### 模式 C: 自定义模式 (Custom)
- **来源**: 用户设备相册。
- **数量**: 10 个槽位。
- **机制**: 用户选择图片 -> 生成关卡。
- **难度曲线**:
  - 1-2关: 2x3 (Tutorial)
  - 3-4关: 3x4 (Easy)
  - 5-6关: 4x5 (Medium)
  - 7-8关: 5x6 (Hard)
  - 9-10关: 6x8 (Expert)

### 3.2 数据模型变更 (`LevelConfig`)

为了支持多种图片来源，`LevelConfig` 将引入 `ImageSource` 密封类：

```kotlin
sealed class ImageSource {
    data class Resource(val resId: Int) : ImageSource()  // 本地资源 ID
    data class Asset(val path: String) : ImageSource()   // Assets 文件路径
    data class UriSource(val uri: Uri) : ImageSource()   // 用户图片 URI
    object Generated : ImageSource()                     // 代码生成图形
}

data class LevelConfig(
    val levelId: String,      // 变更为 String 以支持非数字 ID (如 "g_01")
    val title: String,
    val difficulty: String,
    val imageSource: ImageSource,
    val rows: Int,
    val cols: Int
)
```

### 3.3 交互流程设计

#### 关卡选择页 (`LevelSelectScreen`)
1. **Tab 栏**: 顶部增加 Tab 切换 [经典] [图库] [自定义]。
2. **列表展示**:
   - **经典/图库**: Grid 布局显示关卡卡片。
   - **自定义**: 显示 10 个卡片位。若未设置，显示 "+" 号；若已设置，显示缩略图和 "开始" 按钮。
3. **图片选择**: 自定义模式点击 "+" 触发系统图片选择器 (`ActivityResultLauncher`)。

#### 游戏加载页 (`GameScreen`)
1. **图片加载器**: 根据 `ImageSource` 类型分发加载逻辑。
   - `Asset`: 使用 `AssetManager.open`。
   - `Uri`: 使用 `ImageDecoder` (Android P+) 或 `MediaStore`。
   - `Generated`: 维持现有 Canvas 绘制逻辑。
2. **位图处理**: 加载后统一缩放/裁剪至适应屏幕的尺寸 (如 1024x768 或 屏幕宽x高)，确保切片清晰度。

## 4. 核心逻辑 (已实现与优化)

### 4.1 网格引擎 (Grid Engine)
- **坐标系**: 逻辑坐标 (GridCell row/col) 与 物理坐标 (Pixel x/y) 分离。
- **状态管理**: `areGridCellsFree`, `getGridOccupancy`。
- **交互逻辑**:
  - **拖拽**: 实时更新物理坐标。
  - **释放 (Action Up)**:
    1. **优先吸附**: 检查目标网格是否完全空闲 -> 直接吸附。
    2. **邻居吸附**: 检查是否有物理相邻且逻辑相连的拼图 -> 合并组。
    3. **推动 (Push)**: 递归检查推动链 -> 执行链式移动。
    4. **交换 (Swap)**: 智能非对称交换 -> 检查目标组是否能填入源位置空洞。
    5. **回滚**: 若以上均失败 -> 弹回原位。

### 4.2 粒子系统
- **触发**: 拼图合并成功、关卡胜利。
- **优化**: 限制同屏粒子数量，胜利动画限时播放，降低资源消耗。

## 5. 下一步开发计划
1. **重构 LevelConfig**: 引入 `ImageSource`。
2. **实现 LevelRepository**: 填充 Gallery 和 Custom 逻辑。
3. **UI 更新**: 改造 `LevelSelectScreen` 支持 Tab 切换。
4. **资源集成**: 添加测试图片到 assets。
