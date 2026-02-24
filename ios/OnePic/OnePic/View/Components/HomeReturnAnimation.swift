import SwiftUI
import Combine

/// 返回奖励动画状态（金币/星星复用）
struct RewardReturnAnimationState {
    var from: Int? = nil
    var to: Int? = nil
    var progress: Double = 0
    var flyingCount: Int = 0
    var flyProgress: Double = 0
    var lastFrameFadeOut: Bool = false
    var didEnterLastFrame: Bool = false
    var pendingDeltaForLabel: Int? = nil
    
    mutating func reset() {
        from = nil
        to = nil
        progress = 0
        flyingCount = 0
        flyProgress = 0
        lastFrameFadeOut = false
        didEnterLastFrame = false
        pendingDeltaForLabel = nil
    }
}

/// 返回奖励飞入动画（金币/星星复用）
struct RewardFlyOverlay<Icon: View>: View {
    let count: Int
    let progress: Double
    let start: CGPoint
    let targetFrame: CGRect
    let fallbackTarget: CGPoint
    let lastFrameThreshold: Double
    let lastFrameBelowOffset: CGFloat
    let lastFrameFadeOut: Bool
    @ViewBuilder let icon: () -> Icon
    
    private let arcAmplitude: CGFloat = 24
    private let staggerStep: Double = 0.14
    
    private var hasTarget: Bool {
        targetFrame.width > 0 && targetFrame.height > 0
    }
    
    private var endX: CGFloat {
        hasTarget ? targetFrame.midX : fallbackTarget.x
    }
    
    private var endY: CGFloat {
        hasTarget ? targetFrame.midY : fallbackTarget.y
    }
    
    private var lastFrameY: CGFloat {
        let baseY = hasTarget ? targetFrame.maxY : fallbackTarget.y
        return baseY + lastFrameBelowOffset
    }
    
    private var isLastFrame: Bool {
        progress >= lastFrameThreshold
    }
    
    var body: some View {
        ZStack {
            if isLastFrame {
                icon()
                    .position(x: endX, y: lastFrameY)
                    .scaleEffect(1)
                    .opacity(lastFrameFadeOut ? 0 : 1)
                    .animation(.easeOut(duration: 0.12), value: lastFrameFadeOut)
            } else {
                ForEach(0..<count, id: \.self) { i in
                    let stagger = Double(i) * staggerStep
                    let rawP = min(1, max(0, progress * 1.12 - stagger))
                    let eased = easeOut(rawP)
                    let x = start.x + (endX - start.x) * eased
                    let y = start.y + (endY - start.y) * eased + arcAmplitude * sin(eased * .pi)
                    let scale = 0.45 + 0.55 * eased
                    let opacity = rawP < 0.06 ? rawP / 0.06 : (rawP > 0.94 ? (1 - rawP) / 0.06 : 1)
                    
                    icon()
                        .position(x: x, y: y)
                        .scaleEffect(scale)
                        .opacity(opacity)
                }
            }
        }
    }
    
    private func easeOut(_ t: Double) -> Double {
        guard t > 0, t < 1 else { return t }
        return 1 - pow(1 - t, 1.4)
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
        Group {
            if #available(iOS 17.0, *) {
                Text("\(value)")
                    .contentTransition(.numericText())
            } else {
                Text("\(value)")
            }
        }
        .scaleEffect(tickScale)
        .onChangeCompat(of: value) { newVal in
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
