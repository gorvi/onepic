import SwiftUI

struct PuzzlePieceView: View {
    let piece: PuzzlePiece
    var isWin: Bool = false
    
    private var cornerRadius: CGFloat {
        isWin ? 0 : 3
    }
    private let gapStrokeWidth: CGFloat = 1
    
    var body: some View {
        Group {
            if piece.image.cgImage != nil {
                Image(uiImage: piece.image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.5))
            }
        }
        .frame(width: CGFloat(piece.width), height: CGFloat(piece.height))
        .clipShape(SelectiveRoundedRectangle(radius: cornerRadius, hiddenEdges: piece.hiddenEdges))
        .overlay(
            SelectiveEdgeShape(hiddenEdges: piece.hiddenEdges)
                .stroke(
                    LinearGradient(
                        colors: [
                            Color(hex: 0xFFB2EBF2),
                            Color(hex: 0xFFFFC1CC),
                            Color(hex: 0xFFB2FBDA),
                            Color(hex: 0xFFFFF176)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ).opacity(isWin ? 0 : 0.85),
                    lineWidth: gapStrokeWidth + 0.5
                )
        )
        .shadow(color: Color(hex: 0x2979FF).opacity(isWin ? 0 : (piece.isLocked ? 0.3 : 0.6)), radius: isWin ? 0 : (piece.isLocked ? 4 : 8))
        .position(x: piece.currentX + piece.width / 2, y: piece.currentY + piece.height / 2)
        .zIndex(Double(piece.zIndex))
        .animation(.easeInOut(duration: 0.8), value: isWin)
    }
}

/// 自定義形狀，僅在未隱藏的邊緣繪製線條
struct SelectiveEdgeShape: Shape {
    var hiddenEdges: Set<PuzzlePiece.Edge>
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        
        // Top
        if !hiddenEdges.contains(.top) {
            path.move(to: CGPoint(x: rect.minX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        }
        
        // Right
        if !hiddenEdges.contains(.right) {
            path.move(to: CGPoint(x: rect.maxX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        }
        
        // Bottom
        if !hiddenEdges.contains(.bottom) {
            path.move(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        }
        
        // Left
        if !hiddenEdges.contains(.left) {
            path.move(to: CGPoint(x: rect.minX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        }
        
        return path
    }
}

/// 自定義剪切形狀，合並邊不使用圓角
struct SelectiveRoundedRectangle: Shape {
    var radius: CGFloat
    var hiddenEdges: Set<PuzzlePiece.Edge>
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: getCornersToRound(),
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
    
    private func getCornersToRound() -> UIRectCorner {
        var corners: UIRectCorner = []
        
        // 如果 top 和 left 都沒隱藏，左上角圓角
        if !hiddenEdges.contains(.top) && !hiddenEdges.contains(.left) {
            corners.insert(.topLeft)
        }
        // 如果 top 和 right 都沒隱藏，右上角圓角
        if !hiddenEdges.contains(.top) && !hiddenEdges.contains(.right) {
            corners.insert(.topRight)
        }
        // 如果 bottom 和 left 都沒隱藏，左下角圓角
        if !hiddenEdges.contains(.bottom) && !hiddenEdges.contains(.left) {
            corners.insert(.bottomLeft)
        }
        // 如果 bottom 和 right 都沒隱藏，右下角圓角
        if !hiddenEdges.contains(.bottom) && !hiddenEdges.contains(.right) {
            corners.insert(.bottomRight)
        }
        
        return corners
    }
}
