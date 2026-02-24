import SwiftUI

struct GameBoardView: View {
    @StateObject private var viewModel = GameViewModel()
    @ObservedObject private var levelManager = LevelProgressManager.shared
    @State private var activeDragPieceId: Int?
    @State private var lastTapTime: Date?
    @State private var lastTapLocation: CGPoint?
    @State private var showWinOverlay = false
    @State private var scoreRevealProgress: Double = 0
    @State private var starRevealed: [Bool] = [false, false, false]
    @State private var winPopupScale: CGFloat = 0.92
    @State private var isScoreAnimating: Bool = false
    @State private var showAdRewardPopup = false
    @State private var isThumbnailFocused = false
    @State private var previewTimerSeconds: Int = 0
    @State private var previewTimer: Timer? = nil
    @State private var showPreviewConfirm = false
    @State private var showNotEnoughCoins = false
    @State private var adCooldownRemaining: Int = 0
    @State private var showAdCooldownAlert = false
    @State private var adRefreshTimer: Timer? = nil
    @State private var buffFxPulse = false

    let levelConfig: LevelConfig
    var mainLevelIndexForAscended: Int? = nil
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject private var tabBarVisibility: TabBarVisibility
    
    private var showAscendedHintInWin: Bool {
        guard mainLevelIndexForAscended == nil else { return false }
        let levels = LevelRepository.shared.getClassicLevels()
        guard let idx = levels.firstIndex(where: { $0.levelId == levelConfig.levelId }) else { return false }
        return LevelRepository.shared.getAscendedLevel(mainIndex: idx) != nil
    }
    
    private var currentBestStars: Int {
        if let mainId = mainLevelIndexForAscended {
            return LevelProgressManager.shared.getAscendedLevelStars(mainLevelId: mainId)
        }
        return LevelProgressManager.shared.getStars(for: levelConfig.levelId)
    }
    
    private func formatElapsed(_ seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return "\(m):\(s < 10 ? "0" : "")\(s)"
    }
    
    private var barBg: some View {
        Capsule().fill(Color.black.opacity(0.35))
    }
    
    private var actionBtnBg: some View {
        RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.35))
    }
    
    var body: some View {
        GeometryReader { geometry in
            let boardVisualOffsetY: CGFloat = geometry.size.height <= 700 ? 8 : 20
            ZStack(alignment: .top) {
                // 1. Background
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
                
                // 2. Pieces
                ForEach(viewModel.pieces) { piece in
                    PuzzlePieceView(piece: piece, isWin: viewModel.isLevelCompleted)
                        .id("\(piece.id)-\(piece.groupId)")
                        .offset(y: boardVisualOffsetY) 
                }
                .zIndex(10)
                
                // Validation Layer
                if viewModel.isLevelCompleted {
                    ZStack {
                        SurfaceScanEffect(width: viewModel.boardWidth, height: viewModel.boardHeight)
                        GreenScanEffect(width: viewModel.boardWidth, height: viewModel.boardHeight)
                    }
                    .position(
                        x: viewModel.boardOffsetX + viewModel.boardWidth / 2,
                        y: viewModel.boardOffsetY + viewModel.boardHeight / 2 + boardVisualOffsetY
                    )
                    .allowsHitTesting(false)
                    .zIndex(15)
                }
                
                ParticleOverlayView(particleSystem: viewModel.particleSystem)
                    .zIndex(550)
                
                if let hintId = viewModel.hintPieceId,
                   let piece = viewModel.pieces.first(where: { $0.id == hintId }),
                   let target = viewModel.hintTarget {
                    HintOverlayView(
                        piece: piece,
                        target: target,
                        progress: viewModel.hintAnimProgress,
                        visualOffsetY: boardVisualOffsetY
                    )
                        .frame(width: geometry.size.width, height: geometry.size.height, alignment: .topLeading)
                        .allowsHitTesting(false)
                        .zIndex(600)
                }
                
                // Interaction Layer
                if !showWinOverlay {
                    Color.clear
                        .frame(width: viewModel.boardWidth, height: viewModel.boardHeight)
                        .contentShape(Rectangle())
                        .position(
                            x: viewModel.boardOffsetX + viewModel.boardWidth / 2,
                            y: viewModel.boardOffsetY + viewModel.boardHeight / 2 + boardVisualOffsetY
                        )
                        .gesture(
                            TapGesture(count: 2)
                                .onEnded { _ in
                                    if let loc = lastTapLocation {
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
                                        .onEnded { _ in
                                            if let pieceId = activeDragPieceId {
                                                viewModel.onDragEnded(pieceId: pieceId)
                                                activeDragPieceId = nil
                                            }
                                        }
                                )
                        )
                        .zIndex(2500)
                }
                
                // UI Overlay
                VStack(spacing: 8) { 
                    headerRow(geometry: geometry)
                    statsRow
                    controlArea
                }
                .padding(.top, 0)
                .zIndex(2000)
                .foregroundColor(.white)
                
                if viewModel.pieces.isEmpty {
                    Text(TRANS.get("loading", "Loading..."))
                        .foregroundColor(.white)
                        .zIndex(500)
                }
                
                if viewModel.isTutorialMode && viewModel.tutorialStep < 2 && !viewModel.isLevelCompleted {
                    TutorialOverlayView(step: viewModel.tutorialStep)
                        .padding(.top, 140)
                        .id("tutorial-step-\(viewModel.tutorialStep)")
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .move(edge: .top)),
                            removal: .opacity.combined(with: .scale(scale: 0.8))
                        ))
                        .zIndex(3000)
                }

                if showAdRewardPopup {
                    CoinRewardPopupView()
                        .zIndex(6800)
                        .transition(.opacity)
                }

                if showWinOverlay {
                    winOverlayView
                        .zIndex(7000)
                }
            }
            .alert(TRANS.get("confirm_preview_title", "Unlock Preview?"), isPresented: $showPreviewConfirm) {
                Button(TRANS.get("cancel", "Cancel"), role: .cancel) { }
                Button(TRANS.get("confirm", "Confirm")) {
                    startPaidPreview()
                }
            } message: {
                Text(TRANS.get("confirm_preview_msg", "Spend 200 Coins for 20s clear preview?"))
            }
            .alert(TRANS.get("hint_not_enough", "Not enough coins!"), isPresented: $showNotEnoughCoins) {
                Button(TRANS.get("close", "Close"), role: .cancel) { }
            }
            .alert(TRANS.get("confirm_preview_title", "Unlock Preview?"), isPresented: $showAdCooldownAlert) {
                Button(TRANS.get("close", "Close"), role: .cancel) { }
            } message: {
                Text(TRANS.get("ad_cooldown_msg", "Ad cooling down, please wait {s}s").replacingOccurrences(of: "{s}", with: "\(adCooldownRemaining)"))
            }
            .onAppear {
                tabBarVisibility.hideTabBar = true
                levelManager.updateBuffState()
                withAnimation(.easeInOut(duration: 0.85).repeatForever(autoreverses: true)) {
                    buffFxPulse = true
                }
                if viewModel.pieces.isEmpty {
                    viewModel.loadLevel(levelConfig, viewSize: geometry.size, mainLevelIndexForAscended: mainLevelIndexForAscended)
                }
                viewModel.startElapsedTimer()
                startAdRefreshTimer()
            }
            .onChangeCompat(of: viewModel.isLevelCompleted) { completed in
                if completed {
                    handleLevelCompletion()
                } else {
                    showWinOverlay = false
                    showAdRewardPopup = false
                }
            }
            .onChangeCompat(of: showWinOverlay) { showing in
                if showing {
                    triggerWinAnimations()
                } else {
                    resetWinAnimations()
                }
            }
            .onChangeCompat(of: geometry.size) { newSize in
                viewModel.updateLayout(viewSize: newSize)
            }
            .onDisappear {
                tabBarVisibility.hideTabBar = false
                viewModel.clearHint()
                viewModel.stopElapsedTimer()
                previewTimer?.invalidate()
                previewTimer = nil
                stopAdRefreshTimer()
            }
        }
        .background(
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                Color.black.frame(minHeight: 120).ignoresSafeArea(edges: .bottom)
            }
            .allowsHitTesting(false)
        )
        .navigationBarHidden(true)
        .hideTabBarCompat()
        .ignoresSafeArea(.container, edges: .bottom)
        
        // Final bottom banner ad
        .overlay(
            VStack {
                Spacer()
                BannerAdView(adUnitID: AdConfig.bannerGameId)
                    .frame(height: 50)
                    .background(Color.black.opacity(0.1))
            }
            .edgesIgnoringSafeArea(.bottom)
            .zIndex(6000),
            alignment: .bottom
        )
    }

    // MARK: - Subviews & Logic

    @ViewBuilder
    private func headerRow(geometry: GeometryProxy) -> some View {
        HStack {
            Button(action: { presentationMode.wrappedValue.dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color.black.opacity(0.4)))
            }
            Spacer()
            Button(action: {
                SoundManager.shared.playClick()
                viewModel.loadLevel(levelConfig, viewSize: geometry.size, mainLevelIndexForAscended: mainLevelIndexForAscended)
            }) {
                Image(systemName: "arrow.clockwise")
                    .font(.system(size: 16, weight: .bold))
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color.black.opacity(0.4)))
            }
            .foregroundColor(.white)
        }
        .padding(.horizontal, 22)
        .padding(.top, 10)
    }

    @ViewBuilder
    private var statsRow: some View {
        let isDoubleActive = levelManager.isBuffActive
        HStack {
            HStack(spacing: 8) {
                HStack(spacing: 4) {
                    CoinIconView(size: 16)
                        .rotation3DEffect(.degrees(Double(viewModel.scoreEventCount) * 180), axis: (x: 0, y: 1, z: 0))
                    Text("\(levelManager.coins)")
                        .font(.system(size: 16, weight: .black)).monospacedDigit()
                        .foregroundStyle(.white)
                }
                if isDoubleActive {
                    Text("x2")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .foregroundStyle(Color(hex: 0xFFE082))
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(
                            Capsule()
                                .fill(Color(hex: 0xFF8F00).opacity(0.26))
                                .overlay(Capsule().stroke(Color(hex: 0xFFD54F), lineWidth: 1))
                        )
                        .scaleEffect(buffFxPulse ? 1.06 : 0.96)
                        .shadow(color: Color(hex: 0xFFC107).opacity(buffFxPulse ? 0.75 : 0.25), radius: buffFxPulse ? 8 : 2)
                }
                if viewModel.score > 0 {
                    Text("(+\(viewModel.score))")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(isDoubleActive ? Color(hex: 0xFFD54F) : .green)
                }
            }
            .padding(.horizontal, 14).frame(height: 38)
            .background(statsBackground(isDoubleActive: isDoubleActive))
            
            Spacer()
            HStack(spacing: 4) {
                Image(systemName: "clock.fill").font(.system(size: 12))
                Text(formatElapsed(viewModel.elapsedSeconds)).font(.system(size: 14, weight: .bold)).monospacedDigit()
            }
            .padding(.horizontal, 10).frame(height: 32).background(barBg)
            
            Spacer()
            HStack(spacing: 2) {
                ForEach(1...3, id: \.self) { n in
                    Image(systemName: n <= currentBestStars ? "star.fill" : "star")
                        .font(.system(size: 12))
                        .foregroundStyle(n <= currentBestStars ? .yellow : .white.opacity(0.3))
                }
            }
            .padding(.horizontal, 8).frame(height: 32).background(barBg)
        }
        .padding(.horizontal, 22)
    }

    @ViewBuilder
    private func statsBackground(isDoubleActive: Bool) -> some View {
        ZStack {
            if isDoubleActive {
                RoundedRectangle(cornerRadius: 19)
                    .fill(Color(hex: 0xFF6F00).opacity(0.18))
            } else {
                RoundedRectangle(cornerRadius: 19)
                    .fill(.ultraThinMaterial)
            }
            RoundedRectangle(cornerRadius: 19)
                .stroke(
                    isDoubleActive
                    ? LinearGradient(
                        colors: [Color(hex: 0xFFE082), Color(hex: 0xFFB300), Color(hex: 0xFFE082)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    : LinearGradient(
                        colors: [isScoreAnimating ? Color.yellow : Color.white.opacity(0.2)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    lineWidth: 1.5
                )
                .shadow(color: isDoubleActive ? Color(hex: 0xFFB300).opacity(0.35) : .clear, radius: isDoubleActive ? 8 : 0)
        }
    }

    @ViewBuilder
    private var controlArea: some View {
        HStack {
            Button(action: { viewModel.showHint() }) {
                VStack(spacing: 1) {
                    Image(systemName: "lightbulb.fill").font(.system(size: 16))
                    Text("\(GameViewModel.HINT_COST)").font(.system(size: 9, weight: .bold))
                }
                .foregroundColor(viewModel.canAffordHint ? .yellow : .white.opacity(0.4))
                .frame(width: 48, height: 48).background(actionBtnBg)
            }
            .disabled(!viewModel.canAffordHint)
            
            Spacer()
            if !viewModel.pieces.isEmpty {
                levelPreviewView
            }
            Spacer()
            
            Button(action: { 
                if LevelProgressManager.shared.canWatchAd() {
                    viewModel.requestCoinsFromAd() 
                } else {
                    adCooldownRemaining = LevelProgressManager.shared.getAdCooldownRemaining()
                    showAdCooldownAlert = true
                }
            }) {
                VStack(spacing: 1) {
                    if adCooldownRemaining > 0 {
                        Text("\(adCooldownRemaining)s")
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(.white.opacity(0.6))
                    } else {
                        Image(systemName: "video.fill").font(.system(size: 16))
                        Text("+\(GameViewModel.AD_REWARD_COINS)").font(.system(size: 9, weight: .bold))
                    }
                }
                .frame(width: 48, height: 48).background(actionBtnBg)
            }
        }
        .padding(.horizontal, 20)
    }

    @ViewBuilder
    private var levelPreviewView: some View {
        switch levelConfig.imageSource {
        case .asset, .resource:
            ZStack(alignment: .bottom) {
                ImageUtils.loadImage(source: levelConfig.imageSource)
                    .resizable().aspectRatio(contentMode: .fit)
                    .frame(width: 60, height: 60)
                    .blur(radius: isThumbnailFocused ? 0 : 8)
                    .animation(.easeInOut(duration: 0.3), value: isThumbnailFocused)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.4), lineWidth: 1))
                
                if isThumbnailFocused && previewTimerSeconds > 0 {
                    Text("\(previewTimerSeconds)s")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .background(Capsule().fill(Color.black.opacity(0.6)))
                        .offset(y: 4)
                }
            }
            .onTapGesture {
                SoundManager.shared.playClick()
                if !isThumbnailFocused {
                    showPreviewConfirm = true
                }
            }
        default: EmptyView()
        }
    }
    
    private func startPaidPreview() {
        let cost = 200
        if LevelProgressManager.shared.consumeCoins(cost) {
            // 清理旧计时器
            previewTimer?.invalidate()
            previewTimer = nil
            
            withAnimation {
                isThumbnailFocused = true
                previewTimerSeconds = 20
            }
            
            previewTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
                if previewTimerSeconds > 0 {
                    previewTimerSeconds -= 1
                } else {
                    stopPaidPreview()
                }
            }
        } else {
            showNotEnoughCoins = true
        }
    }
    
    private func stopPaidPreview() {
        previewTimer?.invalidate()
        previewTimer = nil
        withAnimation {
            isThumbnailFocused = false
            previewTimerSeconds = 0
        }
    }
    
    private func startAdRefreshTimer() {
        adRefreshTimer?.invalidate()
        adRefreshTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            let rem = LevelProgressManager.shared.getAdCooldownRemaining()
            if rem != adCooldownRemaining {
                adCooldownRemaining = rem
            }
            levelManager.updateBuffState()
        }
    }
    
    private func stopAdRefreshTimer() {
        adRefreshTimer?.invalidate()
        adRefreshTimer = nil
    }

    @ViewBuilder
    private var winOverlayView: some View {
        VStack {
            Spacer()
            VStack(spacing: 12) {
                HStack {
                    Image(systemName: "trophy.fill").font(.system(size: 28)).foregroundColor(.yellow)
                    Text(TRANS.get("you_win", "You Win!")).font(.system(size: 32, weight: .heavy))
                }
                if showAscendedHintInWin {
                    Text(TRANS.get("asc_unlocked_hint", "New Level Unlocked"))
                        .font(.subheadline.bold()).padding(.horizontal, 16).padding(.vertical, 6)
                        .background(Color.purple.opacity(0.4)).clipShape(Capsule())
                }
                HStack(spacing: 8) {
                    ForEach(1...3, id: \.self) { n in
                        Image(systemName: n <= viewModel.completionStars ? "star.fill" : "star")
                            .font(.system(size: 28)).foregroundColor(starRevealed[n-1] ? .yellow : .white.opacity(0.3))
                    }
                }
                HStack(spacing: 6) {
                    CoinIconView(size: 24)
                    Text("\(Int(round(Double(viewModel.score) * scoreRevealProgress)))")
                        .font(.title2.bold())
                }
                Button(action: { presentationMode.wrappedValue.dismiss() }) {
                    Text(TRANS.get("continue", "Continue")).font(.title3.bold())
                        .frame(maxWidth: .infinity).padding(.vertical, 14)
                        .background(Color.blue).foregroundColor(.white).clipShape(Capsule())
                }
            }
            .padding(24).background(.ultraThinMaterial).clipShape(RoundedRectangle(cornerRadius: 28))
            .padding(.horizontal, 32).padding(.bottom, 200).scaleEffect(winPopupScale)
        }
    }

    private func handleLevelCompletion() {
        // 教学关卡和前2关（包括升级B关卡）不显示插屏广告
        let skipAdLevelIds: Set<String> = ["tutorial_0", "tutorial_0_B", "g_1_A", "g_1_B", "g_2_A", "g_2_B"]
        let shouldSkipAd = skipAdLevelIds.contains(levelConfig.levelId) || levelConfig.levelId.hasPrefix("tutorial")
        
        if shouldSkipAd {
            // 直接显示胜利界面
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                showWinOverlay = true
            }
            return
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            if AdManager.shared.isRewardedInterstitialReady {
                AdManager.shared.showRewardedInterstitial(
                    onAdDismissed: {
                        showAdRewardPopup = true
                        SoundManager.shared.playWin()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            withAnimation { showAdRewardPopup = false }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { showWinOverlay = true }
                        }
                    },
                    onReward: { amount in
                        viewModel.levelManager.addCoins(amount)
                    }
                )
            } else {
                AdManager.shared.loadRewardedInterstitial()
                showWinOverlay = true
            }
        }
    }

    private func triggerWinAnimations() {
        scoreRevealProgress = 0
        starRevealed = [false, false, false]
        winPopupScale = 0.92
        withAnimation(.spring(response: 0.45, dampingFraction: 0.75)) { winPopupScale = 1.0 }
        withAnimation(.easeOut(duration: 0.6)) { scoreRevealProgress = 1.0 }
        for i in 0..<min(3, viewModel.completionStars) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25 * Double(i)) {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) { starRevealed[i] = true }
            }
        }
    }

    private func resetWinAnimations() {
        scoreRevealProgress = 0
        starRevealed = [false, false, false]
        winPopupScale = 0.92
    }
}

// MARK: - Dedicated Subview Structs

struct GreenScanEffect: View {
    let width: CGFloat
    let height: CGFloat
    @State private var phase: CGFloat = 0
    @State private var opacity: Double = 1.0
    
    var body: some View {
        let perimeter = 2 * (width + height)
        let duration = max(0.6, Double(perimeter) / 1500.0)
        
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
                .frame(width: width, height: height)
            
            RoundedRectangle(cornerRadius: 12)
                .trim(from: max(0, phase - 0.25), to: phase)
                .stroke(
                    LinearGradient(colors: [Color(hex: 0x69F0AE).opacity(0), Color(hex: 0x69F0AE).opacity(0.6), Color(hex: 0xB9F6CA)], startPoint: .leading, endPoint: .trailing),
                    style: StrokeStyle(lineWidth: 6, lineCap: .round)
                )
                .frame(width: width, height: height)
                .opacity(opacity)
                .onAppear {
                    withAnimation(.linear(duration: duration)) { phase = 1.25 }
                    withAnimation(.easeOut(duration: 0.2).delay(duration)) { opacity = 0 }
                }
        }
    }
}

struct SurfaceScanEffect: View {
    let width: CGFloat
    let height: CGFloat
    @State private var offset: CGFloat = -1.5
    @State private var opacity: Double = 0
    
    var body: some View {
        ZStack {
            Rectangle()
                .fill(LinearGradient(colors: [.clear, .white.opacity(0.1), .white.opacity(0.5), .white.opacity(0.8), .white.opacity(0.5), .white.opacity(0.1), .clear], startPoint: .leading, endPoint: .trailing))
                .frame(width: width * 0.8, height: height * 4)
                .rotationEffect(.degrees(45))
                .offset(x: width * offset, y: width * offset)
                .opacity(opacity)
        }
        .frame(width: width, height: height).clipped()
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                opacity = 1.0
                withAnimation(.easeInOut(duration: 0.7)) { offset = 1.5 }
                withAnimation(.easeIn(duration: 0.2).delay(0.6)) { opacity = 0 }
            }
        }
    }
}

struct CoinRewardPopupView: View {
    @State private var showContent = false
    @State private var ringScale: CGFloat = 0.3
    @State private var ringOpacity: Double = 0
    @State private var coinScale: CGFloat = 0.1
    @State private var coinRotation: Double = -30
    @State private var textOffset: CGFloat = 30
    @State private var textOpacity: Double = 0
    @State private var glowPulse: Bool = false
    @State private var sparklePhase: Double = 0
    
    var body: some View {
        ZStack {
            // 背景模糊遮罩
            Color.black.opacity(0.7).ignoresSafeArea()
            
            // 柔和光芒扩散
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color(hex: 0xFFD700).opacity(0.3), Color(hex: 0xFFA000).opacity(0.1), .clear],
                        center: .center,
                        startRadius: 20,
                        endRadius: 200
                    )
                )
                .frame(width: 400, height: 400)
                .scaleEffect(ringScale)
                .opacity(ringOpacity)
            
            // 外圈旋转光点
            ForEach(0..<6) { i in
                Circle()
                    .fill(Color(hex: 0xFFD700).opacity(0.6))
                    .frame(width: 6, height: 6)
                    .offset(y: -130)
                    .rotationEffect(.degrees(Double(i) * 60 + sparklePhase))
                    .blur(radius: 2)
            }
            .opacity(ringOpacity)
            
            VStack(spacing: 20) {
                // 标题
                Text(TRANS.get("reward_from_ad", "Watch Reward"))
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(.white.opacity(0.7))
                    .tracking(2)
                    .textCase(.uppercase)
                    .opacity(textOpacity)
                    .offset(y: textOffset)
                
                // 金币主体
                ZStack {
                    // 外发光环
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [Color(hex: 0xFFD700).opacity(0.6), Color(hex: 0xFFA000).opacity(0.2)],
                                startPoint: .top,
                                endPoint: .bottom
                            ),
                            lineWidth: 3
                        )
                        .frame(width: 130, height: 130)
                        .shadow(color: Color(hex: 0xFFD700).opacity(glowPulse ? 0.6 : 0.2), radius: glowPulse ? 20 : 8)
                    
                    // 金币本体 - 多层渐变
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color(hex: 0xFFF176),
                                    Color(hex: 0xFFD700),
                                    Color(hex: 0xFFC107),
                                    Color(hex: 0xFFA000)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 110, height: 110)
                        .overlay(
                            // 内圈凹槽
                            Circle()
                                .stroke(Color(hex: 0xE6A000).opacity(0.5), lineWidth: 2)
                                .frame(width: 90, height: 90)
                        )
                        .overlay(
                            // 高光弧线
                            Circle()
                                .trim(from: 0, to: 0.3)
                                .stroke(Color.white.opacity(0.4), lineWidth: 3)
                                .frame(width: 100, height: 100)
                                .rotationEffect(.degrees(-60))
                        )
                        .shadow(color: Color(hex: 0xFFA000).opacity(0.4), radius: 16, y: 8)
                    
                    // 硬币上的星形图案（替代丑陋的 ¥）
                    Image(systemName: "star.fill")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color(hex: 0xFFF9C4), Color(hex: 0xFFE082)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .shadow(color: Color(hex: 0xE6A000).opacity(0.6), radius: 2, y: 1)
                }
                .scaleEffect(coinScale)
                .rotation3DEffect(.degrees(coinRotation), axis: (x: 0, y: 1, z: 0))
                
                // 金额数字
                HStack(spacing: 4) {
                    Text("+100")
                        .font(.system(size: 52, weight: .heavy, design: .rounded))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color(hex: 0xFFF9C4), .white, Color(hex: 0xFFD700)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    
                    CoinIconView(size: 28)
                }
                .shadow(color: Color(hex: 0xFFA000).opacity(0.5), radius: 8, y: 4)
                .opacity(textOpacity)
                .offset(y: textOffset)
            }
        }
        .onAppear {
            // 1. 光环扩散
            withAnimation(.easeOut(duration: 0.5)) {
                ringScale = 1.2
                ringOpacity = 1.0
            }
            
            // 2. 金币弹入 + 旋转
            withAnimation(.spring(response: 0.6, dampingFraction: 0.65).delay(0.15)) {
                coinScale = 1.0
                coinRotation = 0
            }
            
            // 3. 文字上浮
            withAnimation(.easeOut(duration: 0.4).delay(0.35)) {
                textOpacity = 1.0
                textOffset = 0
            }
            
            // 4. 脉冲光晕
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true).delay(0.6)) {
                glowPulse = true
            }
            
            // 5. 光点旋转
            withAnimation(.linear(duration: 6).repeatForever(autoreverses: false)) {
                sparklePhase = 360
            }
        }
    }
}
