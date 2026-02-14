import SwiftUI

struct LevelNodeView: View {
    let index: Int
    let level: LevelConfig
    let isLocked: Bool
    let isCompleted: Bool
    let isNextToPlay: Bool
    let stageIndex: Int
    var shouldAnimateUnlock: Bool = false
    var ascendedLevel: LevelConfig? = nil
    var isAscendedUnlocked: Bool = false
    var isAscendedCompleted: Bool = false
    var ascendedStars: Int = 0
    var onAscendedClick: (() -> Void)? = nil
    
    var isNew: Bool = false
    var isHighlighted: Bool = false
    
    @ObservedObject private var animDriver = SharedAnimationDriver.shared
    
    @State private var visualLocked: Bool? = nil
    @State private var isShaking: Bool = false
    @State private var isCharging: Bool = false
    @State private var isExploding: Bool = false
    @State private var lockExplodeScale: CGFloat = 1.0
    @State private var lockExplodeOpacity: Double = 1.0
    @State private var flashBurstOpacity: Double = 0
    @State private var unlockFireworkProgress: CGFloat = 0
    
    @State private var locatePulseScale: CGFloat = 1.0
    @State private var locatePulseOpacity: Double = 0
    @State private var locateIconOpacity: Double = 0
    
    var body: some View {
        let colors = ThemeUtils.getStageColors(stageIndex: stageIndex)
        
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        gradient: Gradient(colors: [
                            isCompleted ? Color(hex: 0x4CAF50).toMacaron().opacity(0.7) : colors[1].toMacaron().opacity(0.7),
                            isCompleted ? Color(hex: 0x2E7D32).toMacaron().opacity(0.3) : colors[0].toMacaron().opacity(0.3),
                            Color.clear
                        ]),
                        center: .center,
                        startRadius: 0,
                        endRadius: 75
                    )
                )
                .frame(width: 150, height: 150)
                .drawingGroup() // Rasterize FIRST so the texture is cached
                .scaleEffect(animDriver.pulseScale) // Scale the cached texture (cheap)
                .blur(radius: 10)

            
            if !isLocked {
                Circle()
                    .stroke(
                        colors[0].opacity(0.5),
                        style: StrokeStyle(lineWidth: 1, dash: [4, 3])
                    )
                    .frame(width: 120, height: 120)
                    .rotationEffect(.degrees(animDriver.rotation))
            }
            
            ZStack {
                Group {
                    if !isLocked {
                        switch level.imageSource {
                        case .asset, .resource:
                            ImageUtils.loadImage(source: level.imageSource)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 96, height: 96)
                                .clipShape(Circle())
                                .overlay {
                                    // PERFORMANCE: Shared shimmer driver
                                    ThumbnailShimmerLayer(offset: animDriver.shimmerOffset)
                                        .clipShape(Circle())
                                }
                                .drawingGroup()
                        default:
                            Circle()
                                .fill(Color.black.opacity(0.7))
                        }
                    } else {
                        Circle()
                            .fill(Color.black.opacity(0.7))
                    }
                }
                
                let showLock = { () -> Bool in
                    if shouldAnimateUnlock, !isLocked {
                        return visualLocked ?? true
                    }
                    return isLocked
                }()
                
                Group {
                    if index == 0 {
                        Image(systemName: "graduationcap.fill")
                            .font(.system(size: 32))
                            .foregroundColor(.white)
                            .shadow(color: Color.cyan.opacity(0.8), radius: 8)
                    } else if showLock {
                        LockShakeBreatheModifier(
                            isShaking: isShaking,
                            isExploding: isExploding,
                            isCharging: isCharging,
                            isLockVisible: showLock
                        ) {
                            Image(systemName: "lock.fill")
                                .foregroundColor(.white.opacity(isCharging ? 0.7 : 0.3))
                                .font(.system(size: 28))
                        }
                        .scaleEffect(isExploding ? lockExplodeScale : (isCharging ? 1.15 : 1.0))
                        .opacity(isExploding ? lockExplodeOpacity : 1.0)
                    } else {
                        Text("\(index)")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
                .animation(.spring(response: 0.4, dampingFraction: 0.8), value: showLock)
                
                if !isLocked {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color.black.opacity(0.1), Color.black.opacity(0.4)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .drawingGroup()
                }
                
                if isCompleted {
                    ZStack {
                        Circle()
                            .strokeBorder(
                                LinearGradient(
                                    colors: [Color(hex: 0x81C784), Color(hex: 0x4CAF50), Color(hex: 0x2E7D32)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 5
                            )
                        MacaronCometRing()
                            .rotationEffect(.degrees(animDriver.rotation)) // Shared Driver
                    }
                } else if isNextToPlay {
                    Circle()
                        .strokeBorder(
                            LinearGradient(
                                colors: [colors[0].toMacaron(), Color(hex: 0xFFFFC1CC), colors[1].toMacaron()],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 10
                        )
                        .scaleEffect(animDriver.pulseScale) // Shared Driver
                } else if !isLocked {
                     Circle()
                        .stroke(Color.white.opacity(0.5), lineWidth: 3)
                } else {
                    Circle()
                        .stroke(Color.white.opacity(0.15), lineWidth: 2)
                }
                
                if flashBurstOpacity > 0 {
                    Circle()
                        .fill(
                            RadialGradient(
                                gradient: Gradient(colors: [
                                    Color.white.opacity(flashBurstOpacity),
                                    colors[0].opacity(flashBurstOpacity * 0.6),
                                    Color.clear
                                ]),
                                center: .center,
                                startRadius: 0,
                                endRadius: 60
                            )
                        )
                        .frame(width: 120, height: 120)
                }
                
                // PERFORMANCE OPTIMIZATION: Only render fireworks when needed
                if unlockFireworkProgress > 0 {
                    UnlockFireworkBurstView(progress: unlockFireworkProgress, colors: colors, radius: 48)
                }
            }
            .frame(width: 96, height: 96)
            .shadow(color: isHighlighted ? .white : colors[0].toMacaron().opacity(0.9), radius: isHighlighted ? 25 : 18)
            .overlay {
                if isNextToPlay {
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [colors[0].opacity(0.35), colors[1].opacity(0.25), colors[0].opacity(0.35)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 8
                        )
                        .frame(width: 130, height: 130)
                        .scaleEffect(animDriver.pulseScale)
                    
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [colors[0].opacity(0.6), colors[1].opacity(0.5), colors[0].opacity(0.6)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                        lineWidth: 14
                    )
                    .frame(width: 114, height: 114)
                    .scaleEffect(animDriver.pulseScale)
                }
            }
            
            if isNew {
                Text("NEW")
                    .font(.system(size: 8, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 2)
                    .background(Color.red)
                    .clipShape(Capsule())
                    .offset(x: 32, y: -32)
                    .zIndex(201)
            }
            
            if isHighlighted {
                Circle()
                    .fill(RadialGradient(gradient: Gradient(colors: [Color(hex: 0xFFD700).opacity(0.6), Color.clear]), center: .center, startRadius: 0, endRadius: 120))
                    .frame(width: 240, height: 240)
                    .zIndex(100)
                
                Circle()
                    .stroke(Color(hex: 0xFFD700).opacity(locatePulseOpacity), lineWidth: 12)
                    .frame(width: 140, height: 140)
                    .scaleEffect(locatePulseScale)
                    .zIndex(101)
                
                VStack(spacing: 0) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 32))
                        .foregroundColor(Color(hex: 0xFFD700))
                        .shadow(color: .black.opacity(0.5), radius: 4)
                        .offset(y: locateIconOpacity > 0 ? -10 : 0)
                    Spacer().frame(height: 100)
                }
                .opacity(locateIconOpacity)
                .zIndex(102)
                .animation(.spring(response: 0.4, dampingFraction: 0.6), value: locateIconOpacity)
            }
        }
        // .drawingGroup() REMOVED: Parent group causes re-rasterization on every frame when children animate
        .overlay(alignment: .topTrailing) {
            if let ascended = ascendedLevel, isCompleted {
                Group {
                    if isAscendedUnlocked {
                        Button(action: { onAscendedClick?() }) {
                            AscendedSatelliteView(
                                ascendedLevel: ascended,
                                isUnlocked: true,
                                isCompleted: isAscendedCompleted,
                                stars: ascendedStars
                            )
                        }
                        .buttonStyle(.plain)
                        .padding(12)
                    } else {
                        AscendedSatelliteView(
                            ascendedLevel: ascended,
                            isUnlocked: false,
                            isCompleted: isAscendedCompleted,
                            stars: ascendedStars
                        )
                    }
                }
                .offset(x: 44, y: -32)
                .zIndex(1000)
            }
        }
        .onAppear {
            if isHighlighted { startPulseAnimation() }
            // PERFORMANCE: Use shared animation driver instead of individual animations
            // if isNextToPlay { ... } -> Driven by SharedAnimationDriver.shared.pulseScale in body
            
            // if !isLocked { startThumbShimmerTimer() } -> Driven by SharedAnimationDriver.shimmerOffset
            if shouldAnimateUnlock, !isLocked, visualLocked == nil { startUnlockSequence() }
        }
        .onChange(of: shouldAnimateUnlock) { _, nowAnimating in
            if nowAnimating, !isLocked { startUnlockSequence() }
        }
    }
    
    private func startPulseAnimation() {
        locatePulseScale = 1.0; locatePulseOpacity = 1.0; locateIconOpacity = 1.0
        withAnimation(Animation.easeInOut(duration: 1.2).repeatCount(2, autoreverses: false)) {
            locatePulseScale = 1.5; locatePulseOpacity = 0
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            withAnimation(.easeOut(duration: 0.5)) { locateIconOpacity = 0 }
        }
    }
    
    private func startUnlockSequence() {
        visualLocked = true; isShaking = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            isShaking = false; isExploding = true; flashBurstOpacity = 0.7
            PlatformUtils.vibrateSuccess()
            withAnimation(.easeOut(duration: 0.4)) {
                lockExplodeScale = 2.2; lockExplodeOpacity = 0; flashBurstOpacity = 0; unlockFireworkProgress = 1
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.65)) { visualLocked = false }
                isExploding = false; unlockFireworkProgress = 0
            }
        }
    }
}

private struct ThumbnailShimmerLayer: View {
    let offset: CGFloat
    var body: some View {
        GeometryReader { geo in
            ZStack {
                LinearGradient(
                    gradient: Gradient(colors: [
                        .clear,
                        .white.opacity(0.05),
                        .white.opacity(0.4),  // Highlight center 
                        .white.opacity(0.05),
                        .clear
                    ]),
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(width: geo.size.width * 0.5, height: geo.size.height * 2.5) // Taller to cover full rotation
                .rotationEffect(.degrees(25)) // Standard sweep angle
                .offset(x: geo.size.width * offset, y: 0) // Simplify y offset
            }
        }
        .allowsHitTesting(false)
    }
}

private struct MacaronCometRing: View {
    var body: some View {
        Circle()
            .trim(from: 0, to: 0.25)
            .stroke(
                AngularGradient(
                    gradient: Gradient(colors: [.white.opacity(0), .white.opacity(0.3), .white.opacity(0.9), .white.opacity(0.3), .clear]),
                    center: .center,
                    startAngle: .degrees(0),
                    endAngle: .degrees(90)
                ),
                style: StrokeStyle(lineWidth: 4, lineCap: .round)
            )
            .blur(radius: 1).padding(2)
    }
}

private struct FireworkParticleConfig {
    let angle: Double; let speed: CGFloat; let size: CGFloat; let color: Color; let shapeKind: Int; let aspectRatio: CGFloat; let rotationDeg: Double
    static func forIndex(_ i: Int) -> FireworkParticleConfig {
        let h = (i * 31 + 17) % 100; let angle = Double(i) / 48 * 2 * .pi + Double((h % 23) - 11) * 0.08
        let speed = 0.6 + CGFloat((h * 7) % 40) / 100; let size = CGFloat(3 + (h * 13) % 8)
        let colors: [Color] = [Color(hex: 0xFF6B6B), Color(hex: 0xFFE66D), Color(hex: 0x4ECDC4), Color(hex: 0x95E1D3), Color(hex: 0xF38181), Color(hex: 0xAA96DA)]
        return FireworkParticleConfig(angle: angle, speed: speed, size: size, color: colors[i % colors.count], shapeKind: (i * 19 + 7) % 5, aspectRatio: 0.5 + CGFloat((h * 3) % 11) / 10, rotationDeg: Double((h * 29 + i * 11) % 360))
    }
}

private struct UnlockFireworkBurstView: View {
    let progress: CGFloat; let colors: [Color]; let radius: CGFloat
    var body: some View {
        GeometryReader { geo in
            let cx = geo.size.width / 2; let cy = geo.size.height / 2
            ZStack {
                ForEach(0..<48, id: \.self) { i in
                    let cfg = FireworkParticleConfig.forIndex(i); let r = radius * progress * cfg.speed
                    particleShape(config: cfg, opacity: Double(1 - progress), w: cfg.shapeKind == 1 ? cfg.size * cfg.aspectRatio : cfg.size, h: cfg.shapeKind == 1 ? cfg.size / cfg.aspectRatio : cfg.size)
                        .position(x: cx + cos(cfg.angle) * r, y: cy + sin(cfg.angle) * r)
                }
            }
        }.frame(width: 96, height: 96).opacity(progress > 0 ? 1 : 0)
    }
    @ViewBuilder private func particleShape(config: FireworkParticleConfig, opacity: Double, w: CGFloat, h: CGFloat) -> some View {
        switch config.shapeKind {
        case 0: Circle().fill(config.color.opacity(opacity)).frame(width: config.size, height: config.size)
        case 1: Ellipse().fill(config.color.opacity(opacity)).frame(width: max(2, w), height: max(2, h)).rotationEffect(.degrees(config.rotationDeg))
        case 2: RoundedRectangle(cornerRadius: 1).fill(config.color.opacity(opacity)).frame(width: max(2, config.size * 0.8), height: max(2, config.size * 1.2)).rotationEffect(.degrees(config.rotationDeg))
        case 3: Capsule().fill(config.color.opacity(opacity)).frame(width: max(2, config.size * 1.2), height: max(2, config.size * 0.6)).rotationEffect(.degrees(config.rotationDeg))
        default: Rectangle().fill(config.color.opacity(opacity)).frame(width: config.size * 0.7, height: config.size * 1.4).rotationEffect(.degrees(45))
        }
    }
}

private struct LockShakeBreatheModifier<Content: View>: View {
    let isShaking: Bool; var isExploding: Bool = false; var isCharging: Bool = false; var isLockVisible: Bool = true; @ViewBuilder let content: () -> Content
    @State private var shakeOffset: CGFloat = -2; @State private var shakeRotation: Double = -8; @State private var breatheScale: CGFloat = 1.0
    var body: some View {
        content().scaleEffect(isExploding || isCharging ? 1.0 : (breatheScale * (isShaking ? 1.3 : 1.0))).rotationEffect(.degrees(isExploding || isCharging ? 0 : shakeRotation)).offset(x: isExploding || isCharging ? 0 : shakeOffset)
            .onAppear { if isLockVisible && !isExploding && !isCharging { startIdleAnimation() } }
    }
    private func startIdleAnimation() {
        withAnimation(Animation.linear(duration: 0.1).repeatForever(autoreverses: true)) { shakeOffset = 2 }
        withAnimation(Animation.linear(duration: 0.15).repeatForever(autoreverses: true)) { shakeRotation = 8 }
        withAnimation(Animation.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) { breatheScale = 1.15 }
    }
}

private struct AscendedSatelliteView: View {
    let ascendedLevel: LevelConfig; let isUnlocked: Bool; let isCompleted: Bool; let stars: Int; @State private var rotationAngle: Double = 0
    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                if isUnlocked { Circle().stroke(Color(hex: 0x8E24AA).opacity(0.5), style: StrokeStyle(lineWidth: 1, dash: [4, 3])).frame(width: 50, height: 50).rotationEffect(.degrees(rotationAngle)) }
                Circle().fill(LinearGradient(colors: [Color(hex: 0x8E24AA), Color(hex: 0x311B92), Color.black], startPoint: .topLeading, endPoint: .bottomTrailing))
                ImageUtils.loadImage(source: ascendedLevel.imageSource).resizable().aspectRatio(contentMode: .fill).frame(width: 40, height: 40).clipShape(Circle()).overlay(Color.black.opacity(0.3)).clipShape(Circle())
                if isCompleted { Image(systemName: "star.fill").font(.system(size: 18)).foregroundColor(Color(hex: 0xFFD700)) }
                else if isUnlocked { 
                    MiniRocketView(scale: 0.5)
                        .offset(y: -5)
                }
                else { Image(systemName: "lock.fill").font(.system(size: 12)).foregroundColor(.white.opacity(0.5)) }
            }.frame(width: 40, height: 40).scaleEffect(isUnlocked ? SharedAnimationDriver.shared.pulseScale : 1.0).overlay(Circle().stroke(isCompleted ? Color(hex: 0xFFD700) : Color.white, lineWidth: isCompleted ? 3 : 2)).shadow(radius: isCompleted ? 12 : 8)
            if isCompleted {
                HStack(spacing: 2) { ForEach(0..<3, id: \.self) { i in Image(systemName: "star.fill").font(.system(size: 6)).foregroundColor(i < stars ? Color(hex: 0xFFD700) : Color.white.opacity(0.3)) } }
                .padding(.horizontal, 4).padding(.vertical, 2).background(LinearGradient(colors: [Color(hex: 0xFFD700).opacity(0.9), Color(hex: 0xFFA000).opacity(0.8)], startPoint: .leading, endPoint: .trailing)).cornerRadius(4)
            }
        }.onAppear { if isUnlocked { withAnimation(Animation.linear(duration: 10).repeatForever(autoreverses: false)) { rotationAngle = 360 } } }
    }
}
