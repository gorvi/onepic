# 拼图切片算法说明

## 一、预期行为（与 Android 一致）

1. **每块应显示原图对应区域**：如 12 块 = 3 行 × 4 列，每块是原图的一个矩形切片，无黑块、无缺失。
2. **坐标体系**：Android 用 `Bitmap.createBitmap(orig, x, y, w, h)` 按像素裁切；iOS 使用 `cgImage` 像素坐标（`cgImage.width/height`）做等价格式裁切。
3. **输出**：每块为 `pieceWidth × pieceHeight` 像素的位图，iOS 输出 `scale: 1` 便于 SwiftUI 显示。

### Android 对照（`ImageSlicer.kt`）

- `pieceWidth = originalBitmap.width / cols`，`pieceHeight = originalBitmap.height / rows`
- 每块：`Bitmap.createBitmap(originalBitmap, x, y, pieceWidth, pieceHeight)`
- 像素坐标由 `GameBoardView.updateLayout()` 根据屏幕尺寸后续计算；切片只负责按网格切图并生成 `PuzzlePiece`。

---

## 二、当前实现（`ImageSlicer.swift`）

### 2.1 主流程 `slice(image:rows:cols:)`

```
1. 归一化方向：若 imageOrientation != .up，先 normalizeOrientation() 得到 .up 的 UIImage
2. 取 cgImage，用其像素尺寸：width = cgImage.width, height = cgImage.height
3. pieceWidth = width / cols, pieceHeight = height / rows；若 ≤0 直接返回空数组
4. bounds = CGRect(x:0, y:0, width:width, height:height)
5. 遍历 row in 0..<rows, col in 0..<cols：
   - x = col * pieceWidth, y = row * pieceHeight
   - rect = CGRect(x, y, pieceWidth, pieceHeight).integral，再 rect = rect.intersection(bounds)
   - 若 rect.width < 1 或 rect.height < 1 → createPlaceholder(pieceWidth, pieceHeight)
   - 否则 → renderSliceByDraw(from: image, rect: rect, cgImage: cgImage)（内部先尝试 cropping，失败再 draw）
6. 用 pieceImage 创建 PuzzlePiece（id, currentX/Y=0, targetX/Y=0, width/height=像素宽高, image, zIndex=0, isLocked=false, groupId=id, row, col）
7. 返回 pieces
```

说明：裁切**统一**在 `renderSliceByDraw` 内完成；主流程不直接调用 `cgImage.cropping`。因 WebP 等格式下 `CGImage.cropping` 可能产生黑块，当前实现**始终**走 `renderSliceByDraw`，由该函数内部决定用 cropping 还是 draw。

### 2.2 裁切逻辑 `renderSliceByDraw(from:rect:cgImage:)`

**双路径（在函数内部）：**

| 路径 | 条件 | 说明 |
|------|------|------|
| `cgImage.cropping(to: rect)` | 成功时 | 返回 `UIImage(cgImage: cropped, scale: 1, orientation: .up)`，直接像素拷贝 |
| `UIGraphicsImageRenderer` | cropping 返回 nil（如部分 WebP） | 用 `image.draw(at:)` 绘制到固定尺寸画布 |

**draw 路径要点（rect 为像素，draw 使用点坐标）：**

- `rect.origin.x / y`、`rect.size` 均为像素。
- 点坐标与像素关系：`points = pixels / image.scale`。
- 偏移：`image.draw(at: CGPoint(x: -rect.origin.x / scale, y: -rect.origin.y / scale))`，使要露出的区域落在渲染器的 (0,0)-(rect.width, rect.height) 内。
- 渲染器：`UIGraphicsImageRenderer(size: CGSize(width: rect.width, height: rect.height), format: format)`，`format.scale = 1`，输出像素尺寸 = rect.size。

### 2.3 黑块根因（已修复）

| 问题 | 错误写法 | 正确写法 |
|------|----------|----------|
| 未考虑 scale | `translateBy(-rect.x, -rect.y)` 或 `draw(at: CGPoint(-rect.x, -rect.y))` | `draw(at: CGPoint(x: -rect.origin.x/scale, y: -rect.origin.y/scale))` |
| 后果 | 像素当点用，偏移错误，裁切区域落入负象限 → **输出黑块** | 裁切区域正确，显示正常 |

---

## 三、归一化方向 `normalizeOrientation(_:)`

- 当 `image.imageOrientation == .up` 时直接返回原图。
- 否则用 `UIGraphicsImageRenderer(size: image.size)` 重绘：`image.draw(in: CGRect(origin: .zero, size: size))`，得到方向为 `.up` 的新图，保证后续 `cgImage` 与 `size` 一一对应。

---

## 四、占位符 `createPlaceholder(width:height:)`

- 调用时机：`rect.width < 1 || rect.height < 1` 时在主流程用占位；`renderSliceByDraw` 内 rect 无效时也返回占位。
- 实现：`w = max(1, width)`，`h = max(1, height)`，`UIGraphicsImageRenderer` 填充 `UIColor.darkGray` 的矩形，尺寸为 w×h。

---

## 五、后续处理（GameViewModel）

- 切片后 `pieces[i].width/height` 为**图像像素尺寸**（CGFloat(pieceWidth/Height)）。
- `loadLevel` 中会把这些覆盖为 `cellWidth` / `cellHeight`（视图格子尺寸），以匹配棋盘布局与拖拽坐标。
- 拼图块初始 `groupId = id`（每块独立），`row/col` 表示正确网格位置，用于判定完成与目标格。
