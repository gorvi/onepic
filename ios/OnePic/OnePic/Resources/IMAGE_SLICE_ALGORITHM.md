# 拼图切片算法说明

## 预期行为（与 Android 一致）

1. **每块应显示原图对应区域**：12 块拼图 = 3 行 × 4 列，每块是原图的一个矩形切片，无黑块、无缺失
2. **坐标体系**：Android 用 Bitmap 像素坐标，直接 `Bitmap.createBitmap(orig, x, y, w, h)` 拷贝像素
3. **iOS 对应**：需用像素级拷贝，输出 `pieceWidth × pieceHeight` 像素的位图

## 黑块根因分析

1. **UIImage 坐标系**：`draw(at:)` 使用点坐标，`scale` 影响尺寸
2. **当前 bug**：`renderSliceByDraw` 中 `translateBy(-rect.x, -rect.y)` 未考虑 `image.scale`，导致绘制偏移到负象限，输出为黑
3. **正确转换**：rect 为像素，需 `translateBy(-rect.x/scale, -rect.y/scale)` 使图像点 (rect.x/scale, rect.y/scale) 对齐到 (0,0)

## 合理算法

1. 归一化方向 → 确保 `.up`
2. 用 `CGContext` 或 `UIGraphicsImageRenderer` 输出 `rect.width × rect.height` 像素
3. 绘制时：`translateBy(-rect.x/scale, -rect.y/scale)` 或等价变换，保证裁切区域正确
4. 输出 `scale: 1`，便于 SwiftUI 缩放显示
