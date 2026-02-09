# Android vs iOS 拼图逻辑深度分析

## 一、坐标系

### 数据模型：两边均用左上角

| 平台 | 存储 | 说明 |
|------|------|------|
| Android | `piece.currentX, currentY` | 块左上角像素坐标 |
| iOS | `piece.currentX, currentY` | 块左上角像素坐标（与 Android 一致） |

### 网格换算公式

**Android：**
```
relativeX = currentX - puzzleBounds.left
relativeY = currentY - puzzleBounds.top
col = round(relativeX / cellW)
row = round(relativeY / cellH)
```

**iOS（`GameViewModel.toGrid`）：**
```
relativeX = x - boardOffsetX
relativeY = y - boardOffsetY
col = round(relativeX / cellWidth)
row = round(relativeY / cellHeight)
```

### iOS 视图层：中心点转换

SwiftUI `.position(x:y:)` 使用**视图中心**，因此 `PuzzlePieceView` 中：
```swift
.position(x: piece.currentX + piece.width / 2, y: piece.currentY + piece.height / 2)
```
将存储的左上角坐标转为中心点，供 `.position` 使用。

### 格子左上角坐标

**Android：** `targetX = offsetX + col * cellW`, `targetY = offsetY + row * cellH`

**iOS（`gridTopLeft`）：**
```swift
CGPoint(x: boardOffsetX + CGFloat(col) * cellWidth, y: boardOffsetY + CGFloat(row) * cellHeight)
```

---

## 二、合并与拆分

### 合并条件

- 相邻 + 对齐 → 可合并
- 合并后同组 `groupId` 一致，移动时整组一起拖动

### 拆分（unmerge）

- 长按已合并组可拆分
- iOS 需从 `paidConnections` 与 `paidEdgesByGroup` 中移除该组边，扣回金币
- 再次合并可重新计费

### 得分与金币

- 每连接一条新边得 2 金币
- `paidConnections`：已计费边集合（edgeKey）
- `paidEdgesByGroup`：每组已付边，拆分时用于扣回
- `scoredGroups`：每组已得分，unmerge 时扣回

---

## 三、拖动释放逻辑（`handleActionUp`）

**优先级顺序（与 Android 一致）：**

1. **Neighbor Snapping**：尝试与相邻块合并
2. **Free Grid**：目标格位空闲 → 整组落子
3. **Push / Swap**：与重叠块交互（挤压 / 互换）
4. **Snap to Grid**：无冲突时对齐到网格
5. **Revert**：无效则回退到起始位置

### 拖动增量计算（左上角）

```swift
// movedMinX/Y = 组内最小 currentX/Y（组左上角）
let movedMinX = movedGroup.map { $0.currentX }.min() ?? 0
let movedMinY = movedGroup.map { $0.currentY }.min() ?? 0
// groupOriginMinX/Y = 拖动开始时组左上角
let dragDx = movedMinX - groupOriginMinX
let dragDy = movedMinY - groupOriginMinY
// 网格偏移
gridDeltaCol = round(dragDx / cellW)
gridDeltaRow = round(dragDy / cellH)
```

---

## 四、相关文件

| 平台 | 切片 | 视图模型 | 交互逻辑 |
|------|------|----------|----------|
| Android | `ImageSlicer.kt` | `GameBoardView.kt` | `GameBoardView` 内 |
| iOS | `ImageSlicer.swift` | `GameViewModel.swift` | `GameViewModel+Interaction.swift` |
