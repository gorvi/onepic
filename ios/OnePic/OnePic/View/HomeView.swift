import SwiftUI
import Combine

// Local extension to resolve hex ambiguity
extension Color {
    static let coinGoldLight = Color(red: 1, green: 0.85, blue: 0.35)
    static let coinGoldMid = Color(red: 0.95, green: 0.75, blue: 0.2)
    static let coinGoldDark = Color(red: 0.8, green: 0.55, blue: 0.05)
}

/// 金币图标：圆形金色渐变 + 内圈高光，替代 emoji
struct CoinIconView: View {
    var size: CGFloat = 24
    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [.coinGoldLight, .coinGoldMid, .coinGoldDark],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: size, height: size)
            Circle()
                .stroke(
                    LinearGradient(
                        colors: [.coinGoldLight.opacity(0.9), .coinGoldDark.opacity(0.6)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: max(1, size * 0.08)
                )
                .frame(width: size, height: size)
            Circle()
                .fill(
                    RadialGradient(
                        colors: [.white.opacity(0.8), .clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: size * 0.45
                    )
                )
                .frame(width: size * 0.6, height: size * 0.6)
                .offset(x: -size * 0.2, y: -size * 0.2)
                .blur(radius: 1)
        }
        .shadow(color: .coinGoldDark.opacity(0.6), radius: size * 0.2, x: 0, y: 1.5)
        .shadow(color: Color(hex: 0xFFF176).opacity(0.4), radius: size * 0.5) // 柠檬黄溢出光感
    }
}

/// 首页金币区域 frame，用于飞入动画目标位置
private struct CoinAreaFrameKey: PreferenceKey {
    static var defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) { value = nextValue() }
}

/// 首页星星区域 frame，用于飞入动画目标位置（与金币一致效果）
private struct StarAreaFrameKey: PreferenceKey {
    static var defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) { value = nextValue() }
}

extension Color {
    init(localHex: UInt) {
        let r = Double((localHex & 0xFF0000) >> 16) / 255.0
        let g = Double((localHex & 0x00FF00) >> 8) / 255.0
        let b = Double(localHex & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

struct HomeView: View {
    var levels: [LevelConfig] {
        LevelRepository.shared.getClassicLevels()
    }
    @ObservedObject var levelManager = LevelProgressManager.shared
    @State private var selectedLevel: LevelConfig?
    @State private var mainLevelIndexForAscended: Int? = nil
    @State private var navigateToGame = false
    @State private var progressVersion = 0
    @State private var animatingUnlockLevelId: String? = nil
    @State private var animatingAscendedUnlockMainIndex: Int? = nil
    @State private var pendingUnlockLevelIdForDisplay: String? = nil
    @State private var targetScrollId: String? = nil
    @State private var completedLevelsBeforeGame: Set<String> = []
    @State private var lastPlayedLevelIndex: Int? = nil
    @State private var highlightedLevelId: String? = nil
    @State private var hasInitialScrolled = false
    @ObservedObject var visitorManager: CelestialVisitorManager
    @Binding var targetLevelId: String?
    
    /// 进入拼图前记录的硬币/星星，用于返回时播放「新增」动画
    @State private var totalCoinsBeforeGame: Int? = nil
    @State private var totalStarsBeforeGame: Int? = nil
    /// 返回时得分递增动画：从 returnCoinsFrom 到 returnCoinsTo，进度 0~1
    @State private var returnCoinsFrom: Int? = nil
    @State private var returnCoinsTo: Int? = nil
    @State private var returnCoinsProgress: Double = 0
    /// 返回时星星飞入：新增几颗、飞行进度 0~1
    @State private var flyingStarCount: Int = 0
    @State private var starFlyProgress: Double = 0
    /// 返回时金币飞入：显示几枚图标（最多 5 枚）、飞行进度 0~1
    @State private var flyingCoinCount: Int = 0
    @State private var coinFlyProgress: Double = 0
    /// 返回时星星数字递增（与飞星同步）
    @State private var returnStarsFrom: Int? = nil
    @State private var returnStarsTo: Int? = nil
    @State private var returnStarsProgress: Double = 0
    /// 定时器驱动逐格递增（由 ReturnTicker 驱动，保证电表跳动可见）
    @StateObject private var returnTicker = ReturnTicker()
    /// 加完后在总数旁显示「+N」，持续一段时间
    @State private var showCoinsDeltaLabel: Int? = nil
    @State private var showStarsDeltaLabel: Int? = nil
    /// 首页金币区域 frame（用于飞入目标）
    @State private var coinAreaFrame: CGRect = .zero
    /// 收集完成后金币区抖动偏移
    @State private var coinsShakeOffset: CGFloat = 0
    /// 最后一帧金币淡出，让最后一帧消失更快
    @State private var lastFrameCoinFadeOut: Bool = false
    /// 是否已进入金币最后一帧（用于可靠触发 onChange）
    @State private var didEnterLastFrame: Bool = false
    /// 星星区域 frame（与金币一致：飞向实际位置）
    @State private var starAreaFrame: CGRect = .zero
    @State private var starsShakeOffset: CGFloat = 0
    @State private var lastFrameStarFadeOut: Bool = false
    @State private var didEnterStarLastFrame: Bool = false
    /// 用于金币/星星消失后即时显示 +N（在淡出时设置，不等到 cleanup）
    @State private var pendingCoinsDeltaForLabel: Int? = nil
    @State private var pendingStarsDeltaForLabel: Int? = nil
    
    /// 返回动画时长（放慢以便看清飞入）
    private static let returnAnimationDuration: Double = 1.5
    private static let returnAnimationCleanupDelay: Double = 2.0
    /// 电表式每格间隔（秒），略放慢减轻卡顿
    private static let tickInterval: Double = 0.14
    
    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
            ZStack {
                SharedGalaxyBackground(atmosphereTheme: "cosmos", visitorManager: visitorManager)
                ScrollViewReader { scrollProxy in
                    ScrollView {
                        let _ = progressVersion
                        LazyVStack(spacing: 0) {
                            Text(TRANS.get("chapter_coming_soon", "Coming Soon"))
                                .frame(maxWidth: .infinity)
                                .font(.system(size: 14, weight: .medium))
                                .tracking(2)
                                .foregroundColor(.white.opacity(0.5))
                                .padding(.top, 150)
                                .padding(.bottom, 60)
                                .padding(.bottom, 60)
                            ForEach(Array(levels.enumerated().reversed()), id: \.element.levelId) { index, level in
                                
                                LevelRowItem(
                                    index: index,
                                    level: level,
                                    previousLevel: index > 0 ? levels[index - 1] : nil,
                                    shouldAnimateUnlock: animatingUnlockLevelId == level.levelId,
                                    shouldAnimateAscendedUnlock: animatingAscendedUnlockMainIndex == index,
                                    pendingUnlockLevelIdForDisplay: pendingUnlockLevelIdForDisplay,
                                    highlightedLevelId: highlightedLevelId,
                                    onLevelTap: { handleLevelTap(level: level, index: index) },
                                    onAscendedTap: { handleAscendedTap(index: index) }
                                )
                                .id(level.levelId)
                                
                                // Insert Chapter Title BELOW the lowest level of each sector (entry point from bottom)
                                if index > 0 && (index - 1) % 5 == 0 {
                                    ChapterTitleView(stageIndex: (index - 1) / 5 + 1)
                                        .padding(.vertical, 30)
                                } else if index == 0 {
                                    // Tutorial Chapter Footer (at absolute bottom)
                                    ChapterTitleView(stageIndex: 0)
                                        .padding(.top, 30)
                                        .padding(.bottom, 100) // Extra padding to ensure it's comfortably visible at scroll end
                                }
                            }
                            Spacer().frame(height: 30)
                            
                            // 首页原生广告（全息风格）
                            AdMobNativeAdView(scene: .home)
                                .padding(.horizontal, 16)
                                .padding(.bottom, 40)
                            
                            Spacer().frame(height: 200)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)
                    }
                    .scrollContentBackground(.hidden)
                    .onChange(of: targetScrollId) { _, newVal in
                        if let id = newVal { withAnimation(.easeInOut(duration: 0.6)) { scrollProxy.scrollTo(id, anchor: .center) }; targetScrollId = nil }
                    }
                    .onChange(of: targetLevelId) { _, newVal in
                        if let id = newVal { processLocate(id: id, scrollProxy: scrollProxy) }
                    }
                    .onAppear {
                        if let id = targetLevelId {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { processLocate(id: id, scrollProxy: scrollProxy) }
                            hasInitialScrolled = true
                        } else if !hasInitialScrolled {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                let nextToPlay = levels.first { level in
                                    guard let idx = levels.firstIndex(where: { $0.levelId == level.levelId }) else { return false }
                                    let isUnlocked = idx == 0 || LevelProgressManager.shared.isLevelCompleted(levels[idx - 1].levelId)
                                    let isCompleted = LevelProgressManager.shared.isLevelCompleted(level.levelId)
                                    return isUnlocked && !isCompleted
                                }
                                if let target = nextToPlay {
                                    scrollProxy.scrollTo(target.levelId, anchor: .center)
                                } else if let last = levels.last {
                                    scrollProxy.scrollTo(last.levelId, anchor: .center)
                                }
                                hasInitialScrolled = true
                            }
                        }
                    }
                }
                CelestialVisitorInteractionOverlay(manager: visitorManager).ignoresSafeArea()
                VStack {
                    HStack {
                        HStack(spacing: 6) {
                            CoinIconView(size: 22)
                            TickingNumberView(value: displayCoins(geometry: geometry))
                                .font(.system(size: 22, weight: .black))
                                .foregroundColor(.white)
                                .monospacedDigit()
                                .shadow(color: .black.opacity(0.5), radius: 2)
                            if let delta = showCoinsDeltaLabel, delta > 0 {
                                Text("+\(delta)")
                                    .font(.system(size: 22, weight: .black))
                                    .foregroundColor(Color(localHex: 0x7CFC00))
                                    .shadow(color: Color(localHex: 0x7CFC00).opacity(0.5), radius: 4)
                                    .transition(.asymmetric(
                                        insertion: .scale(scale: 0.3).combined(with: .opacity),
                                        removal: .opacity.animation(.easeOut(duration: 0.25))
                                    ))
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            ZStack {
                                Capsule().fill(Color.black.opacity(0.35))
                                Capsule().stroke(
                                    LinearGradient(colors: [Color(hex: 0xFFF176).opacity(0.4), Color.clear], startPoint: .top, endPoint: .bottom),
                                    lineWidth: 1
                                )
                            }
                        )
                        .background(GeometryReader { g in Color.clear.preference(key: CoinAreaFrameKey.self, value: g.frame(in: .named("homeOverlay"))) })
                        .offset(x: coinsShakeOffset)
                        Spacer()
                        HStack(spacing: 6) {
                            Image(systemName: "star.fill").foregroundColor(Color(localHex: 0xFFD700))
                                .font(.system(size: 18))
                                .shadow(color: Color(localHex: 0xFFD700).opacity(0.6), radius: 4)
                            TickingNumberView(value: displayStars(geometry: geometry))
                                .font(.system(size: 22, weight: .black))
                                .foregroundColor(.white)
                                .monospacedDigit()
                                .shadow(color: .black.opacity(0.5), radius: 2)
                            if let delta = showStarsDeltaLabel, delta > 0 {
                                Text("+\(delta)")
                                    .font(.system(size: 22, weight: .black))
                                    .foregroundColor(Color(localHex: 0xFFD700))
                                    .shadow(color: Color(localHex: 0xFFD700).opacity(0.4), radius: 4)
                                    .transition(.asymmetric(
                                        insertion: .scale(scale: 0.3).combined(with: .opacity),
                                        removal: .opacity.animation(.easeOut(duration: 0.25))
                                    ))
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            ZStack {
                                Capsule().fill(Color.black.opacity(0.35))
                                Capsule().stroke(
                                    LinearGradient(colors: [Color(hex: 0xFFD700).opacity(0.4), Color.clear], startPoint: .top, endPoint: .bottom),
                                    lineWidth: 1
                                )
                            }
                        )
                        .background(GeometryReader { g in Color.clear.preference(key: StarAreaFrameKey.self, value: g.frame(in: .named("homeOverlay"))) })
                        .offset(x: starsShakeOffset)
                    }.padding().padding(.top, 40)
                    Spacer().allowsHitTesting(false)
                }
                // 返回时新增金币从屏幕中部「飞」到左上角
                if flyingCoinCount > 0 {
                    flyingCoinsOverlay(geometry: geometry)
                }
                // 返回时新增星星从屏幕中部「飞」到右上角星星处
                if flyingStarCount > 0 {
                    flyingStarsOverlay(geometry: geometry)
                }
            }
            .coordinateSpace(name: "homeOverlay")
            .onPreferenceChange(CoinAreaFrameKey.self) { coinAreaFrame = $0 }
            .onPreferenceChange(StarAreaFrameKey.self) { starAreaFrame = $0 }
            .background(Color.black)
            }
            .toolbarBackground(.hidden, for: .navigationBar)
            #if os(iOS)
            .toolbar(.hidden, for: .navigationBar)
            #endif
            .onReceive(NotificationCenter.default.publisher(for: .levelProgressDidChange)) { _ in progressVersion += 1 }
            .onChange(of: navigateToGame) { _, isNavigatingToGame in if !isNavigatingToGame { handleTransitionBackFromGame() } }
            .navigationDestination(isPresented: $navigateToGame) {
                if let level = selectedLevel { GameBoardView(levelConfig: level, mainLevelIndexForAscended: mainLevelIndexForAscended) }
            }
        }
        .makeTransparentBackground()
    }
    
    /// 顶部栏显示的硬币数（返回时用定时器逐格递增，触发电表跳动）
    private func displayCoins(geometry: GeometryProxy) -> Int {
        if let stepped = returnTicker.steppedCoins { return stepped }
        guard let from = returnCoinsFrom, let to = returnCoinsTo else {
            return LevelProgressManager.shared.getCoins()
        }
        return from + Int(round(Double(to - from) * returnCoinsProgress))
    }
    
    /// 顶部栏显示的星星数（返回时用定时器逐格递增，触发电表跳动）
    private func displayStars(geometry: GeometryProxy) -> Int {
        if let stepped = returnTicker.steppedStars { return stepped }
        guard let from = returnStarsFrom, let to = returnStarsTo else {
            return LevelProgressManager.shared.progress.totalStars
        }
        return from + Int(round(Double(to - from) * returnStarsProgress))
    }
    
    /// 金币飞入进度（由 returnTicker 逐格时同步计算）
    private var coinFlyProgressValue: Double {
        if let c = returnTicker.steppedCoins, let f = returnCoinsFrom, let t = returnCoinsTo, t > f {
            return Double(c - f) / Double(t - f)
        }
        return coinFlyProgress
    }
    
    /// 星星飞入进度（由 returnTicker 逐格时同步计算）
    private var starFlyProgressValue: Double {
        if let s = returnTicker.steppedStars, let f = returnStarsFrom, let t = returnStarsTo, t > f {
            return Double(s - f) / Double(t - f)
        }
        return starFlyProgress
    }
    
    /// 飞行动画用轻量金币（单圆+渐变），减少绘制提升流畅度
    private func flyingCoinShape(size: CGFloat) -> some View {
        Circle()
            .fill(
                LinearGradient(
                    colors: [Color.coinGoldLight, Color.coinGoldMid],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: size, height: size)
    }
    
    /// easeOut：后半段减速，飞入更自然
    private static func easeOut(_ t: Double) -> Double {
        guard t > 0, t < 1 else { return t }
        return 1 - pow(1 - t, 1.4)
    }
    
    /// 返回时新增金币从屏幕中部飞向首页金币位置（带弧线）；最后一帧一枚金币在金币区下方
    private static let coinFlyLastFrameThreshold: Double = 0.96
    private static let coinFlyTargetBelowOffset: CGFloat = 18
    
    private func flyingCoinsOverlay(geometry: GeometryProxy) -> some View {
        let startX = geometry.size.width * 0.5
        let startY = geometry.size.height * 0.35
        let hasTarget = coinAreaFrame.width > 0 && coinAreaFrame.height > 0
        let endX = hasTarget ? coinAreaFrame.midX : 70
        let endY = hasTarget ? coinAreaFrame.midY : 55
        let lastFrameY = hasTarget ? coinAreaFrame.maxY + Self.coinFlyTargetBelowOffset : endY + Self.coinFlyTargetBelowOffset
        let arcAmplitude: CGFloat = 24
        let isLastFrame = coinFlyProgressValue >= Self.coinFlyLastFrameThreshold
        return ZStack {
            if isLastFrame {
                flyingCoinShape(size: 28)
                    .position(x: endX, y: lastFrameY)
                    .scaleEffect(1)
                    .opacity(lastFrameCoinFadeOut ? 0 : 1)
                    .animation(.easeOut(duration: 0.12), value: lastFrameCoinFadeOut)
            } else {
                ForEach(0..<flyingCoinCount, id: \.self) { i in
                    let stagger = Double(i) * 0.14
                    let rawP = min(1, max(0, coinFlyProgressValue * 1.12 - stagger))
                    let p = Self.easeOut(rawP)
                    let x = startX + (endX - startX) * p
                    let yOffset = arcAmplitude * sin(p * .pi)
                    let y = startY + (endY - startY) * p + yOffset
                    let scale = 0.45 + 0.55 * p
                    let opacity = rawP < 0.06 ? Double(rawP) / 0.06 : (rawP > 0.94 ? Double(1 - rawP) / 0.06 : 1)
                    flyingCoinShape(size: 28)
                        .position(x: x, y: y)
                        .scaleEffect(scale)
                        .opacity(opacity)
                }
            }
        }
        .onChange(of: coinFlyProgressValue) { _, progress in
            if !didEnterLastFrame && progress >= Self.coinFlyLastFrameThreshold {
                didEnterLastFrame = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
                    withAnimation(.easeOut(duration: 0.12)) { lastFrameCoinFadeOut = true }
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                        if let c = pendingCoinsDeltaForLabel, c > 0 { showCoinsDeltaLabel = c }
                    }
                    if pendingCoinsDeltaForLabel != nil { triggerCoinsShakeAndSound() }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                        withAnimation(.easeOut(duration: 0.3)) {
                            showCoinsDeltaLabel = nil
                            showStarsDeltaLabel = nil
                        }
                    }
                }
            }
        }
        .drawingGroup()
        .allowsHitTesting(false)
    }
    
    /// 触发金币区抖动并播收集音效
    private func triggerCoinsShakeAndSound() {
        SoundManager.shared.playCoinCollect()
        let steps: [CGFloat] = [6, -6, 5, -5, 3, -3, 0]
        for (i, x) in steps.enumerated() {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.04) {
                withAnimation(.easeInOut(duration: 0.04)) {
                    coinsShakeOffset = x
                }
            }
        }
    }
    
    /// 触发星星区抖动并播收集音效（与金币一致效果）
    private func triggerStarsShakeAndSound() {
        SoundManager.shared.playCoinCollect()
        let steps: [CGFloat] = [6, -6, 5, -5, 3, -3, 0]
        for (i, x) in steps.enumerated() {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.04) {
                withAnimation(.easeInOut(duration: 0.04)) {
                    starsShakeOffset = x
                }
            }
        }
    }
    
    /// 星星飞入最后一帧阈值、目标下方偏移（与金币一致）
    private static let starFlyLastFrameThreshold: Double = 0.96
    private static let starFlyTargetBelowOffset: CGFloat = 18
    
    /// 返回时新增星星从屏幕中部飞向首页星星位置（弧线、easeOut）；最后一帧一枚在星星区下方并淡出（与金币一致效果）
    private func flyingStarsOverlay(geometry: GeometryProxy) -> some View {
        let startX = geometry.size.width * 0.5
        let startY = geometry.size.height * 0.35
        let hasTarget = starAreaFrame.width > 0 && starAreaFrame.height > 0
        let endX = hasTarget ? starAreaFrame.midX : (geometry.size.width - 70)
        let endY = hasTarget ? starAreaFrame.midY : 55
        let lastFrameY = hasTarget ? starAreaFrame.maxY + Self.starFlyTargetBelowOffset : endY + Self.starFlyTargetBelowOffset
        let arcAmplitude: CGFloat = 24
        let isLastFrame = starFlyProgressValue >= Self.starFlyLastFrameThreshold
        return ZStack {
            if isLastFrame {
                Image(systemName: "star.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(LinearGradient(colors: [.yellow, .orange], startPoint: .top, endPoint: .bottom))
                    .position(x: endX, y: lastFrameY)
                    .scaleEffect(1)
                    .opacity(lastFrameStarFadeOut ? 0 : 1)
                    .animation(.easeOut(duration: 0.12), value: lastFrameStarFadeOut)
            } else {
                ForEach(0..<flyingStarCount, id: \.self) { i in
                    let stagger = Double(i) * 0.14
                    let rawP = min(1, max(0, starFlyProgressValue * 1.12 - stagger))
                    let p = Self.easeOut(rawP)
                    let x = startX + (endX - startX) * p
                    let yOffset = arcAmplitude * sin(p * .pi)
                    let y = startY + (endY - startY) * p + yOffset
                    let scale = 0.45 + 0.55 * p
                    let opacity = rawP < 0.06 ? Double(rawP) / 0.06 : (rawP > 0.94 ? Double(1 - rawP) / 0.06 : 1)
                    Image(systemName: "star.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(LinearGradient(colors: [.yellow, .orange], startPoint: .top, endPoint: .bottom))
                        .position(x: x, y: y)
                        .scaleEffect(scale)
                        .opacity(opacity)
                }
            }
        }
        .onChange(of: starFlyProgressValue) { _, progress in
            if !didEnterStarLastFrame && progress >= Self.starFlyLastFrameThreshold {
                didEnterStarLastFrame = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
                    withAnimation(.easeOut(duration: 0.12)) { lastFrameStarFadeOut = true }
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                        if let s = pendingStarsDeltaForLabel, s > 0 { showStarsDeltaLabel = s }
                    }
                    if pendingStarsDeltaForLabel != nil { triggerStarsShakeAndSound() }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                        withAnimation(.easeOut(duration: 0.3)) {
                            showCoinsDeltaLabel = nil
                            showStarsDeltaLabel = nil
                        }
                    }
                }
            }
        }
        .drawingGroup()
        .allowsHitTesting(false)
    }
    
    private func processLocate(id: String, scrollProxy: ScrollViewProxy) {
        // We need to scroll to the root level ID (the one visible in the main list)
        // Main levels end in _A, Ascended levels end in _B.
        // Even if we are locating a _B level, the ScrollView only identifies the row by its _A id
        // because the Ascended nodes are siblings within the same Row/Item or grouped.
        // Actually, in HomeView.swift, LevelRowItem has .id(level.levelId).
        // If it's an ascended tap/locate, we still want to scroll to that specific row.
        
        let rootId: String
        if id.hasSuffix("_B") {
            rootId = id.replacingOccurrences(of: "_B", with: "_A")
        } else if id == "tutorial_0_B" {
            rootId = "tutorial_0_A"
        } else {
            rootId = id
        }
        
        // Use LevelRepository.shared.getClassicLevels() result which matches HomeView's levels array
        let foundLevel = levels.first { $0.levelId == rootId }
        
        guard let target = foundLevel else { 
            print("⚠️ processLocate: Target rootId \(rootId) not found in classic levels. Searching all...")
            // Fallback: search all 120 (just in case)
            let allLevels = LevelRepository.shared.getAllGalleryLevels()
            if let backupTarget = allLevels.first(where: { $0.levelId == rootId }) {
                print("📍 processLocate (Fallback): Scrolling to \(backupTarget.levelId)")
                scrollProxy.scrollTo(backupTarget.levelId, anchor: .center)
            } else {
                print("❌ processLocate: Complete failure finding \(rootId)")
            }
            targetLevelId = nil
            return 
        }
        
        print("📍 processLocate: Scrolling to \(target.levelId) for target \(id)")
        // Use withAnimation for smooth scroll
        withAnimation(.easeInOut(duration: 0.8)) {
            scrollProxy.scrollTo(target.levelId, anchor: .center)
        }
        
        // Highlight logic (pulse effect)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.highlightedLevelId = nil
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { self.highlightedLevelId = id }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                if self.highlightedLevelId == id { withAnimation(.easeOut(duration: 0.8)) { self.highlightedLevelId = nil } }
            }
        }
        
        // Clear the request
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { 
            if self.targetLevelId == id { self.targetLevelId = nil } 
        }
    }
    
    private func handleTransitionBackFromGame() {
        progressVersion += 1
        let pm = LevelProgressManager.shared
        let currentCoins = pm.getCoins()
        let currentStars = pm.progress.totalStars
        
        // 返回时得分/星星「新增」动画：定时器逐格递增（电表跳动）+ 飞入 + 加完后显示 +N
        // 仅用存储的 current - before 作为增量，且只进入一次（立即清空 before，防止 onChange 二次触发导致重复动画）
        if let beforeCoins = totalCoinsBeforeGame, let beforeStars = totalStarsBeforeGame {
            totalCoinsBeforeGame = nil
            totalStarsBeforeGame = nil
            var deltaCoins = currentCoins - beforeCoins
            let deltaStars = currentStars - beforeStars
            // 校验：若 delta 多于本局应得（score），修正存储并限制显示（解决多计金币导致的 +N 虚高）
            if let cap = pm.lastGameCoinScore, deltaCoins > cap {
                pm.addCoins(-(deltaCoins - cap))
                deltaCoins = cap
            }
            pm.lastGameCoinScore = nil
            returnCoinsFrom = beforeCoins
            returnCoinsTo = beforeCoins + deltaCoins
            returnStarsFrom = beforeStars
            returnStarsTo = currentStars
            if deltaCoins > 0 {
                returnCoinsProgress = 0
                flyingCoinCount = min(deltaCoins, 5)
                coinFlyProgress = 0
                lastFrameCoinFadeOut = false
                didEnterLastFrame = false
                pendingCoinsDeltaForLabel = deltaCoins
            }
            if deltaStars > 0 {
                returnStarsProgress = 0
                flyingStarCount = deltaStars
                starFlyProgress = 0
                lastFrameStarFadeOut = false
                didEnterStarLastFrame = false
                pendingStarsDeltaForLabel = deltaStars
            }
            returnTicker.start(
                fromCoins: beforeCoins, toCoins: currentCoins,
                fromStars: beforeStars, toStars: currentStars,
                interval: Self.tickInterval
            )
            DispatchQueue.main.asyncAfter(deadline: .now() + Self.returnAnimationCleanupDelay) {
                if showCoinsDeltaLabel == nil, let c = pendingCoinsDeltaForLabel, c > 0 {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) { showCoinsDeltaLabel = c }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) { withAnimation(.easeOut(duration: 0.3)) { showCoinsDeltaLabel = nil } }
                }
                if showStarsDeltaLabel == nil, let s = pendingStarsDeltaForLabel, s > 0 {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) { showStarsDeltaLabel = s }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) { withAnimation(.easeOut(duration: 0.3)) { showStarsDeltaLabel = nil } }
                }
                returnTicker.stop()
                totalCoinsBeforeGame = nil
                totalStarsBeforeGame = nil
                returnCoinsFrom = nil
                returnCoinsTo = nil
                returnCoinsProgress = 0
                returnStarsFrom = nil
                returnStarsTo = nil
                returnStarsProgress = 0
                flyingStarCount = 0
                starFlyProgress = 0
                flyingCoinCount = 0
                coinFlyProgress = 0
                lastFrameCoinFadeOut = false
                didEnterLastFrame = false
                lastFrameStarFadeOut = false
                didEnterStarLastFrame = false
                pendingCoinsDeltaForLabel = nil
                pendingStarsDeltaForLabel = nil
            }
        }
        
        let currentCompleted = Set(levels.filter { pm.isLevelCompleted($0.levelId) }.map { $0.levelId })
        let isProgressMode = !currentCompleted.subtracting(completedLevelsBeforeGame).isEmpty
        let ascendedMain = pm.pendingAscendedUnlockMainIndex
        let nextId = pm.pendingUnlockLevelIdForAnimation
        
        if let mainIdx = ascendedMain {
            pendingUnlockLevelIdForDisplay = pm.pendingUnlockLevelIdForAnimation
            pm.pendingAscendedUnlockMainIndex = nil
            let mainId = levels[mainIdx].levelId
            targetScrollId = mainId
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                animatingAscendedUnlockMainIndex = mainIdx
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                    animatingAscendedUnlockMainIndex = nil
                    if let next = pm.pendingUnlockLevelIdForAnimation {
                        pm.pendingUnlockLevelIdForAnimation = nil
                        targetScrollId = next
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { pendingUnlockLevelIdForDisplay = nil; animatingUnlockLevelId = next; DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { animatingUnlockLevelId = nil } }
                    } else { pendingUnlockLevelIdForDisplay = nil }
                }
            }
        } else if let next = nextId {
            pendingUnlockLevelIdForDisplay = next
            pm.pendingUnlockLevelIdForAnimation = nil
            
            // 先滚动到当前刚打完的关卡，给用户一个视觉起跳点（可选，参考 Android）
            if isProgressMode, let playedIdx = lastPlayedLevelIndex {
                targetScrollId = levels[playedIdx].levelId
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    // 向上/下滚动到新解开的关卡
                    targetScrollId = next
                    // 关键修复：延迟加长到 0.8s。滚动本身 0.6s，加个 buffer 确保到位。
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { 
                        pendingUnlockLevelIdForDisplay = nil
                        animatingUnlockLevelId = next
                        // 特效播放大约需要 1.5s，保留足够时间
                        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { animatingUnlockLevelId = nil } 
                    }
                }
            } else {
                targetScrollId = next
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { 
                    pendingUnlockLevelIdForDisplay = nil
                    animatingUnlockLevelId = next
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { animatingUnlockLevelId = nil } 
                }
            }
        } else if !isProgressMode, let playedIdx = lastPlayedLevelIndex {
            targetScrollId = levels[playedIdx].levelId
        }
        completedLevelsBeforeGame = []
        lastPlayedLevelIndex = nil
    }
    
    private func handleLevelTap(level: LevelConfig, index: Int) {
        if index == 0 || LevelProgressManager.shared.isLevelCompleted(levels[index-1].levelId) {
            completedLevelsBeforeGame = Set(levels.filter { LevelProgressManager.shared.isLevelCompleted($0.levelId) }.map { $0.levelId })
            let pm = LevelProgressManager.shared
            totalCoinsBeforeGame = pm.getCoins()
            totalStarsBeforeGame = pm.progress.totalStars
            lastPlayedLevelIndex = index; selectedLevel = level; mainLevelIndexForAscended = nil; navigateToGame = true
        }
    }
    
    private func handleAscendedTap(index: Int) {
        if let ascended = LevelRepository.shared.getAscendedLevel(mainIndex: index), LevelProgressManager.shared.isLevelCompleted(levels[index].levelId) {
            completedLevelsBeforeGame = Set(levels.filter { LevelProgressManager.shared.isLevelCompleted($0.levelId) }.map { $0.levelId })
            let pm = LevelProgressManager.shared
            totalCoinsBeforeGame = pm.getCoins()
            totalStarsBeforeGame = pm.progress.totalStars
            lastPlayedLevelIndex = index; selectedLevel = ascended; mainLevelIndexForAscended = index; navigateToGame = true
        }
    }
}

/// 返回时逐格递增驱动：定时器每格更新 steppedCoins/steppedStars，触发电表跳动
/// 大增量时限制最大步数，避免几十个金币/星星时步数过多导致卡顿（最多 5 次刷新）
private let returnTickerMaxSteps = 5

final class ReturnTicker: ObservableObject {
    @Published var steppedCoins: Int? = nil
    @Published var steppedStars: Int? = nil
    private var timer: Timer?
    private var fromCoins = 0, toCoins = 0, fromStars = 0, toStars = 0
    private var stepCoins = 1, stepStars = 1
    
    func start(fromCoins: Int, toCoins: Int, fromStars: Int, toStars: Int, interval: Double) {
        stop()
        self.fromCoins = fromCoins
        self.toCoins = toCoins
        self.fromStars = fromStars
        self.toStars = toStars
        let deltaCoins = max(0, toCoins - fromCoins)
        let deltaStars = max(0, toStars - fromStars)
        stepCoins = deltaCoins <= returnTickerMaxSteps ? 1 : max(1, (deltaCoins + returnTickerMaxSteps - 1) / returnTickerMaxSteps)
        stepStars = deltaStars <= returnTickerMaxSteps ? 1 : max(1, (deltaStars + returnTickerMaxSteps - 1) / returnTickerMaxSteps)
        steppedCoins = fromCoins
        steppedStars = fromStars
        timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            self?.tick()
        }
        RunLoop.main.add(timer!, forMode: .common)
    }
    
    private func tick() {
        var coins = steppedCoins ?? fromCoins
        var stars = steppedStars ?? fromStars
        if coins < toCoins { coins = min(coins + stepCoins, toCoins) }
        if stars < toStars { stars = min(stars + stepStars, toStars) }
        steppedCoins = coins
        steppedStars = stars
        if coins >= toCoins && stars >= toStars {
            timer?.invalidate()
            timer = nil
        }
    }
    
    func stop() {
        timer?.invalidate()
        timer = nil
        steppedCoins = nil
        steppedStars = nil
    }
}

/// 电表式数字跳动：数值变化时轻量缩放，减轻卡顿
struct TickingNumberView: View {
    let value: Int
    @State private var tickScale: CGFloat = 1.0
    @State private var lastValue: Int = -1
    
    var body: some View {
        Text("\(value)")
            .contentTransition(.numericText())
            .scaleEffect(tickScale)
            .onChange(of: value) { _, newVal in
                guard lastValue >= 0 && newVal != lastValue else {
                    lastValue = newVal
                    return
                }
                lastValue = newVal
                tickScale = 1.2
                withAnimation(.easeOut(duration: 0.18)) {
                    tickScale = 1.0
                }
            }
            .onAppear { lastValue = value }
    }
}

struct LevelRowItem: View {
    let index: Int
    let level: LevelConfig
    let previousLevel: LevelConfig?
    let shouldAnimateUnlock: Bool
    let shouldAnimateAscendedUnlock: Bool
    let pendingUnlockLevelIdForDisplay: String?
    let highlightedLevelId: String?
    let onLevelTap: () -> Void
    let onAscendedTap: () -> Void
    
    var stageIndex: Int { index == 0 ? 0 : (index - 1) / 5 + 1 }
    var offsetX: CGFloat {
        switch index % 4 {
        case 1: return 60
        case 3: return -60
        default: return 0
        }
    }
    var previousOffsetX: CGFloat {
        if index == 0 { return 0 }
        switch (index - 1) % 4 {
        case 1: return 60
        case 3: return -60
        default: return 0
        }
    }
    
    var body: some View {
        let isCompleted = LevelProgressManager.shared.isLevelCompleted(level.levelId)
        let ascendedLevel = LevelRepository.shared.getAscendedLevel(mainIndex: index)
        let isAscendedUnlocked = isCompleted
        let isAscendedCompleted = LevelProgressManager.shared.getCompletedAscendedLevels().contains(index)
        let ascendedStars = LevelProgressManager.shared.getAscendedLevelStars(mainLevelId: index)
        
        // Match logic for _A (main) and _B (ascended)
        let mainHighlighted = highlightedLevelId == level.levelId
        let ascendedHighlighted = (ascendedLevel?.levelId != nil) && (highlightedLevelId == ascendedLevel?.levelId)
        
        VStack(spacing: 0) {
            ZStack {
                LevelNodeView(
                    index: index,
                    level: level,
                    isLocked: (index > 0 && (previousLevel.map { !LevelProgressManager.shared.isLevelCompleted($0.levelId) } ?? true)) || (level.levelId == pendingUnlockLevelIdForDisplay && !shouldAnimateUnlock),
                    isCompleted: isCompleted,
                    isNextToPlay: !isCompleted && (index == 0 || (previousLevel.map { LevelProgressManager.shared.isLevelCompleted($0.levelId) } ?? false)),
                    stageIndex: stageIndex,
                    shouldAnimateUnlock: shouldAnimateUnlock,
                    ascendedLevel: nil,
                    isAscendedUnlocked: false,
                    isAscendedCompleted: false,
                    ascendedStars: 0,
                    onAscendedClick: nil,
                    isNew: !isCompleted && (index == 0 || (previousLevel.map { LevelProgressManager.shared.isLevelCompleted($0.levelId) } ?? false)),
                    isHighlighted: mainHighlighted
                )
                .offset(x: offsetX).padding(.vertical, 10).contentShape(Rectangle()).onTapGesture { onLevelTap() }.frame(maxWidth: .infinity)
                if let ascended = ascendedLevel, isCompleted, isAscendedUnlocked {
                    Button(action: { onAscendedTap() }) { 
                        AscendedSatelliteBadgeView(
                            ascendedLevel: ascended, 
                            isUnlocked: true, 
                            isCompleted: isAscendedCompleted, 
                            stars: ascendedStars, 
                            shouldPulse: shouldAnimateAscendedUnlock,
                            isHighlighted: ascendedHighlighted
                        ) 
                    }
                    .buttonStyle(.plain).contentShape(Circle()).padding(12).offset(x: offsetX + 72, y: -48).zIndex(200)
                }
            }.frame(maxWidth: .infinity).zIndex(ascendedLevel != nil && isCompleted ? 100 : 0)
            if index > 0 {
                let midX = (previousOffsetX + offsetX) / 2
                PathConnectorView(stageIndex: stageIndex, isCompleted: LevelProgressManager.shared.isLevelCompleted(previousLevel?.levelId ?? "")).offset(x: midX).padding(.vertical, -10)
            }
        }.frame(maxWidth: .infinity)
        // .drawingGroup() removed: Avoid re-rasterizing entire row on every animation frame
    }
}

private struct AscendedSatelliteBadgeView: View {
    let ascendedLevel: LevelConfig
    let isUnlocked: Bool
    let isCompleted: Bool
    let stars: Int
    let shouldPulse: Bool
    let isHighlighted: Bool
    
    @State private var rotationSatellite1: Double = 0
    @State private var rotationSatellite2: Double = 0
    @State private var pulseScale: CGFloat = 1.0
    @State private var localizationPulse: CGFloat = 0.5
    @State private var isPinVisible = false
    /// 升级关卡完成后返回时，圆圈内「变成星星」动画进度 0→1
    @State private var starAppearProgress: CGFloat = 1
    @State private var hasPlayedStarAppearAnimation = false
    
    private let ascendedColors: [Color] = [Color(localHex: 0x8E24AA), Color(localHex: 0x311B92)]
    @ObservedObject private var animDriver = SharedAnimationDriver.shared

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                if isHighlighted {
                    Circle()
                        .stroke(Color(localHex: 0xFFD700), lineWidth: 4)
                        .frame(width: 60, height: 60)
                        .scaleEffect(localizationPulse)
                        .opacity(1.5 - Double(localizationPulse)) // Adjusted for 1.5 scale
                        .onAppear {
                            withAnimation(.easeOut(duration: 1.2).repeatCount(2, autoreverses: false)) {
                                localizationPulse = 1.5
                            }
                        }
                    Circle()
                        .fill(RadialGradient(gradient: Gradient(colors: [Color(localHex: 0xFFD700).opacity(0.4), Color.clear]), center: .center, startRadius: 0, endRadius: 40))
                        .frame(width: 80, height: 80)
                }
                
                Circle().fill(RadialGradient(gradient: Gradient(colors: [ascendedColors[1].opacity(0.35), Color.clear]), center: .center, startRadius: 0, endRadius: 45))
                    .frame(width: 90, height: 90)
                    .drawingGroup() // Static background cached
                    .scaleEffect(animDriver.pulseScale)
                
                Circle().strokeBorder(AngularGradient(gradient: Gradient(colors: [Color.clear, ascendedColors[0].opacity(0.5), Color.clear, ascendedColors[1].opacity(0.5), Color.clear]), center: .center), lineWidth: 1)
                    .frame(width: 65, height: 65)
                    .drawingGroup() // Static ring cached
                    .rotationEffect(.degrees(animDriver.rotation))
                
                Circle().strokeBorder(AngularGradient(gradient: Gradient(colors: [ascendedColors[1].opacity(0.4), Color.clear, ascendedColors[0].opacity(0.4), Color.clear]), center: .center), lineWidth: 0.8)
                    .frame(width: 55, height: 55)
                    .drawingGroup() // Static ring cached
                    .rotationEffect(.degrees(-animDriver.rotation)) // Counter rotation
                
                ZStack {
                    Circle().fill(LinearGradient(colors: [ascendedColors[0], ascendedColors[1], Color.black], startPoint: .topLeading, endPoint: .bottomTrailing))
                    ImageUtils.loadImage(source: ascendedLevel.imageSource).resizable().aspectRatio(contentMode: .fill).frame(width: 40, height: 40).clipShape(Circle()).overlay(Color.black.opacity(0.3)).clipShape(Circle())
                    if isCompleted {
                        Image(systemName: "star.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color(localHex: 0xFFD700))
                            .scaleEffect(starAppearProgress)
                            .opacity(starAppearProgress)
                        if starAppearProgress < 1 {
                            Image(systemName: "paperplane.fill")
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                                .opacity(1 - starAppearProgress)
                        }
                    } else if isUnlocked { Image(systemName: "paperplane.fill").font(.system(size: 14)).foregroundColor(.white) }
                    else { Image(systemName: "lock.fill").font(.system(size: 12)).foregroundColor(.white.opacity(0.5)) }
                }.frame(width: 40, height: 40).overlay(Circle().stroke(isCompleted ? Color(localHex: 0xFFD700) : (isHighlighted ? Color(localHex: 0xFFD700) : Color.white), lineWidth: (isCompleted || isHighlighted) ? 3 : 2))
                
                if isUnlocked && !isCompleted {
                    Text("NEW")
                        .font(.system(size: 6, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 3)
                        .padding(.vertical, 1)
                        .background(Color.red)
                        .clipShape(Capsule())
                        .offset(x: 18, y: -18)
                }
            }
            .overlay {
                if isHighlighted && isPinVisible {
                    VStack(spacing: 0) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 32))
                            .foregroundColor(Color(localHex: 0xFFD700))
                            .offset(y: -50)
                            .transition(.scale.combined(with: .opacity))
                    }
                }
            }
            .frame(width: 90, height: 90)
            .scaleEffect(shouldPulse ? 1.25 : 1.0)
            .shadow(color: isHighlighted ? Color(localHex: 0xFFD700).opacity(0.6) : ascendedColors[0].opacity(0.8), radius: isHighlighted ? 15 : 12)
            
            if isCompleted {
                HStack(spacing: 2) { ForEach(0..<3, id: \.self) { i in Image(systemName: "star.fill").font(.system(size: 6)).foregroundColor(i < stars ? Color(localHex: 0xFFD700) : Color.white.opacity(0.3)) } }
                .padding(.horizontal, 4).padding(.vertical, 2).background(LinearGradient(colors: [Color(localHex: 0xFFD700).opacity(0.9), Color(localHex: 0xFFA000).opacity(0.8)], startPoint: .leading, endPoint: .trailing)).cornerRadius(4)
            }
        }.onAppear {
            // Shared Animation Driver handles rotation/pulse
            if isHighlighted { setupAnimation() }
            triggerStarAppearIfNeeded()
        }
        .onChange(of: isHighlighted) { _, newVal in
            if newVal { setupAnimation() }
        }
        .onChange(of: shouldPulse) { _, _ in triggerStarAppearIfNeeded() }
        .onChange(of: isCompleted) { _, _ in triggerStarAppearIfNeeded() }
    }
    
    /// 升级关卡完成后返回时：圆圈内播放「变成星星」动画（仅一次）
    private func triggerStarAppearIfNeeded() {
        guard isCompleted, shouldPulse, !hasPlayedStarAppearAnimation else { return }
        hasPlayedStarAppearAnimation = true
        starAppearProgress = 0
        withAnimation(.spring(response: 0.45, dampingFraction: 0.7)) {
            starAppearProgress = 1
        }
    }
    
    private func setupAnimation() {
        localizationPulse = 0.5
        isPinVisible = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            withAnimation { isPinVisible = false }
        }
    }
}

// MARK: - Chapter Title Component
struct ChapterTitleView: View {
    let stageIndex: Int
    
    var body: some View {
        let fullName = LevelRepository.shared.getChapterName(chapter: stageIndex)
        
        // Step 1: Split by original colon (Chapter ID vs Name Side)
        let colonSplit = fullName.contains("：") ? fullName.components(separatedBy: "：") : fullName.components(separatedBy: ":")
        
        HStack(spacing: 12) {
            // Left Line
            Rectangle()
                .fill(LinearGradient(colors: [.clear, .white.opacity(0.3)], startPoint: .leading, endPoint: .trailing))
                .frame(height: 1)
            
            VStack(spacing: 6) {
                if colonSplit.count >= 2 {
                    // Line 1: Chapter Label (e.g. Chapter 1)
                    Text(colonSplit[0].trimmingCharacters(in: .whitespaces))
                        .font(.system(size: 10, weight: .bold))
                        .tracking(2)
                        .foregroundColor(.white.opacity(0.6))
                    
                    let rest = colonSplit[1...].joined(separator: ":").trimmingCharacters(in: .whitespaces)
                    
                    if rest.contains("·") {
                        let dotSplit = rest.components(separatedBy: "·")
                        
                        // Line 2: First Part of Name
                        Text(dotSplit[0].trimmingCharacters(in: .whitespaces))
                            .font(.system(size: 16, weight: .black))
                            .tracking(1)
                            .foregroundColor(.white)
                        
                        // Line 3: The Dot Indicator (Independent Line)
                        Text("·")
                            .font(.system(size: 20, weight: .black))
                            .foregroundColor(.white.opacity(0.4))
                            .padding(.vertical, -4)
                        
                        // Line 4: Second Part of Name
                        if dotSplit.count >= 2 {
                            Text(dotSplit[1...].joined(separator: "·").trimmingCharacters(in: .whitespaces))
                                .font(.system(size: 16, weight: .black))
                                .tracking(1)
                                .foregroundColor(.white)
                        }
                    } else {
                        // Fallback to 2-line if no dot found
                        Text(rest)
                            .font(.system(size: 16, weight: .black))
                            .tracking(1)
                            .foregroundColor(.white)
                    }
                } else {
                    // Full fallback
                    Text(fullName)
                        .font(.system(size: 14, weight: .black))
                        .tracking(1)
                        .foregroundColor(.white)
                }
            }
            .multilineTextAlignment(.center)
            .fixedSize()
            
            // Right Line
            Rectangle()
                .fill(LinearGradient(colors: [.white.opacity(0.3), .clear], startPoint: .leading, endPoint: .trailing))
                .frame(height: 1)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 40)
    }
}
