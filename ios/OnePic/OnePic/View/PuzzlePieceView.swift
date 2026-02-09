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
                    .drawingGroup() 
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.5))
            }
        }
        .frame(width: CGFloat(piece.width), height: CGFloat(piece.height))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        .overlay(
            // 马卡龙渐变边框
            RoundedRectangle(cornerRadius: cornerRadius)
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
