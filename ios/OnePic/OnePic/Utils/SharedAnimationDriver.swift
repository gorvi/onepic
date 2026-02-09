import SwiftUI
import Combine
import QuartzCore // Required for CADisplayLink

/// 性能优化：所有关卡节点复用同一个动画驱动，避免120×4=480个独立动画状态
class SharedAnimationDriver: ObservableObject {
    static let shared = SharedAnimationDriver()
    
    @Published var pulseScale: CGFloat = 1.0
    @Published var breatheRingScale: CGFloat = 1.0
    @Published var rotationSatellite1: Double = 0
    @Published var rotationSatellite2: Double = 0
    
    // Generic rotation for any continuous spinning element
    @Published var rotation: Double = 0
    
    // Shared shimmer offset for thumbnails (-2.0 to 2.0)
    @Published var shimmerOffset: CGFloat = -2.0
    
    private var displayLink: CADisplayLink?
    private let startTime: Date
    
    private init() {
        self.startTime = Date()
        startAnimations()
    }
    
    private func startAnimations() {
        // Use CADisplayLink for continuous, synchronized updates that work for late-appearing views
        let link = CADisplayLink(target: self, selector: #selector(updateFrame))
        link.add(to: .current, forMode: .common)
        self.displayLink = link
    }
    
    @objc private func updateFrame() {
        let elapsed = Date().timeIntervalSince(startTime)
        
        // 1. Rotation: 12 seconds per full continuous rotation
        // 0 -> 360
        rotation = (elapsed.remainder(dividingBy: 12.0) / 12.0) * 360.0
        rotationSatellite1 = rotation
        rotationSatellite2 = -((elapsed.remainder(dividingBy: 8.0) / 8.0) * 360.0) // 8s counter-rotation
        
        // 2. Pulse: 1.6 seconds period (0.8s in, 0.8s out)
        // sin wave tailored to map 1.0 -> 1.15
        // (sin(t) + 1) / 2 gives 0->1.
        let pulsePeriod = 1.6
        let pulseProgress = (sin(elapsed * .pi * 2 / pulsePeriod - .pi / 2) + 1.0) / 2.0
        pulseScale = 1.0 + 0.15 * pulseProgress
        
        // 3. Breathe Ring: 2.4 seconds period (1.2s in, 1.2s out)
        // 1.0 -> 1.08
        let breathePeriod = 2.4
        let breatheProgress = (sin(elapsed * .pi * 2 / breathePeriod - .pi / 2) + 1.0) / 2.0
        breatheRingScale = 1.0 + 0.08 * breatheProgress
        
        // 4. Shimmer: 4 seconds linear sweep
        // -2.0 -> 2.0
        let shimmerDuration = 4.0
        let shimmerNorm = elapsed.remainder(dividingBy: shimmerDuration) / shimmerDuration // 0 -> 1
        shimmerOffset = -2.0 + 4.0 * shimmerNorm
    }
    
    deinit {
        displayLink?.invalidate()
    }
}
