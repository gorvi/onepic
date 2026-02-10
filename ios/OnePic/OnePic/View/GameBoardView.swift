import SwiftUI

struct GameBoardView: View {
    @StateObject private var viewModel = GameViewModel()
    @State private var activeDragPieceId: Int?
    @State private var lastTapTime: Date?
    @State private var lastTapLocation: CGPoint?
    @State private var showWinOverlay = false  // 延迟显示，先让用户看完整拼图+烟花
    /// 得分动画：0~1 表示从 0 到目标分数的进度，用于弹窗内数字递增
    @State private var scoreRevealProgress: Double = 0
    /// 星星依次出现：索引 i 为 true 表示第 i 颗星已播放出现动画
    @State private var starRevealed: [Bool] = [false, false, false]
    /// 胜利弹窗卡片入场缩放（0.92 -> 1.0）
    @State private var winPopupScale: CGFloat = 0.92
    /// 分数跳动动画状态
    @State private var isScoreAnimating: Bool = false


    let levelConfig: LevelConfig
    var mainLevelIndexForAscended: Int? = nil  // 升级关卡完成时用于 markAscendedLevelCompleted
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject private var tabBarVisibility: TabBarVisibility
    
    /// 本主关卡有升级关卡且刚完成时，在 Win 弹窗显示「已解锁升级关卡」提示
    private var showAscendedHintInWin: Bool {
        guard mainLevelIndexForAscended == nil else { return false }
        let levels = LevelRepository.shared.getClassicLevels()
        guard let idx = levels.firstIndex(where: { $0.levelId == levelConfig.levelId }) else { return false }
        return LevelRepository.shared.getAscendedLevel(mainIndex: idx) != nil
    }
    
    /// 本关当前最佳星星数（拼图页顶部展示，主关/升级关从进度取）
    private var currentBestStars: Int {
        if let mainId = mainLevelIndexForAscended {
            return LevelProgressManager.shared.getAscendedLevelStars(mainLevelId: mainId)
        }
        return LevelProgressManager.shared.getStars(for: levelConfig.levelId)
    }
    
    /// 格式化已用时间为 "分:秒"
    private func formatElapsed(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return "\(m):\(s < 10 ? "0" : "")\(s)"
    }
    
    // MARK: - Shared Styles
    private var barBg: some View {
        Capsule().fill(Color.black.opacity(0.35))
    }
    
    private var actionBtnBg: some View {
        RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.35))
    }
    
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .top) {
                // 1. 全屏背景层
                Group {
                    switch levelConfig.imageSource {
                    case .asset, .resource:
                        ImageUtils.loadImage(source: levelConfig.imageSource)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .blur(radius: 60)
                            .overlay(Color.black.opacity(0.6))
                            .drawingGroup() 
                    default:
                        Color.black
                    }
                }
                .frame(width: geometry.size.width, height: geometry.size.height)
                .contentShape(Rectangle())
                .ignoresSafeArea(.all)
                .zIndex(0)
                
                // Pieces（纯展示）
                ForEach(viewModel.pieces) { piece in
                    PuzzlePieceView(piece: piece, isWin: viewModel.isLevelCompleted)
                        .id("\(piece.id)-\(piece.groupId)")
                        .offset(y: 20) 
                }
                .zIndex(10)
                
                // --- Validation Layer (Green Edge Flow + Surface Shimmer Scan) ---
                if viewModel.isLevelCompleted {
                    ZStack {
                        // 1. Surface Shimmer (The "Scan" animation)
                        SurfaceScanEffect(width: viewModel.boardWidth, height: viewModel.boardHeight)
                        
                        // 2. Green Edge Runner (The dynamic border)
                        GreenScanEffect(width: viewModel.boardWidth, height: viewModel.boardHeight)
                    }
                    .position(
                        x: viewModel.boardOffsetX + viewModel.boardWidth / 2,
                        y: viewModel.boardOffsetY + viewModel.boardHeight / 2 + 20
                    )
                    .allowsHitTesting(false)
                    .zIndex(15)
                }
                
                // Particle overlay (Android parity: merge burst + win fireworks)
                ParticleOverlayView(particleSystem: viewModel.particleSystem)
                    .zIndex(550)
                
                // Hint overlay (Android parity: ghost flying)
                if let hintId = viewModel.hintPieceId,
                   let piece = viewModel.pieces.first(where: { $0.id == hintId }),
                   let target = viewModel.hintTarget {
                    HintOverlayView(piece: piece, target: target, progress: viewModel.hintAnimProgress)
                        .zIndex(600)
                }
                
                // 整版交互层：改用 SimultaneousGesture，并限制感应范围仅限拼图区
                if !showWinOverlay {
                    Color.clear
                        .frame(width: viewModel.boardWidth, height: viewModel.boardHeight)
                    .contentShape(Rectangle())
                    .position(
                        x: viewModel.boardOffsetX + viewModel.boardWidth / 2,
                        y: viewModel.boardOffsetY + viewModel.boardHeight / 2 + 20
                    )
                    .gesture(
                        TapGesture(count: 2)
                            .onEnded { _ in
                                if let loc = lastTapLocation {
                                    print("🎓 DEBUG: SIMULTANEOUS NATIVE DOUBLE TAP at \(loc)")
                                    if let tappedId = viewModel.pieceIdAtLocation(x: loc.x, y: loc.y) {
                                        viewModel.unmergeGroup(pieceId: tappedId)
                                    }
                                }
                            }
                            .simultaneously(with: 
                                DragGesture(minimumDistance: 0)
                                    .onChanged { value in
                                        lastTapLocation = value.startLocation
                                        let pieceId: Int?
                                        if let id = activeDragPieceId {
                                            pieceId = id
                                        } else if let id = viewModel.pieceIdAtLocation(x: value.startLocation.x, y: value.startLocation.y) {
                                            activeDragPieceId = id
                                            pieceId = id
                                        } else {
                                            pieceId = nil
                                        }
                                        if let id = pieceId {
                                            viewModel.onDragChanged(pieceId: id, translation: value.translation)
                                        }
                                    }
                                    .onEnded { value in
                                        if let pieceId = activeDragPieceId {
                                            viewModel.onDragEnded(pieceId: pieceId)
                                            activeDragPieceId = nil
                                        }
                                    }
                            )
                    )
                    .zIndex(2500)
                }
                
                // 3. UI Overlay - 重构为更平衡的布局
                VStack(spacing: 8) { 
                    // --- 第一行：返回(左) & 刷新(右) ---
                    HStack {
                        Button(action: { presentationMode.wrappedValue.dismiss() }) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 36, height: 36)
                                .background(Circle().fill(Color.black.opacity(0.4)))
                                .shadow(color: .black.opacity(0.2), radius: 2)
                        }
                        
                        Spacer()
                        
                        // 刷新按钮移动到第一行最右侧
                        Button(action: {
                            SoundManager.shared.playClick()
                            viewModel.loadLevel(levelConfig, viewSize: geometry.size, mainLevelIndexForAscended: mainLevelIndexForAscended)
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 16, weight: .bold))
                                .frame(width: 36, height: 36)
                                .background(Circle().fill(Color.black.opacity(0.4)))
                                .shadow(color: .black.opacity(0.2), radius: 2)
                        }
                        .foregroundColor(.white)
                    }
                    .padding(.horizontal, 22)
                    .padding(.top, 10) // 从 15 调至 10，几乎贴死顶缘
                    // 取消额外 padding，直接由父级 VStack padding.top 控制
                    
                    
                    HStack {
                        // 左侧：金币
                        HStack(spacing: 6) {
                            CoinIconView(size: 16)
                                .rotation3DEffect(
                                    .degrees(Double(viewModel.scoreEventCount) * 180),
                                    axis: (x: 0, y: 1, z: 0)
                                )
                                .animation(.spring(response: 0.6, dampingFraction: 0.7), value: viewModel.scoreEventCount)
                            
                            Text("\(viewModel.score)")
                                .font(.system(size: 15, weight: .black))
                                .monospacedDigit()
                                .foregroundStyle(
                                    LinearGradient(
                                        colors: isScoreAnimating ? [.yellow, .white] : [.white, .init(white: 0.9)],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .scaleEffect(isScoreAnimating ? 1.2 : 1.0)
                                .shadow(color: isScoreAnimating ? .orange.opacity(0.6) : .clear, radius: 4)
                        }
                        .padding(.horizontal, 12)
                        .frame(height: 36)
                        .background(
                            ZStack {
                                RoundedRectangle(cornerRadius: 18)
                                    .fill(.ultraThinMaterial)
                                    .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)
                                
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(
                                        LinearGradient(
                                            colors: isScoreAnimating 
                                                ? [Color(hex: 0xFFD700), Color(hex: 0xFFA000).opacity(0.5)] 
                                                : [Color.white.opacity(0.2), Color.white.opacity(0.05)],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                        lineWidth: isScoreAnimating ? 1.5 : 0.5
                                    )
                            }
                        )
                        .onChange(of: viewModel.scoreEventCount) { _ in
                            // Trigger pulse animation
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
                                isScoreAnimating = true
                            }
                            
                            // Auto-reset after short delay provided by MainActor
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                withAnimation(.easeOut(duration: 0.2)) {
                                    isScoreAnimating = false
                                }
                            }
                        }

                        
                        Spacer()
                        
                        // 中间：时间
                        HStack(spacing: 4) {
                            Image(systemName: "clock.fill")
                            .font(.system(size: 12))
                            Text(formatElapsed(viewModel.elapsedSeconds))
                                .font(.system(size: 14, weight: .bold))
                                .monospacedDigit()
                        }
                        .padding(.horizontal, 10)
                        .frame(height: 32)
                        .background(barBg)
                        
                        Spacer()
                        
                        // 右侧：星星
                        HStack(spacing: 2) {
                            ForEach(1...3, id: \.self) { n in
                                Image(systemName: n <= currentBestStars ? "star.fill" : "star")
                                    .font(.system(size: 12))
                                    .foregroundStyle(n <= currentBestStars 
                                        ? LinearGradient(colors: [.yellow, .orange], startPoint: .top, endPoint: .bottom)
                                        : LinearGradient(colors: [.white.opacity(0.4), .white.opacity(0.2)], startPoint: .top, endPoint: .bottom))
                            }
                        }
                        .padding(.horizontal, 8)
                        .frame(height: 32)
                        .background(barBg)
                    }
                    .padding(.horizontal, 22)
                    
                    // --- 第三行：提示 (Hint) | 预览图 (Preview) | 广告 (Ad) ---
                    HStack(alignment: .center, spacing: 0) {
                        // 提示按钮 - 缩小尺寸
                        Button(action: { viewModel.showHint() }) {
                            VStack(spacing: 1) {
                                Image(systemName: "lightbulb.fill")
                                    .font(.system(size: 16))
                                Text("\(GameViewModel.HINT_COST)")
                                    .font(.system(size: 9, weight: .bold))
                            }
                            .foregroundColor(viewModel.canAffordHint ? .yellow : .white.opacity(0.4))
                            .frame(width: 48, height: 48)
                            .background(actionBtnBg)
                        }
                        .disabled(!viewModel.canAffordHint)
                        
                        Spacer()
                        
                        // 居中预览展示（悬浮感）
                        if !viewModel.pieces.isEmpty {
                            Group {
                                switch levelConfig.imageSource {
                                case .asset, .resource:
                                    ImageUtils.loadImage(source: levelConfig.imageSource)
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .frame(width: 60, height: 60)
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.4), lineWidth: 1))
                                        .shadow(color: .black.opacity(0.4), radius: 4)
                                default: EmptyView()
                                }
                            }
                        }
                        
                        Spacer()
                        
                        // 广告按钮 - 缩小尺寸
                        Button(action: { viewModel.requestCoinsFromAd() }) {
                            VStack(spacing: 1) {
                                Image(systemName: "video.fill")
                                    .font(.system(size: 16))
                                Text("+\(GameViewModel.AD_REWARD_COINS)")
                                    .font(.system(size: 9, weight: .bold))
                            }
                            .background(actionBtnBg)
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.top, 0)
                .zIndex(2000) // 关键：将 UI 层级提升到 2000，绝对高于拼图块
                .foregroundColor(.white)
                
                // Debug / Empty State
                if viewModel.pieces.isEmpty {
                    Text(TRANS.get("loading", "Loading..."))
                        .foregroundColor(.white)
                        .zIndex(500)
                }
                
                // Tutorial Overlay (Android parity: 文字提示)
                if viewModel.isTutorialMode && viewModel.tutorialStep < 2 && !viewModel.isLevelCompleted {
                    TutorialOverlayView(step: viewModel.tutorialStep)
                        .padding(.top, 140) // 避开状态栏
                        .id("tutorial-step-\(viewModel.tutorialStep)") // 强制刷新
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .move(edge: .top)),
                            removal: .opacity.combined(with: .scale(scale: 0.8))
                        ))
                        .zIndex(3000)
                }

                // Win Overlay（延迟 1.5 秒显示，对齐 Android：先让用户看完整拼图+烟花）
                // Win Overlay（延迟 1.5 秒显示）
                // 优化方案v3：全息悬浮 HUD，大幅上移避开底部，增加透明度
                if showWinOverlay {
                    VStack {
                        Spacer()
                        
                        VStack(spacing: 12) {
                            // 胜利标题 + 装饰
                            HStack {
                                Image(systemName: "trophy.fill")
                                    .foregroundStyle(LinearGradient(colors: [.yellow, .orange], startPoint: .top, endPoint: .bottom))
                                    .font(.system(size: 28))
                                
                                Text(TRANS.get("you_win", "You Win!"))
                                    .font(.system(size: 32, weight: .heavy))
                                    .foregroundStyle(.white)
                                    .shadow(color: .cyan.opacity(0.8), radius: 8)
                            }
                            
                            // 升级提示
                            if showAscendedHintInWin {
                                HStack(spacing: 4) {
                                    Image(systemName: "sparkles")
                                    Text(TRANS.get("asc_unlocked_hint", "New Level Unlocked"))
                                }
                                .font(.subheadline.bold())
                                .foregroundColor(.white)
                                .padding(.vertical, 6)
                                .padding(.horizontal, 16)
                                .background(
                                    LinearGradient(
                                        colors: [Color(hex: 0x9C27B0).opacity(0.4), Color(hex: 0xBA68C8).opacity(0.4)],
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                                .clipShape(Capsule())
                                .shadow(radius: 4)
                            }
                            
                            // 星星获得（1–3 星）：依次弹出动画
                            HStack(spacing: 8) {
                                ForEach(1...3, id: \.self) { n in
                                    let earned = n <= viewModel.completionStars
                                    let revealed = n <= starRevealed.count && starRevealed[n - 1]
                                    Image(systemName: earned ? "star.fill" : "star")
                                        .font(.system(size: 28))
                                        .foregroundStyle(earned
                                            ? LinearGradient(colors: [.yellow, .orange], startPoint: .top, endPoint: .bottom)
                                            : LinearGradient(colors: [.white.opacity(0.4), .white.opacity(0.3)], startPoint: .top, endPoint: .bottom))
                                        .scaleEffect(earned ? (revealed ? 1.0 : 0.2) : 1.0)
                                        .opacity(earned ? (revealed ? 1 : 0) : 1)
                                        .animation(.spring(response: 0.4, dampingFraction: 0.6), value: revealed)
                                }
                            }
                            .padding(.vertical, 4)
                            
                            // 金币显示：从 0 递增到目标值，用金币图标不用文字
                            HStack(spacing: 6) {
                                CoinIconView(size: 24)
                                Text("\(Int(round(Double(viewModel.score) * scoreRevealProgress)))")
                                    .font(.title2.bold())
                                    .foregroundStyle(
                                        LinearGradient(colors: [.white, .cyan], startPoint: .leading, endPoint: .trailing)
                                    )
                            }
                            .padding(.vertical, 4)
                            
                            // 继续按钮
                            Button(action: {
                                presentationMode.wrappedValue.dismiss()
                            }) {
                                Text(TRANS.get("continue", "Continue"))
                                    .font(.title3.bold())
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(
                                        LinearGradient(
                                            colors: [Color(hex: 0x00E5FF).opacity(0.9), Color(hex: 0x2979FF).opacity(0.9)],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .foregroundColor(.white)
                                    .clipShape(Capsule())
                                    .shadow(color: Color(hex: 0x2979FF).opacity(0.4), radius: 8, y: 4)
                                    .overlay(
                                        Capsule()
                                            .stroke(.white.opacity(0.4), lineWidth: 1)
                                    )
                            }
                        }
                        .padding(24)
                        // 透明度优化：使用 ultraThinMaterial + opacity 降低模糊浓度 + 极淡的背景色
                        .background(.ultraThinMaterial.opacity(0.9))
                        .background(Color.black.opacity(0.2))
                        .clipShape(RoundedRectangle(cornerRadius: 28))
                        .overlay(
                            RoundedRectangle(cornerRadius: 28)
                                .stroke(
                                    LinearGradient(
                                        colors: [Color.cyan.opacity(0.5), Color.purple.opacity(0.5), Color.cyan.opacity(0.5)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 1
                                )
                        )
                        .shadow(color: .black.opacity(0.2), radius: 15, y: 8)
                        .padding(.horizontal, 32)
                        .padding(.bottom, 160) // 大幅抬高 (100 -> 160)，彻底脱离底部区域
                        .scaleEffect(winPopupScale)
                        .animation(.spring(response: 0.45, dampingFraction: 0.75), value: winPopupScale)
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            
            // 底部横幅广告
            VStack {
                Spacer()
                BannerAdView(adUnitID: AdConfig.bannerGameId)
                    .frame(height: 50)
                    .background(Color.black.opacity(0.1))
            }
            .edgesIgnoringSafeArea(.bottom)
            .zIndex(6000) // 确保在最顶层
            
            .onAppear {
                tabBarVisibility.hideTabBar = true
                viewModel.loadLevel(levelConfig, viewSize: geometry.size, mainLevelIndexForAscended: mainLevelIndexForAscended)
            }
            .onChange(of: viewModel.isLevelCompleted) { _, completed in
                if completed {
                    // Update: Remove Fusion/Ghost
                    // Just wait for green scan animation (handled in GreenScanEffect onAppear) and then show win overlay
            
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                        showWinOverlay = true
                    }
                } else {
                    showWinOverlay = false
                }
            }
            .onChange(of: showWinOverlay) { _, showing in
                if showing {
                    scoreRevealProgress = 0
                    starRevealed = [false, false, false]
                    winPopupScale = 0.92
                    withAnimation(.spring(response: 0.45, dampingFraction: 0.75)) {
                        winPopupScale = 1.0
                    }
                    withAnimation(.easeOut(duration: 0.6)) {
                        scoreRevealProgress = 1.0
                    }
                    let earned = viewModel.completionStars
                    for i in 0..<min(3, earned) {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25 * Double(i)) {
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
                                var next = starRevealed
                                next[i] = true
                                starRevealed = next
                            }
                        }
                    }
                } else {
                    scoreRevealProgress = 0
                    starRevealed = [false, false, false]
                    winPopupScale = 0.92
                }
            }
            .onChange(of: geometry.size) { _, newSize in
                viewModel.updateLayout(viewSize: newSize)
            }
            .onDisappear {
                tabBarVisibility.hideTabBar = false
                viewModel.clearHint()
                viewModel.stopElapsedTimer()
            }
        }
        .background(
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                Color.black
                    .frame(minHeight: 120)
                    .ignoresSafeArea(edges: .bottom)
            }
            .allowsHitTesting(false)
        )
        .navigationBarHidden(true)
        .toolbar(.hidden, for: .tabBar)
        .ignoresSafeArea(.container, edges: .bottom)
    }
}


/// 边缘顺着拼图边流动的绿光
private struct GreenScanEffect: View {
    let width: CGFloat
    let height: CGFloat
    @State private var phase: CGFloat = 0
    @State private var opacity: Double = 1.0
    
    var body: some View {
        let perimeter = 2 * (width + height)
        let duration = max(0.6, Double(perimeter) / 1500.0) // Adjust speed based on actual size
        
        ZStack {
            // Path for the light runner - Static faint border
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
                .frame(width: width, height: height)
            
            // The Green Light Runner - Improved Flow
            RoundedRectangle(cornerRadius: 12)
                .trim(from: max(0, phase - 0.25), to: phase)
                .stroke(
                    LinearGradient(
                        colors: [
                            Color(hex: 0x69F0AE).opacity(0),
                            Color(hex: 0x69F0AE).opacity(0.6),
                            Color(hex: 0xB9F6CA) 
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    style: StrokeStyle(lineWidth: 6, lineCap: .round) // Wider for better visibility on different sizes
                )
                .frame(width: width, height: height)
                // Removed rotationEffect: RoundedRectangle path naturally starts near top-left or top-center
                .opacity(opacity)
                .onAppear {
                    withAnimation(.linear(duration: duration)) {
                        phase = 1.25
                    }
                    
                    withAnimation(.easeOut(duration: 0.2).delay(duration)) {
                        opacity = 0
                    }
                }
        }
    }
}

/// 整个表面扫过的白光（从左上到右下）
private struct SurfaceScanEffect: View {
    let width: CGFloat
    let height: CGFloat
    @State private var offset: CGFloat = -1.5
    @State private var opacity: Double = 0
    
    var body: some View {
        ZStack {
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [
                            .clear, 
                            .white.opacity(0.1), 
                            .white.opacity(0.5), 
                            .white.opacity(0.8), 
                            .white.opacity(0.5), 
                            .white.opacity(0.1), 
                            .clear
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(width: width * 0.8, height: height * 4)
                // 45 degrees for Top-Left to Bottom-Right feel
                .rotationEffect(.degrees(45))
                .offset(x: width * offset, y: width * offset)
                .opacity(opacity)
        }
        .frame(width: width, height: height)
        .clipped()
        .onAppear {
            // Trigger after green lap (0.8s)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                opacity = 1.0
                withAnimation(.easeInOut(duration: 0.7)) {
                    offset = 1.5
                }
                withAnimation(.easeIn(duration: 0.2).delay(0.6)) {
                    opacity = 0
                }
            }
        }
    }
}


