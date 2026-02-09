import UIKit
import SwiftUI

struct ImageSlicer {
    
    /// 与 Android Bitmap.createBitmap 一致：按像素坐标切片
    static func slice(image originalImage: UIImage, rows: Int, cols: Int) -> [PuzzlePiece] {
        var pieces: [PuzzlePiece] = []
        
        // 归一化方向后使用 cgImage 像素坐标，与 Android 完全一致
        let image = originalImage.imageOrientation == .up ? originalImage : normalizeOrientation(originalImage)
        guard let cgImage = image.cgImage else {
            print("❌ ImageSlicer: No cgImage after normalize")
            return pieces
        }
        
        let width = cgImage.width
        let height = cgImage.height
        let pieceWidth = width / cols
        let pieceHeight = height / rows
        
        guard pieceWidth > 0, pieceHeight > 0 else {
            print("❌ ImageSlicer: Invalid dimensions \(width)x\(height) for \(rows)x\(cols)")
            return pieces
        }
        
        var idCounter = 0
        
        let bounds = CGRect(x: 0, y: 0, width: width, height: height)
        
        for row in 0..<rows {
            for col in 0..<cols {
                let x = col * pieceWidth
                let y = row * pieceHeight
                var rect = CGRect(x: x, y: y, width: pieceWidth, height: pieceHeight).integral
                rect = rect.intersection(bounds)
                
                // WebP/CGImage.cropping 可能产生黑块，强制用 UIGraphicsImageRenderer 绘制（Android Bitmap 无此问题）
                let pieceImage: UIImage
                if rect.width < 1 || rect.height < 1 {
                    pieceImage = createPlaceholder(width: pieceWidth, height: pieceHeight)
                } else {
                    pieceImage = renderSliceByDraw(from: image, rect: rect, cgImage: cgImage)
                }
                #if DEBUG
                if pieceImage.cgImage == nil {
                    print("⚠️ ImageSlicer: Piece \(idCounter) slice failed cgImage")
                }
                #endif
                
                let piece = PuzzlePiece(
                    id: idCounter,
                    currentX: 0,
                    currentY: 0,
                    targetX: 0,
                    targetY: 0,
                    width: CGFloat(pieceWidth),
                    height: CGFloat(pieceHeight),
                    image: pieceImage,
                    zIndex: 0,
                    isLocked: false,
                    groupId: idCounter,
                    row: row,
                    col: col
                )
                pieces.append(piece)
                idCounter += 1
            }
        }
        
        return pieces
    }
    
    /// 归一化方向，确保 cgImage 与 size 对应
    private static func normalizeOrientation(_ image: UIImage) -> UIImage {
        guard image.imageOrientation != .up else { return image }
        let size = image.size
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }
    
    /// 裁切：优先 CGImage.cropping，失败则用 UIGraphicsImageRenderer（WebP 等格式可能需后者）
    private static func renderSliceByDraw(from image: UIImage, rect: CGRect, cgImage: CGImage) -> UIImage {
        guard rect.width > 0, rect.height > 0 else {
            return createPlaceholder(width: 1, height: 1)
        }
        if let cropped = cgImage.cropping(to: rect) {
            return UIImage(cgImage: cropped, scale: 1, orientation: .up)
        }
        let scale = image.scale
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: rect.width, height: rect.height), format: format)
        return renderer.image { _ in
            image.draw(at: CGPoint(x: -rect.origin.x / scale, y: -rect.origin.y / scale))
        }
    }
    
    private static func createPlaceholder(width: Int, height: Int) -> UIImage {
        let w = max(1, width)
        let h = max(1, height)
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: w, height: h))
        return renderer.image { ctx in
            UIColor.darkGray.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))
        }
    }
}
