import SwiftUI

struct PathConnectorView: View {
    let stageIndex: Int
    let isCompleted: Bool
    
    var body: some View {
        let colors = ThemeUtils.getStageColors(stageIndex: stageIndex)
        let symbol = ThemeUtils.getConnectorSymbol(stageIndex: stageIndex)
        
        VStack(spacing: 4) {
            ForEach(0..<3) { index in
                Text(symbol)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(
                        isCompleted
                        ? colors[index % 2].opacity(0.8)
                        : Color.white.opacity(0.4)
                    )
            }
        }
        .frame(height: 60)
        .allowsHitTesting(false)
    }
}
