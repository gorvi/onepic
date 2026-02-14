import SwiftUI
import Combine

struct FloatingBuffWindow: View {
    @ObservedObject var progressManager = LevelProgressManager.shared
    
    // 显示模式：展开(HUD) 或 收起(Ball)
    @State private var isExpanded: Bool = true
    
    // 拖拽状态
    @State private var offset: CGSize = .zero
    @State private var isDragging: Bool = false
    
    // 动画状态
    @State private var rotation: Double = 0
    @State private var isGlowing: Bool = false
    
    let timer = Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()
    @State private var refreshID = UUID() // 强制 UI 刷新的 Hook
    
    var body: some View {
        if progressManager.isBuffActive || progressManager.isWarmUpActive {
            ZStack {
                if isExpanded {
                    expandedHUD
                        .transition(.asymmetric(
                            insertion: .move(edge: .top).combined(with: .opacity),
                            removal: .scale(scale: 0.2, anchor: .top).combined(with: .opacity)
                        ))
                } else {
                    collapsedBall
                        .transition(.asymmetric(
                            insertion: .scale(scale: 0.2).combined(with: .opacity),
                            removal: .opacity
                        ))
                }
            }
            .animation(.spring(response: 0.4, dampingFraction: 0.7), value: isExpanded)
            .onReceive(timer) { _ in
                // 1. 更新数据模型状态
                progressManager.updateBuffState()
                
                // 2. 🚨 关键修复：更新内部刷新 ID 强制 SwiftUI 重绘视图树
                // 解决 @ObservedObject 在某些嵌套布局中由于状态更新太快被合并导致的计时器“停滞”视觉错误
                refreshID = UUID()
                
                // 3. 辅助信号发送
                progressManager.objectWillChange.send()
            }
            .id(refreshID) // 绑定唯一 ID 强制刷新
        } else {
            EmptyView()
        }
    }
    
    // MARK: - Expanded HUD (Banner Style)
    private var expandedHUD: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 10) { // 改为顶部对齐，支持多行
                // 图标区域 - 固定宽度并与首行对齐
                ZStack {
                    Circle()
                        .fill(themeColor.opacity(0.15))
                        .frame(width: 38, height: 38)
                    
                    Image(systemName: progressManager.isBuffWarmingUp() ? "bolt.fill" : "star.fill")
                        .font(.system(size: 18, weight: .bold)) // 调大图标
                        .foregroundColor(themeColor)
                }
                .frame(width: 40)
                .padding(.top, 2) // 微调图标位置，与首行文字视觉对齐
                
                // 文字内容区域 - 支持自动换行，字号更大
                VStack(alignment: .leading, spacing: 4) { // 增加行间距
                    Text(progressManager.isBuffWarmingUp() ? TRANS.get("hud_buff_warming", "SYSTEM_WARMING_UP...") : TRANS.get("hud_buff_active", "DOUBLE_COINS: ACTIVE"))
                        .font(.system(size: 15, weight: .black)) // 大字号
                        .foregroundColor(themeColor)
                        .kerning(0.5)
                        .lineLimit(nil) // 允许换行
                        .fixedSize(horizontal: false, vertical: true) // 允许垂直伸展
                    
                    Text(progressManager.isBuffWarmingUp() ? TRANS.get("hud_buff_hint_prepare", "PREPARE_FOR_GAME_START") : TRANS.get("hud_buff_hint_overclock", "OVERCLOCKING_COLLECTOR"))
                        .font(.system(size: 11, weight: .bold)) // 大字号
                        .foregroundColor(.white.opacity(0.65))
                        .lineLimit(nil) // 允许换行
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .layoutPriority(1)
                
                // 倒计时区域 - 固定宽度且与首行对齐
                let remaining = calculateRemainingSeconds()
                Text(formatTime(remaining))
                    .font(.system(size: 20, weight: .black, design: .monospaced))
                    .foregroundColor(themeColor)
                    .frame(width: 65, alignment: .trailing)
                    .fixedSize(horizontal: true, vertical: false)
                    .padding(.top, 4) // 对齐首行文字中心
                
                // 折叠按钮
                Button(action: {
                    withAnimation(.spring()) {
                        isExpanded = false
                        // 收起时移动到星星左边（右上角偏左位置，不遮挡其他元素）
                        let screenWidth = UIScreen.main.bounds.width
                        offset = CGSize(width: screenWidth / 2 - 120, height: -UIScreen.main.bounds.height / 2 + 100)
                    }
                }) {
                    Image(systemName: "chevron.right.circle.fill")
                        .font(.system(size: 22))
                        .foregroundColor(.white.opacity(0.4))
                }
                .frame(width: 25)
                .padding(.top, 4) // 对齐首行
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 14) // 增加上下内边距，适应多行内容
            
            // 底部进度条
            GeometryReader { geo in
                let remaining = calculateRemainingSeconds()
                let isWarmingUp = progressManager.isBuffWarmingUp()
                let progress = isWarmingUp 
                    ? CGFloat(remaining) / 10.0
                    : CGFloat(remaining) / (CGFloat(progressManager.getConsecutiveDays() * 60 + 180))
                
                Rectangle()
                    .fill(themeColor)
                    .frame(width: geo.size.width * min(progress, 1.0))
                    .shadow(color: themeColor.opacity(0.7), radius: 2)
            }
            .frame(height: 3)
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color.black.opacity(0.75))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(
                            LinearGradient(colors: [themeColor.opacity(0.8), .clear, themeColor.opacity(0.6)], startPoint: .leading, endPoint: .trailing),
                            lineWidth: 2
                        )
                )
        )
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .padding(.horizontal, 14)
        .padding(.top, 44)
        .offset(y: isExpanded ? offset.height : 0)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if isExpanded {
                        offset.height = value.translation.height
                    }
                }
                .onEnded { value in
                    if isExpanded {
                        withAnimation(.spring()) {
                            if offset.height > 120 {
                                isExpanded = false
                                snapToEdge()
                            } else {
                                offset.height = 0
                            }
                        }
                    }
                }
        )
    }
    
    // MARK: - Collapsed Ball (Existing Tech Ball)
    private var collapsedBall: some View {
        ZStack {
            // 旋转光环
            Circle()
                .trim(from: 0, to: 0.25)
                .stroke(
                    LinearGradient(colors: [themeColor, themeColor.opacity(0)], startPoint: .topLeading, endPoint: .bottomTrailing),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .frame(width: 70, height: 70)
                .rotationEffect(.degrees(rotation))
            
            Circle()
                .trim(from: 0.5, to: 0.75)
                .stroke(
                    LinearGradient(colors: [themeColor.opacity(0.8), themeColor.opacity(0)], startPoint: .bottomTrailing, endPoint: .topLeading),
                    style: StrokeStyle(lineWidth: 3, lineCap: .round)
                )
                .frame(width: 70, height: 70)
                .rotationEffect(.degrees(-rotation))
            
            // 背景 & 阴影
            Circle()
                .fill(Color.black.opacity(0.4))
                .frame(width: 60, height: 60)
                .overlay(Circle().stroke(themeColor.opacity(0.6), lineWidth: 1.5))
                .shadow(color: themeColor.opacity(isGlowing ? 0.8 : 0.4), radius: isGlowing ? 12 : 6)
            
            VStack(spacing: 2) {
                Image(systemName: progressManager.isBuffWarmingUp() ? "bolt.fill" : "2.circle.fill")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(themeColor)
                
                Text(formatTime(calculateRemainingSeconds()))
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(.white)
            }
            
            // 恢复展开的视觉引导 (尖括号提示)
            Image(systemName: "chevron.left")
                .font(.system(size: 10, weight: .black))
                .foregroundColor(themeColor)
                .offset(x: -32)
                .opacity(isGlowing ? 1 : 0.5)
        }
        .frame(width: 80, height: 80)
        .contentShape(Circle())
        .position(x: UIScreen.main.bounds.width / 2 + offset.width,
                  y: UIScreen.main.bounds.height / 2 + offset.height)
        .onAppear {
            withAnimation(.linear(duration: 4).repeatForever(autoreverses: false)) {
                rotation = 360
            }
            withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
                isGlowing = true
            }
        }
        .onTapGesture {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                isExpanded = true
                offset = .zero // 恢复展开时强制居顶
            }
        }
        .gesture(
            DragGesture()
                .onChanged { value in
                    isDragging = true
                    offset = CGSize(
                        width: value.location.x - UIScreen.main.bounds.width / 2,
                        height: value.location.y - UIScreen.main.bounds.height / 2
                    )
                }
                .onEnded { value in
                    // 移除边缘吸附，允许悬浮球停留在任意位置
                    isDragging = false
                }
        )
    }
    
    private var themeColor: Color {
        progressManager.isWarmUpActive ? Color(hex: 0x00B0FF) : Color(hex: 0xFFFFD700)
    }
    
    private func snapToEdge() {
        let screenWidth = UIScreen.main.bounds.width
        let screenHeight = UIScreen.main.bounds.height
        let margin: CGFloat = 20
        
        // 靠左还是靠右
        let targetX = offset.width + 40 < screenWidth / 2 ? -screenWidth / 2 + 50 : screenWidth / 2 - 50
        
        // 限制 Y 轴范围
        let targetY = min(max(offset.height, -screenHeight / 2 + 100), screenHeight / 2 - 150)
        
        offset = CGSize(width: targetX, height: targetY)
    }
    
    private func formatTime(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }

    /// 🚨 核心修复：直接从存储读取时间戳并计算，绕过所有的 Observer 延迟
    /// 对齐 Android LevelProgressManager.kt 逻辑
    private func calculateRemainingSeconds() -> Int {
        if progressManager.isBuffWarmingUp() {
            return progressManager.getWarmUpRemainingSeconds()
        } else if progressManager.isDoubleCoinsActive() {
            return progressManager.getDoubleCoinsRemainingSeconds()
        }
        return 0
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        FloatingBuffWindow()
    }
}
