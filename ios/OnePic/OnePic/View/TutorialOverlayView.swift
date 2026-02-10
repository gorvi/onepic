import SwiftUI

struct TutorialOverlayView: View {
    let step: Int
    @State private var alpha: Double = 0.6
    
    // 获取多语言翻译
    private var currentText: String {
        if step == 0 {
            let welcome = TRANS.get("tut_welcome", "Welcome to OnePic!")
            let step1 = TRANS.get("tut_step_1", "Swap 2 and 4")
            return "🚀 \(welcome)\n👉 \(step1)"
        } else {
            let step2 = TRANS.get("tut_step_double_tap", "Double tap to unmerge")
            return "👆 \(step2)"
        }
    }
    
    var body: some View {
        VStack(spacing: 20) {
            Text(currentText)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
                .background(
                    ZStack {
                        Capsule()
                            .fill(Color.black.opacity(0.7))
                        Capsule()
                            .stroke(Color.cyan.opacity(0.5), lineWidth: 2)
                    }
                )
                .opacity(alpha)
                .shadow(color: .cyan.opacity(0.3), radius: 10)
            
            if step == 0 {
                Image(systemName: "chevron.down")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.cyan)
                    .opacity(alpha)
                    .offset(y: alpha * 10 - 5) // 简单的上下浮动效果
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true)) {
                alpha = 1.0
            }
        }
    }
}

#Preview {
    ZStack {
        Color.gray
        TutorialOverlayView(step: 0)
    }
}
