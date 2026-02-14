import SwiftUI

struct MiniRocketView: View {
    var scale: CGFloat = 1.0
    var showFlame: Bool = true
    
    @State private var hoverOffset: CGFloat = 0
    @State private var launchYOffset: CGFloat = 0
    @State private var rocketOpacity: Double = 1.0
    @State private var isLaunching: Bool = false
    
    var body: some View {
        ZStack(alignment: .bottom) {
            // 1. Precise High-Energy Flame
            if showFlame {
                TimelineView(.animation) { timeline in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    ZStack(alignment: .top) {
                        // Core Intense Flame - Intensifies during launch
                        MiniFlameShape(timer: t * (isLaunching ? 2.5 : 1.5)) 
                            .fill(LinearGradient(
                                colors: [Color(hex: 0xFFD600), Color(hex: 0xFF6D00), .clear],
                                startPoint: .top,
                                endPoint: .bottom
                            ))
                            .frame(width: 10 * scale, height: (isLaunching ? 40 : 28) * scale)
                            .blur(radius: 1.5)
                        
                        // White Hot Inner Core
                        MiniFlameShape(timer: t * 2.0, isCore: true)
                            .fill(Color.white)
                            .frame(width: 5 * scale, height: (isLaunching ? 25 : 16) * scale)
                            .blur(radius: 0.5)
                    }
                    .offset(y: 20 * scale)
                }
            }
            
            // 2. Refined Icon-Style Rocket Body
            ZStack(alignment: .top) {
                // Symmetrical Balanced Fins
                Path { path in
                    path.move(to: CGPoint(x: 10, y: 22))
                    path.addCurve(to: CGPoint(x: 2, y: 34), control1: CGPoint(x: 5, y: 22), control2: CGPoint(x: 2, y: 28))
                    path.addLine(to: CGPoint(x: 9, y: 34))
                    path.closeSubpath()
                    
                    path.move(to: CGPoint(x: 22, y: 22))
                    path.addCurve(to: CGPoint(x: 30, y: 34), control1: CGPoint(x: 27, y: 22), control2: CGPoint(x: 30, y: 28))
                    path.addLine(to: CGPoint(x: 23, y: 34))
                    path.closeSubpath()
                }
                .fill(LinearGradient(colors: [Color(hex: 0xFF1744), Color(hex: 0xD50000)], startPoint: .top, endPoint: .bottom))
                
                // Capsule Body
                ZStack(alignment: .top) {
                    Capsule()
                        .fill(LinearGradient(stops: [
                            .init(color: .white, location: 0),
                            .init(color: Color(white: 0.92), location: 1)
                        ], startPoint: .leading, endPoint: .trailing))
                        .frame(width: 18, height: 32)
                    
                    Path { path in
                        path.move(to: CGPoint(x: 16, y: 0))
                        path.addArc(center: CGPoint(x: 16, y: 9), radius: 9, startAngle: .degrees(180), endAngle: .degrees(360), clockwise: false)
                        path.closeSubpath()
                    }
                    .fill(Color(hex: 0xD50000))
                    .frame(width: 18, height: 10)
                    .offset(x: -7, y: 0)
                    
                    Circle()
                        .stroke(Color(white: 0.8), lineWidth: 1.5)
                        .background(Circle().fill(Color(hex: 0x263238))) 
                        .frame(width: 8, height: 8)
                        .offset(y: 12)
                }
            }
            .frame(width: 32, height: 40)
        }
        .offset(y: hoverOffset + launchYOffset)
        .opacity(rocketOpacity)
        .scaleEffect(scale * (isLaunching ? 0.8 : 1.0))
        .onAppear {
            startHoverAnimation()
            startLaunchCycle()
        }
    }
    
    private func startHoverAnimation() {
        // Randomized start delay to break synchronicity
        let delay = Double.random(in: 0...0.5)
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            withAnimation(Animation.easeInOut(duration: 0.6).repeatForever(autoreverses: true)) {
                hoverOffset = -4
            }
        }
    }
    
    private func startLaunchCycle() {
        // Randomize initial delay to avoid all rockets flying at once
        let initialDelay = Double.random(in: 0...4)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + initialDelay) {
            launch()
        }
    }
    
    private func launch() {
        // 1. Ignite & Prep (Quick burst)
        withAnimation(.easeIn(duration: 0.3)) {
            isLaunching = true
        }
        
        // 2. Blast Off (Faster sprint)
        withAnimation(.easeIn(duration: 0.6)) {
            launchYOffset = -100 
            rocketOpacity = 0
        }
        
        // 3. Prompt Reset & Loop
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            isLaunching = false
            launchYOffset = 30 // Start from slightly below
            
            // 4. Smooth Return (Faster re-entry)
            withAnimation(.easeOut(duration: 0.4)) {
                launchYOffset = 0
                rocketOpacity = 1.0
            }
            
            // 5. Short interval for continuous feel (1.5 ~ 4s)
            let nextDelay = Double.random(in: 1.5...4.0)
            DispatchQueue.main.asyncAfter(deadline: .now() + nextDelay) {
                launch()
            }
        }
    }
}

struct MiniFlameShape: Shape {
    var timer: Double
    var isCore: Bool = false
    
    var animatableData: Double {
        get { timer }
        set { timer = newValue }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let width = rect.width
        let height = rect.height
        
        path.move(to: CGPoint(x: width / 2, y: 0))
        
        let jitter = CGFloat(sin(timer * 20)) * (isCore ? 1 : 2)
        let flicker = CGFloat(cos(timer * 15)) * (isCore ? 0.5 : 1)
        
        path.addCurve(to: CGPoint(x: width / 2 + jitter, y: height + flicker),
                      control1: CGPoint(x: width + (isCore ? 0 : 2), y: height * 0.3),
                      control2: CGPoint(x: width * 0.8, y: height * 0.7))
        
        path.addCurve(to: CGPoint(x: width / 2, y: 0),
                      control1: CGPoint(x: width * 0.2, y: height * 0.7),
                      control2: CGPoint(x: (isCore ? 0 : -2), y: height * 0.3))
        
        return path
    }
}
