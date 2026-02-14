import SwiftUI

struct IntroView: View {
    var onStartJourney: () -> Void
    
    @State private var scrollOffset: CGFloat = 0
    @State private var showButton = false
    
    // Glitch State
    @State private var glitchOffset: CGFloat = 0
    @State private var glitchOpacity: Double = 1.0
    @State private var glitchTimer: Timer?
    
    // Launch State
    @State private var isLaunching = false
    @State private var isWaiting = false // New state for "ready to launch" phase
    @State private var rocketOffsetY: CGFloat = 0
    @State private var rocketScale: CGFloat = 1.0
    @State private var flameScale: CGFloat = 1.0
    
    @State private var vibrationOffset: CGFloat = 0
    @State private var waitVibrationTimer: Timer?
    
    let fullText = TRANS.get("intro_prologue", "The year is 2077. Earth's energy is depleted, and the once-great civilization stands at the edge of extinction.\n\nYou are the Commander of the 'Ark Initiative.' Our only hope lies in collecting the primal energy cores scattered across the globe to forge a vessel capable of crossing the galaxy.\n\nEvery restored puzzle provides the digital power to ignite the warp engines. Gather the energy, reconstruct the blueprints, and lead humanity across the stars to find our new home.")
    
    var body: some View {
        ZStack {
            // 1. Background (Reusing our Particle Logic but darker/static for intro?)
            // Or use the Stardust logic. For now, repurpose ParticleBackground with "void" theme.
            ParticleBackgroundView(theme: "void")
                .overlay(Color.black.opacity(0.3)) // Darker
            
            VStack {
                // Removed top Spacer to move text area up
                
                // 2. Scrolling Text (Star Wars Style +/- Glitch)
                GeometryReader { proxy in
                    ScrollView {
                        Text(fullText)
                            .font(.system(size: 20, weight: .bold, design: .monospaced))
                            .lineSpacing(10)
                            .foregroundColor(Color(hex: 0x00E5FF)) // Sci-fi Cyan
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                            .padding(.vertical, 60)
                            .frame(width: proxy.size.width)
                            .rotation3DEffect(
                                .degrees(20),
                                axis: (x: 1, y: 0, z: 0) // Tilt back
                            )
                            .offset(x: glitchOffset, y: -scrollOffset) // Manual scroll simulation or auto
                            .opacity(glitchOpacity)
                            .shadow(color: Color(hex: 0x00E5FF).opacity(0.8), radius: 10, x: 0, y: 0)
                    }
                    .disabled(true) // Auto scroll only
                }
                .frame(height: 550) // Keep the larger height
                .mask(
                    LinearGradient(gradient: Gradient(colors: [.clear, .black, .black, .clear]), startPoint: .top, endPoint: .bottom)
                )
                
                // 3. Rocket Launch Button
                if showButton {
                    RocketLaunchControl(
                        isLaunching: isLaunching,
                        isWaiting: isWaiting,
                        rocketOffsetY: rocketOffsetY,
                        rocketScale: rocketScale,
                        flameScale: flameScale,
                        vibrationOffset: vibrationOffset,
                        onLaunch: startLaunchSequence
                    )
                    .offset(y: -50) // Move rocket group up 50px
                    .transition(.opacity.combined(with: .scale))
                }
                
                Spacer().frame(height: 30) // Reduce bottom space
            }
        }
        .onAppear {
            startIntroSequence()
        }
        .onDisappear {
            glitchTimer?.invalidate()
            glitchTimer = nil
            waitVibrationTimer?.invalidate()
            waitVibrationTimer = nil
        }
    }
    
    private func startIntroSequence() {
        // Auto Scroll Text - Speeder scroll (20s -> 12s)
        withAnimation(.linear(duration: 12)) {
            scrollOffset = 450
        }
        
        // Glitch Loop
        glitchTimer = Timer.scheduledTimer(withTimeInterval: 3.5, repeats: true) { _ in
            triggerGlitch()
        }
        
        // Show Button Delay - Wait until text is mostly finished (3s -> 10s)
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
            withAnimation(.spring()) {
                showButton = true
                isWaiting = true // Start waiting effects (flame + shake)
                
                // Continuous high-frequency vibration during waiting
                waitVibrationTimer = Timer.scheduledTimer(withTimeInterval: 0.04, repeats: true) { _ in
                    vibrationOffset = CGFloat.random(in: -1.5...1.5)
                }
            }
        }
    }
    
    private func triggerGlitch() {
        // Random short glitches
        let count = Int.random(in: 2...5)
        for i in 0..<count {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.05) {
                glitchOffset = CGFloat.random(in: -5...5)
                glitchOpacity = Double.random(in: 0.5...1.0)
            }
        }
        // Reset
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(count) * 0.05) {
            glitchOffset = 0
            glitchOpacity = 1.0
        }
    }
    
    private func startLaunchSequence() {
        guard !isLaunching else { return }
        waitVibrationTimer?.invalidate() // Stop waiting vibration
        waitVibrationTimer = nil
        
        isLaunching = true
        isWaiting = false
        
        // 1. Charge Up (Vibration)
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.warning)
        
        // Intense Shake animation
        withAnimation(.linear(duration: 1.0)) {
            flameScale = 4.0 // Engine ignite
            rocketScale = 0.95 // Compress
        }
        
        // Heavy Shake Loop
        let shakeTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { t in
            vibrationOffset = CGFloat.random(in: -4...4)
        }
        
        // 2. Blast Off
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            shakeTimer.invalidate()
            vibrationOffset = 0
            
            generator.notificationOccurred(.success) // Big feedback
            
            withAnimation(.easeIn(duration: 0.8)) {
                rocketOffsetY = -1000 // Fly up
                rocketScale = 0.5 // Perspective shrink
            }
            
            // 3. Callback
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                onStartJourney()
            }
        }
    }
}

struct RocketLaunchControl: View {
    var isLaunching: Bool
    var isWaiting: Bool
    var rocketOffsetY: CGFloat
    var rocketScale: CGFloat
    var flameScale: CGFloat
    var vibrationOffset: CGFloat
    var onLaunch: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            // Rocket Container
            ZStack(alignment: .bottom) {
                // --- DECOUPLED PHYSICS ASSEMBLY (Zero-Drift Model) ---
                ZStack(alignment: .top) {
                    
                // 1. INTEGRATED FLAMES (Must be Background Layer)
                Group {
                    if isWaiting && !isLaunching {
                        // Idle State (Powerful Organic Breathing)
                        EngineFlame(width: 14, height: 60, timeOffset: 0)
                            .offset(y: 72)
                        EngineFlame(width: 6, height: 30, timeOffset: 1.5)
                            .offset(x: -18, y: 72)
                        EngineFlame(width: 6, height: 30, timeOffset: 3.0)
                            .offset(x: 18, y: 72)
                    }
                    
                    if isLaunching {
                        // Launch State (Absolute Anchor Locking + Recess for Glow Sealing)
                        Group {
                            PulseWave().offset(y: 74)
                            PulseWave().offset(x: -18, y: 74)
                            PulseWave().offset(x: 18, y: 74)
                            
                            LaunchThrust(width: 28, height: 160 * flameScale).offset(y: 74)
                            LaunchThrust(width: 8, height: 100 * flameScale).offset(x: -18, y: 74)
                            LaunchThrust(width: 8, height: 100 * flameScale).offset(x: 18, y: 74)
                        }
                        .clipped() // Prevent blur bleed above the nozzle line
                    }
                }
                .scaleEffect(rocketScale, anchor: .top)
                    
                    // 2. SHIP BODY (Must be Foreground Layer to hide flame base)
                    ZStack(alignment: .top) {
                        // A. Fins
                        Path { path in
                            path.move(to: CGPoint(x: 20, y: 55)); path.addCurve(to: CGPoint(x: 5, y: 75), control1: CGPoint(x: 12, y: 55), control2: CGPoint(x: 5, y: 65)); path.addLine(to: CGPoint(x: 18, y: 75)); path.closeSubpath()
                            path.move(to: CGPoint(x: 40, y: 55)); path.addCurve(to: CGPoint(x: 55, y: 75), control1: CGPoint(x: 48, y: 55), control2: CGPoint(x: 55, y: 65)); path.addLine(to: CGPoint(x: 42, y: 75)); path.closeSubpath()
                        }
                        .fill(LinearGradient(colors: [Color(hex: 0xFF1744), Color(hex: 0xB71C1C)], startPoint: .top, endPoint: .bottom))
                        
                        // B. Body
                        ZStack(alignment: .top) {
                            Path { path in
                                path.move(to: CGPoint(x: 30, y: 0)); path.addCurve(to: CGPoint(x: 18, y: 30), control1: CGPoint(x: 28, y: 5), control2: CGPoint(x: 18, y: 15)); path.addLine(to: CGPoint(x: 18, y: 72)); path.addLine(to: CGPoint(x: 42, y: 72)); path.addLine(to: CGPoint(x: 42, y: 30)); path.addCurve(to: CGPoint(x: 30, y: 0), control1: CGPoint(x: 42, y: 15), control2: CGPoint(x: 32, y: 5))
                            }
                            .fill(LinearGradient(stops: [.init(color: .white, location: 0), .init(color: Color(white: 0.8), location: 0.5), .init(color: Color(white: 0.9), location: 1)], startPoint: .leading, endPoint: .trailing))
                            
                            // Nose
                            Path { path in
                                path.move(to: CGPoint(x: 30, y: 0)); path.addCurve(to: CGPoint(x: 18, y: 28), control1: CGPoint(x: 28, y: 5), control2: CGPoint(x: 18, y: 15)); path.addLine(to: CGPoint(x: 42, y: 28)); path.addCurve(to: CGPoint(x: 30, y: 0), control1: CGPoint(x: 42, y: 15), control2: CGPoint(x: 32, y: 5))
                            }
                            .fill(LinearGradient(colors: [Color(hex: 0xD50000), Color(hex: 0xB71C1C)], startPoint: .top, endPoint: .bottom))
                        }
                        
                        // C. Energy Core
                        ZStack {
                            Circle().fill(Color(hex: 0x00E5FF).opacity(0.3)).frame(width: 20, height: 20).blur(radius: 4)
                            Circle().fill(RadialGradient(colors: [Color(hex: 0xE0F7FA), Color(hex: 0x00B0FF)], center: .center, startRadius: 0, endRadius: 7)).frame(width: 14, height: 14)
                        }
                        .offset(y: 42)
                    }
                    .frame(width: 60, height: 80)
                    .scaleEffect(rocketScale, anchor: .bottom)
                    .offset(y: isLaunching ? 0 : -8 + sin(Date().timeIntervalSince1970 * 4) * 6)
                }
                .offset(x: vibrationOffset, y: rocketOffsetY + vibrationOffset)
            }
            .onTapGesture {
                onLaunch()
            }
            
            // Text Button
            Button(action: onLaunch) {
                Text(isLaunching ? "IGNITION..." : TRANS.get("intro_button", "LAUNCH ARK"))
                    .font(.system(size: 18, weight: .bold))
                    .tracking(4)
                    .foregroundColor(.white)
                    .padding(.horizontal, 30)
                    .padding(.vertical, 12)
                    .background(
                        Capsule()
                            .stroke(
                                LinearGradient(colors: [Color(hex: 0x6A11CB), Color(hex: 0x2575FC)], startPoint: .leading, endPoint: .trailing),
                                lineWidth: 2
                            )
                    )
            }
            .opacity(isLaunching ? 0 : 1)
        }
    }
}

// MARK: - Specialized Flame Components
struct EngineFlame: View {
    let width: CGFloat
    let height: CGFloat
    let timeOffset: Double
    
    var body: some View {
        let pulse = 1.0 + 0.15 * sin(Date().timeIntervalSince1970 * 12 + timeOffset)
        
        Capsule()
            .fill(LinearGradient(
                stops: [
                    .init(color: .white, location: 0),
                    .init(color: Color(hex: 0xFFEB3B), location: 0.2), // Bright Yellow
                    .init(color: Color(hex: 0xFF5722).opacity(0.8), location: 0.5), // Orange Red
                    .init(color: Color(hex: 0xBF360C).opacity(0.4), location: 0.8), // Deep Red
                    .init(color: .clear, location: 1.0)
                ],
                startPoint: .top,
                endPoint: .bottom
            ))
            .frame(width: width * pulse, height: height * (0.9 + 0.1 * pulse))
            .blur(radius: width / 3)
    }
}

struct FlameShape: Shape {
    var timer: Double
    var isCore: Bool = false
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let width = rect.width
        let height = rect.height
        
        // 頂部圓角（連接火箭處）
        path.move(to: CGPoint(x: 0, y: width / 2))
        path.addArc(center: CGPoint(x: width / 2, y: width / 2), radius: width / 2, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
        
        // 兩側向下拉伸
        let leftControl = height * 0.4
        let rightControl = height * 0.4
        
        // 底部隨機抖動尖端
        let jitter = sin(timer * 20) * (isCore ? 3 : 8)
        let bottomCenter = CGPoint(x: width / 2 + jitter * 0.2, y: height + jitter)
        
        path.addLine(to: CGPoint(x: width, y: rect.minY + width / 2))
        
        // 右側曲線
        path.addCurve(to: bottomCenter,
                      control1: CGPoint(x: width, y: height * 0.6),
                      control2: CGPoint(x: width * 0.8, y: height * 0.8))
        
        // 左側曲線
        path.addCurve(to: CGPoint(x: 0, y: rect.minY + width / 2),
                      control1: CGPoint(x: width * 0.2, y: height * 0.8),
                      control2: CGPoint(x: 0, y: height * 0.6))
        
        path.closeSubpath()
        return path
    }
    
    var animatableData: Double {
        get { timer }
        set { timer = newValue }
    }
}

struct LaunchThrust: View {
    let width: CGFloat
    let height: CGFloat
    @State private var timer: Double = 0
    
    var body: some View {
        TimelineView(.animation) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            
            ZStack(alignment: .top) {
                // 3. Outer Heat Glow (More organic blur)
                FlameShape(timer: t)
                    .fill(Color(hex: 0xFF5722).opacity(0.15))
                    .frame(width: width * 4.0, height: height * 0.95)
                    .blur(radius: 25)
                
                // 2. Main Flame (Jagged Tapered)
                FlameShape(timer: t)
                    .fill(LinearGradient(
                        colors: [Color(hex: 0xFFEB3B), Color(hex: 0xFF5722), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    ))
                    .frame(width: width * 1.6, height: height)
                    .blur(radius: 4)
                
                // 1. Core Kernel (Intense Piercing)
                FlameShape(timer: t, isCore: true)
                    .fill(LinearGradient(
                        stops: [
                            .init(color: .white, location: 0),
                            .init(color: .white.opacity(0.95), location: 0.1),
                            .init(color: Color(hex: 0xFFEB3B).opacity(0.25), location: 0.5),
                            .init(color: .clear, location: 0.95)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    ))
                    .frame(width: width * 0.7, height: height * 0.9)
                    .blur(radius: 1.5)
            }
        }
    }
}

struct PulseWave: View {
    @State private var waveScale: CGFloat = 0.4
    @State private var waveOpacity: Double = 0.8
    
    var body: some View {
        Circle()
            .stroke(
                RadialGradient(colors: [Color(hex: 0x00E5FF), .clear], center: .center, startRadius: 0, endRadius: 30),
                lineWidth: 2
            )
            .frame(width: 40, height: 40)
            .scaleEffect(waveScale)
            .opacity(waveOpacity)
            .onAppear {
                withAnimation(.easeOut(duration: 0.8).repeatForever(autoreverses: false)) {
                    waveScale = 2.5
                    waveOpacity = 0
                }
            }
    }
}
