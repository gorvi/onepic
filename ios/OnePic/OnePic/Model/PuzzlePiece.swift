import Foundation
import SwiftUI
import UIKit

// Verified: PuzzlePiece is a struct.
// In GameViewModel loop `for i in 0..<self.pieces.count`, we are modifying `self.pieces[i]`.
// Since `pieces` is @Published var, this is correct and will trigger UI update.

struct PuzzlePiece: Identifiable {
    enum Edge {
        case top, bottom, left, right
    }
    
    let id: Int
    var currentX: CGFloat
    var currentY: CGFloat
    var targetX: CGFloat
    var targetY: CGFloat
    var width: CGFloat
    var height: CGFloat
    var image: UIImage
    var zIndex: Int
    var isLocked: Bool = false
    var groupId: Int = -1
    var row: Int
    var col: Int
    var hiddenEdges: Set<Edge> = []
}
