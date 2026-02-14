import SwiftUI

/// Android parity: Hint ghost card animation
struct HintOverlayView: View {
    let piece: PuzzlePiece
    /// 目标左上角坐标（与 Android 的 hintTargetX/Y 一致）
    let target: CGPoint
    let progress: CGFloat
    /// 与拼图渲染层保持一致的全局垂直偏移（由外部统一传入，避免硬编码漂移）
    let visualOffsetY: CGFloat
    
    var body: some View {
        let startX = piece.currentX
        let startY = piece.currentY + visualOffsetY
        
        // target 是目标左上角，保持与 Android 一致
        let destX = target.x
        let destY = target.y + visualOffsetY
        
        // 颜色插值：从红到绿
        let animatedColor = Color(
            red: 1.0 - Double(progress),
            green: Double(progress),
            blue: 0.0,
            opacity: 0.9
        )
        
        ZStack(alignment: .topLeading) {
            // 1. 起始位置：红色虚线框
            Rectangle()
                .stroke(style: StrokeStyle(lineWidth: 2, lineCap: .butt, lineJoin: .miter, dash: [6, 3]))
                .foregroundColor(.red.opacity(0.8))
                .frame(width: piece.width, height: piece.height)
                .position(x: startX + piece.width / 2, y: startY + piece.height / 2)
            
            // 2. 目标位置：绿色虚线框
            Rectangle()
                .stroke(style: StrokeStyle(lineWidth: 2, lineCap: .butt, lineJoin: .miter, dash: [6, 3]))
                .foregroundColor(.green.opacity(0.8))
                .frame(width: piece.width, height: piece.height)
                .position(x: destX + piece.width / 2, y: destY + piece.height / 2)

            // 3. 移动幽灵：卡片图像 + 变色边框
            let ghostX = startX * (1 - progress) + destX * progress
            let ghostY = startY * (1 - progress) + destY * progress
            
            ZStack {
                Image(uiImage: piece.image)
                    .resizable()
                    .frame(width: piece.width, height: piece.height)
                    .opacity(0.5)
                
                Rectangle()
                    .stroke(style: StrokeStyle(lineWidth: 2, lineCap: .butt, lineJoin: .miter, dash: [8, 4]))
                    .foregroundColor(animatedColor)
                    .frame(width: piece.width, height: piece.height)
            }
            .position(x: ghostX + piece.width / 2, y: ghostY + piece.height / 2)
        }
    }
}
