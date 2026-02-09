# OnePic 实现逻辑分析

## 一、架构概览

### Android

```
GameScreen (Compose)                → 游戏界面容器
    └── AndroidView(GameBoardView)  → 拼图画布（原生 View）
    └── LevelCompleteDialog         → 通关弹窗
    └── TutorialOverlay             → 教程引导
    └── RewardAnimationOverlay      → 金币/星星飞行动效

GameBoardView (View)                → 核心拼图逻辑
    └── onTouchEvent                → 触摸处理
    └── onDraw                      → 绘制 + 粒子系统
```

### iOS 主界面

```
MainTabView                         → Tab 容器
    └── GalaxyScaffold              → 共享宇宙背景、粒子、CelestialVisitor
        └── TabView
            ├── HomeView            → 关卡选择（NavigationStack）
            ├── GalaxyView          → 升阶 / Memories
            ├── CheckInView         → 签到
            └── MoreView            → 更多
```

### iOS 拼图界面

```
GameBoardView (SwiftUI)             → 游戏界面容器
    └── GameViewModel               → 拼图状态与逻辑
    └── PuzzlePieceView             → 单块渲染
    └── ParticleOverlayView         → 粒子效果
    └── HintOverlayView             → 提示动画
    └── LevelCompleteOverlay        → 通关弹窗
```

---

## 二、数据流

```
LevelRepository → LevelConfig → ImageSlicer → PuzzlePiece[] → GameBoardView / GameViewModel
```

---

## 三、核心数据模型 PuzzlePiece

| 字段 | Android | iOS | 说明 |
|------|---------|-----|------|
| id | Int | Int | 唯一标识，= row*cols + col |
| currentX/Y | Float | CGFloat | **左上角**像素坐标 |
| targetX/Y | Float | CGFloat | 正确位置的左上角 |
| width/height | Float | CGFloat | 块尺寸 |
| row/col | Int | Int | 当前所在网格行列 |
| groupId | Int | Int | 合并后同组相同 |
| image | Bitmap | UIImage | 块图像 |

---

## 四、拼图逻辑要点

### 合并

- 相邻且对齐时合并
- 同组共用一个 groupId
- 移动时整组一起动

### 拆分

- 长按已合并组可拆分
- 需从 `paidConnections` / `scoredGroups` 中移除该组边并扣回金币
- 以便再次合并能重新加分

### 得分

- 每连接一条新边得 2 金币
- 仅对新边计费
- unmerge 时按该组已计费边扣回

---

## 五、跨平台对照文档

| 文档 | 位置 | 说明 |
|------|------|------|
| 切片算法 | `ios/.../Resources/IMAGE_SLICE_ALGORITHM.md` | 像素裁切、scale 处理、黑块根因 |
| 坐标系与逻辑 | `ios/.../Resources/ANDROID_VS_IOS_ANALYSIS.md` | 左上角/中心点、网格换算、合并拆分 |

---

## 六、关键文件路径

### Android

- `logic/ImageSlicer.kt` — 切片
- `model/PuzzlePiece.kt` — 块模型
- `view/GameBoardView.kt` — 画布与触摸
- `ui/GameScreen.kt` — Compose 容器

### iOS

- `Logic/ImageSlicer.swift` — 切片
- `Model/PuzzlePiece.swift` — 块模型
- `ViewModel/GameViewModel.swift` — 状态与布局
- `ViewModel/GameViewModel+Interaction.swift` — 拖动、合并、拆解
- `View/GameBoardView.swift` — 主界面
- `View/PuzzlePieceView.swift` — 单块渲染
