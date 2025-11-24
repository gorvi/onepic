# 新网格引擎日志分析指南

## 日志关键字

### 🔍 交互流程追踪

```
handleActionUp: Overlap detected with X pieces. Attempting interaction.
```
**含义**: 检测到 X 个 piece 被覆盖,开始尝试推挤或交换

---

```
tryPushInteraction: Attempting GRID-BASED push
  Push direction: deltaRow=X, deltaCol=Y
  Push chain depth: Z groups
```
**含义**: 
- 推挤方向: 行偏移X, 列偏移Y (±1 或 0)
- 推挤链深度: 需要推动 Z 个组

**成功标志**: `tryPushInteraction: SUCCESS - Pushed Z groups`  
**失败标志**: `Push blocked: Group out of bounds` 或 `Push blocked: Target cells occupied`

---

```
trySwapInteraction: Attempting GRID-BASED swap
  Moved group original cells: X
  Target group cells: Y
  Moved delta: (rowDelta, colDelta)
  Target delta: (rowDelta, colDelta)
```
**含义**: 
- 移动组占据 X 个格子
- 目标组占据 Y 个格子
- 两组的移动偏移量

**成功标志**: `trySwapInteraction: SUCCESS - Swapped positions`  
**失败标志**: `Swap blocked: Out of bounds` 或 `Swap blocked: Target cells occupied`

---

```
tryGridSnap: Attempting GRID-BASED snap
  Current grid cells: {...}
```
**含义**: 尝试将组对齐到最近的网格位置

**成功标志**: `Grid snap SUCCESS`  
**失败标志**: `Grid snap FAILED (cells occupied or out of bounds)`

---

### ❌ 失败场景分析

#### 场景 1: L形块无法移动
```
handleActionUp: Overlap detected with 4 pieces. Attempting interaction.
tryPushInteraction: Attempting GRID-BASED push
  Push blocked: Target cells occupied
trySwapInteraction: Attempting GRID-BASED swap
  Swap blocked: Out of bounds
handleActionUp: Interaction failed (Push/Swap blocked).
handleActionUp: Action failed. Reverting.
```

**原因分析**:
1. 推挤失败 → 推挤链的终点被其他块占据
2. 交换失败 → 交换后移动组或目标组超出边界
3. **根本原因**: 目标位置没有足够的空间容纳该形状

**解决方法**: 拖动到更空旷的位置,或先移开阻挡的块

---

#### 场景 2: 推挤链过深
```
tryPushInteraction: Chain too deep, aborting
```

**原因**: 推挤链超过 10 层 (可能形成循环或死锁)  
**建议**: 简化拼图布局,避免过于紧密的排列

---

### ✅ 成功场景示例

#### 成功推挤
```
tryPushInteraction: Attempting GRID-BASED push
  Push direction: deltaRow=0, deltaCol=1
  Push chain depth: 2 groups
  Moved piece[7] to grid (2, 3) at pixel (404.8, 857.25)
  Moved piece[12] to grid (3, 3) at pixel (404.8, 1032.0)
tryPushInteraction: SUCCESS - Pushed 2 groups
```

**解读**: 向右推挤, 推动了 2 个组, 每个块精确对齐到网格

---

#### 成功交换
```
trySwapInteraction: Attempting GRID-BASED swap
  Moved group original cells: 3
  Target group cells: 2
  Moved delta: (1, 0)
  Target delta: (-1, 0)
  Moved piece[6] to grid (3, 1) at pixel (218.4, 1032.0)
  Moved piece[10] to grid (2, 1) at pixel (218.4, 857.25)
trySwapInteraction: SUCCESS - Swapped positions
```

**解读**: L形(3格子) 与 2格子块交换, 两个组分别移动 1 行

---

## 调试技巧

### 1. 识别卡死的块
搜索日志: `piece[X] snapped back`  
→ 这个 piece 所在的组无法移动,查看它的 `groupId` 和形状

### 2. 追踪格子占用
在 `ACTION_DOWN` 时,日志会输出:
```
Drag start: piece[X] at (pixelX, pixelY)
```
对比 `piece.row` 和 `piece.col` 可验证格子计算是否正确

### 3. 验证边界限制
查看 `puzzleBounds`: 
```
isValidPlacement: Out of bounds piece[X] at (x, y). Bounds: RectF(...)
```
如果 Bounds 不正确,检查 `updateLayout` 函数

### 4. 检测碰撞误判
如果某个移动"应该成功但失败了",添加日志:
```kotlin
Log.d(TAG, "Occupied cells: $occupiedCells")
Log.d(TAG, "Target cells: $targetCells")
Log.d(TAG, "Conflict: ${targetCells.intersect(occupiedCells)}")
```

---

## 常见问题 FAQ

### Q: 为什么L形块拖到空地也会被还原?
A: 检查 `getCurrentGridPosition` 的计算,可能是:
- `puzzleBounds` 计算错误(偏移不对)
- 像素到格子的舍入逻辑有误(`round` vs `floor`)

### Q: 推挤时为什么只移动了部分块?
A: 检查 `getGridOccupancy` 是否正确返回了组的所有 piece。确认所有 piece 的 `groupId` 一致。

### Q: 交换后块的位置偏移了?
A: 网格引擎应该已消除这个问题。如果仍发生,检查 `moveGroupToGridCells` 中的计算:
```kotlin
piece.currentX = puzzleBounds.left + targetCell.col * cellW
piece.currentY = puzzleBounds.top + targetCell.row * cellH
```

### Q: 如何回滚到旧引擎?
A: 
1. 取消注释旧函数 (搜索 `LEGACY:`)
2. 恢复 `handleActionUp` 调用旧的 `tryInteraction`
3. 重新编译

---

**更新**: 2025-11-23  
**版本**: Grid Engine v1.0


