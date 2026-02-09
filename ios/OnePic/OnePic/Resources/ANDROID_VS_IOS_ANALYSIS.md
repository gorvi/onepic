# Android vs iOS 拼图逻辑深度分析

## 一、坐标系根本差异

### Android：左上角坐标系
```
piece.currentX, piece.currentY = 块左上角
右边界 = currentX + width
底边界 = currentY + height

网格换算：col = round((currentX - puzzleBounds.left) / cellW)
        row = round((currentY - puzzleBounds.top) / cellH)
```

### iOS：中心点坐标系
```
piece.currentX, piece.currentY = 块中心 (SwiftUI .position)
左上角 = (currentX - width/2, currentY - height/2)

网格换算需先转为左上角：
topLeftX = currentX - cellWidth/2
topLeftY = currentY - cellHeight/2
col = round((topLeftX - boardOffsetX) / cellWidth)
row = round((topLeftY - boardOffsetY) / cellHeight)
```

---

## 二、Android 的 row/col 与 currentX/Y 同步机制

### 关键：updateLayout 强制同步
```kotlin
// GameBoardView.kt updateLayout() - 仅在 onSizeChanged 时调用
pieces.forEach { piece ->
    piece.currentX = offsetX + piece.col * newPieceWidth   // 从 row/col 推导
    piece.currentY = offsetY + piece.row * newPieceHeight
}
```
- **row/col 是逻辑真源**：布局时用 row/col 重算 currentX/Y
- 游戏中 updateLayout 不触发，仅靠各操作维护一致性

### row/col 的更新时机（Android）
| 操作 | 是否更新 row/col |
|------|-----------------|
| setPieces (scramble) | ✅ 从 slots 赋值 |
| moveGroupToGridCells | ✅ 同时更新 currentX/Y 和 row/col |
| revertMove | ✅ 从 dragStartStates 恢复 |
| **mergeGroups** | ❌ **不更新** |

### 结论：Android 合并后 row/col 会落后
- mergeGroups 只改 currentX/Y 和 groupId，不改 row/col
- 合并后 source 块的 row/col 仍是合并前值
- 后续依赖 row/col 的逻辑可能用到过期数据

---

## 三、Android 的双轨逻辑：getGridOccupancy vs getCurrentGridPosition

| 函数 | 数据源 | 用途 |
|------|--------|------|
| getGridOccupancy | piece.row, piece.col | swap/push 的 origin、conflicting、vacated |
| getCurrentGridPosition | piece.currentX/Y → 像素换算 | tryGridSnap、isValidPlacement、isPositionValid |

- **swap / push**：用 row/col，合并后可能不准
- **tryGridSnap / isValidPlacement**：用 currentX/Y，始终反映真实位置

---

## 四、黑块根因分析

### 可能原因 1：逻辑空格子（最可能）
- row/col 与 currentX/Y 不同步
- 某格逻辑上“空”，但视觉上有块；或反过来
- 导致：swap/move 决策错误 → 块被移到错位 → 出现空格子（黑块）

### 可能原因 2：驱离逻辑过于激进
- 合并后驱离“占位”块到 solved 位置
- 若目标格已被占，或驱离顺序不当，可能产生新重叠或空格子

### 可能原因 3：moveGroupToGridCells 源位置错误
- 用 getGridOccupancy（row/col）做源位置时，若 row/col 已漂移
- 相对偏移计算错误 → 块被放到错误格子

### 可能原因 4：图片切片（次要）
- Android：Bitmap.createBitmap 直接像素裁剪
- iOS：CGImage.cropping / UIGraphicsImageRenderer，WebP 可能异常
- 若切片失败，会看到灰块/黑块，但通常会有 ⚠️ 日志

---

## 五、Android 的 moveGroupToGridCells 实现

```kotlin
// 使用 piece.row, piece.col 作为源
val sourceOccupancy = getGridOccupancy(group)  // row/col
group.forEach { piece ->
    val sourceCell = GridCell(piece.row, piece.col)
    val offsetRow = piece.row - sourceMinRow
    val offsetCol = piece.col - sourceMinCol
    val targetCell = GridCell(targetMinRow + offsetRow, targetMinCol + offsetCol)
    piece.currentX = puzzleBounds.left + targetCell.col * cellW
    piece.currentY = puzzleBounds.top + targetCell.row * cellH
    piece.row = targetCell.row   // 同时更新 row/col
    piece.col = targetCell.col
}
```
- 源和目标都来自 row/col
- 移完后会同时修正 row/col 和 currentX/Y，保证这次移动后一致

---

## 六、根本原因总结

**核心矛盾**：Android 在 merge 后不更新 row/col，但 swap/push 仍依赖 row/col，存在潜在不一致。Android 能相对稳定，可能因为：
1. tryGridSnap / isValidPlacement 用 getCurrentGridPosition，对冲了部分错误
2. 常见操作序列（如先 move 再 merge）能通过 moveGroupToGridCells 修正 row/col
3. 某些边界路径在 Android 上较少触发

**iOS 的问题**：
1. 中心坐标系与 Android 左上角不同，换算易出错
2. 合并后既改 currentX/Y 又改 row/col，但公式/驱离逻辑有误，易产生重叠或空格子
3. 多次 swap/merge 后，row/col 与像素位置的累积偏差更大

---

## 七、推荐修复方向

1. **统一采用像素真源**：所有 occupancy / 冲突判断一律用 `toGridFromPieceCenter(currentX, currentY)`，不再信任 piece.row/col 做逻辑判断。
2. **merge 后只做一次全局同步**：合并完成后，对**整盘**所有块执行 `row/col = toGridFromPieceCenter(currentX, currentY)`，保证逻辑与视觉一致。
3. **弱化或移除驱离逻辑**：仅在确定不会造成新冲突时才驱离；或直接依赖全局 sync，不做额外驱离。
4. **moveGroupToGridCells 源用像素**：源 occupancy 用 getCurrentGridPosition（像素换算），与第 1 点一致。
