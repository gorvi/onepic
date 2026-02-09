# OnePic Android 版本实现逻辑分析

## 一、架构概览

```
GameScreen (Compose)          → 游戏界面容器
    └── AndroidView(GameBoardView)  → 拼图画布（原生 View）
    └── LevelCompleteDialog   → 通关弹窗
    └── TutorialOverlay       → 教程引导
    └── RewardAnimationOverlay → 金币/星星飞行动效

GameBoardView (View)          → 核心拼图逻辑
    └── onTouchEvent          → 触摸处理
    └── onDraw                → 绘制 + 粒子系统
```

**数据流**: `LevelRepository` → `LevelConfig` → `ImageSlicer` → `PuzzlePiece[]` → `GameBoardView.setPieces()`

---

## 二、核心数据模型

### PuzzlePiece

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 唯一标识，= row*cols + col（正确位置） |
| currentX/Y | Float | **左上角**像素坐标 |
| targetX/Y | Float | 正确位置的左上角 |
| width/height | Float | 块尺寸 |
| row/col | Int | **当前**逻辑网格位置（会随移动/合并变化） |
| groupId | Int | 组 ID，同一组块一起移动 |
| zIndex | Int | 绘制/拾取优先级 |

**注意**: Android 用 **左上角** 坐标，iOS 用 **中心** 坐标。

### 坐标系

- **逻辑网格**: `(row, col)`，row=0 为顶行，col=0 为左列
- **像素坐标**: 左上角为 `(0,0)`，`puzzleBounds` 为拼图矩形区域

---

## 三、触摸与交互

### 1. 触摸流程

```
onTouchEvent
    ├── GestureDetector.onDoubleTap  → 双击 → unmergeSelectedGroup
    └── when (action)
            ├── ACTION_DOWN  → 按坐标找顶层块 → 选中的 group
            ├── ACTION_MOVE  → 整体移动该 group + 边界限制
            └── ACTION_UP    → handleActionUp → checkForWin
```

### 2. 块命中检测

```kotlin
pieces.sortedByDescending { it.zIndex }
    .firstOrNull { 
        x >= it.currentX && x <= it.currentX + it.width && 
        y >= it.currentY && y <= it.currentY + it.height 
    }
```

按 **zIndex 降序** 找第一个包含触摸点的块（左上角 + 宽高）。

### 3. 拖拽边界

拖拽时 group 被限制在 `puzzleBounds` 内，通过 clamp `dx/dy` 实现。

---

## 四、handleActionUp 松手策略（优先级）

松手后的处理顺序：

1. **SNAP（邻接合并）**  
   - 若当前块与正确邻居在阈值内对齐，则合并  
   - 教程: 主轴 35%、交叉轴 5%  
   - 普通: 主轴 15%、交叉轴 5%  

2. **目标格为空 → Grid Snap**  
   - 目标格子未被占用时，直接移动过去  

3. **Push（推挤）**  
   - 目标格被占 → 按拖拽方向构建推挤链  
   - 链式推挤，最大深度 10  
   - 播放 PUSH 音效  

4. **Swap（交换）**  
   - 与目标组互换位置  
   - 对方组填入我方 vacated 格  
   - 播放 SWAP 音效  

5. **Valid Placement → Grid Snap**  
   - 当前位置合法且无重叠，做网格对齐  

6. **Grid Snap 兜底**  
   - 尝试对齐到最近网格  

7. **Revert（回退）**  
   - 以上都失败 → 恢复 `dragStartStates`  
   - 播放 REVERT 音效  

---

## 五、邻接合并 (checkNeighborSnapping)

### 邻居判定

基于 **正确位置**（id → row/col）判断是否在解中相邻：

- 右邻: `targetCorrectCol - 1 == currentCorrectCol`
- 左邻: `targetCorrectCol + 1 == currentCorrectCol`
- 下邻: `targetCorrectRow - 1 == currentCorrectRow`
- 上邻: `targetCorrectRow + 1 == currentCorrectRow`

### 对齐阈值

- 主轴: 块宽/高的 15%（教程 35%）
- 交叉轴: 5%（防止斜角误合并）

### 合并后

- 播放 SNAP 音效
- 发射粒子
- 加分：每边 2 分，通过 `onScoreChange`
- 合并边去重：`paidConnections` 防止重复计分

---

## 六、网格相关

### GridCell

```kotlin
data class GridCell(val row: Int, val col: Int)
```

### 关键函数

| 函数 | 作用 |
|------|------|
| getGridOccupancy(group) | 组当前占用的 `(row,col)` 集合 |
| getCurrentGridPosition(group) | 从像素坐标反推网格 |
| offsetGridCells(cells, dRow, dCol) | 偏移网格 |
| areGridCellsInBounds | 是否在拼图范围内 |
| areGridCellsFree | 是否未被占用 |
| moveGroupToGridCells | 按目标网格移动并更新 row/col |

### moveGroupToGridCells 映射

按组内块的**相对位置**建立 `源格 → 目标格` 映射，保证组形状不变。

---

## 七、洗牌 (setPieces)

1. 生成所有格 `(r,c)`
2. 教程 2x2: 固定 `1,4,2,3` 布局
3. 其他: `shuffle`，直到**非已解**（最多 100 次）
4. 把 slots 赋给 pieces，更新 `row/col`

---

## 八、胜利判定

```kotlin
val allSameGroup = pieces.all { it.groupId == firstGroupId }
```

所有块属于同一 group 即胜利，与是否在正确格子无关。

---

## 九、拆分 (unmergeSelectedGroup)

1. 把组内每块的 `groupId` 设回 `piece.id`
2. 随机偏移 ±10px，视觉分离
3. 提高 zIndex
4. 若该组有得分，扣回分数 `onScoreChange(-scoreToDeduct, 0)`
5. 播放 REVERT 音效

---

## 十、Hint 提示

- `showHint()`: 选一块错位的，做幽灵飞行动画到目标格
- 红色虚线 = 当前，绿色虚线 = 目标
- 教程有免费 hint，其他关卡消耗 100 金币或看广告

---

## 十一、计分与金币

- **scoredGroups**: groupId → 该组累计得分
- **paidConnections**: "idA-idB" 边集合，避免重复发币
- 合并: 每新边 +2 分，+2 金币（仅新边）
- Buff: `isDoubleCoinsActive` 时分数和金币 x2

---

## 十二、与 iOS 的主要差异

| 项目 | Android | iOS |
|------|---------|-----|
| 坐标 | 左上角 (currentX, currentY) | 中心 (currentX, currentY) |
| 触摸 | 单个 onTouchEvent | 整版 DragGesture + 坐标查找 |
| 双击 | GestureDetector.onDoubleTap | 短拖拽 + 时间/距离判断 |
| 拆分扣分 | 有 | 无 |
| 粒子/烟花 | ParticleSystem | 无 |
| Hint | 有，幽灵飞行 | 无 |

---

## 十三、文件职责

| 文件 | 职责 |
|------|------|
| GameBoardView.kt | 拼图绘制、触摸、handleActionUp、合并/推挤/交换 |
| GameScreen.kt | Compose UI、弹窗、教程、奖励动画、广告 |
| ImageSlicer.kt | Bitmap 切分、生成 PuzzlePiece |
| LevelRepository.kt | 关卡配置、多语言 JSON |
| LevelProgressManager.kt | 解锁、完成、星星、时间、金币、Buff |
| ParticleSystem.kt (view) | 合并粒子、胜利烟花 |
