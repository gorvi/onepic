import SwiftUI

/// Android parity: Hint overlay - red source frame, flying ghost, green target frame
struct HintOverlayView: View {
    let piece: PuzzlePiece?
    let target: CGPoint?
    let progress: CGFloat
    
    var body: some View {
        if let piece = piece, let target = target {
            // 关键修复：GameBoardView 中拼图容器偏移为 20，此处同步修正
            let containerOffsetY: CGFloat = 20
            let sourceCenter = CGPoint(x: piece.currentX + piece.width / 2, y: piece.currentY + piece.height / 2 + containerOffsetY)
            let targetCenterWithOffset = CGPoint(x: target.x, y: target.y + containerOffsetY)
            
            let ghostX = sourceCenter.x + (targetCenterWithOffset.x - sourceCenter.x) * progress
            let ghostY = sourceCenter.y + (targetCenterWithOffset.y - sourceCenter.y) * progress
            
            ZStack {
                // 1. Source Frame (Red) - Android style
                DashedRectView(center: sourceCenter, width: piece.width, height: piece.height, color: .red.opacity(0.8), strokeWidth: 4)
                    .opacity(1.0 - progress) // Gradually disappear as ghost flies
                
                // 2. Target Frame (Green) - Android style
                DashedRectView(center: targetCenterWithOffset, width: piece.width, height: piece.height, color: .green.opacity(0.8), strokeWidth: 4)
                
                // 3. Ghost (semi-transparent piece) at interpolated position
                Image(uiImage: piece.image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: piece.width, height: piece.height)
                    .clipShape(RoundedRectangle(cornerRadius: 1))
                    .opacity(0.7)
                    .position(x: ghostX, y: ghostY)
            }
            .allowsHitTesting(false)
        }
    }
}

private struct DashedRectView: View {
    let center: CGPoint
    let width: CGFloat
    let height: CGFloat
    let color: Color
    let strokeWidth: CGFloat
    
    var body: some View {
        Rectangle()
            .stroke(style: StrokeStyle(lineWidth: strokeWidth, dash: [10, 10]))
            .foregroundColor(color)
            .frame(width: width, height: height)
            .position(center)
    }
}
