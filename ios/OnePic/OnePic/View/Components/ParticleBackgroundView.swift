import SwiftUI

struct ParticleBackgroundView: View {
    let theme: String
    @State private var particles: [ThemedParticle] = []
    @State private var currentConfig: ParticleThemeConfig
    
    init(theme: String) {
        self.theme = theme
        _currentConfig = State(initialValue: ParticleThemeUtils.getParticleTheme(theme: theme))
    }
    
    var body: some View {
        TimelineView(.animation) { timeline in
            Canvas(renderer: { context, size in
                let now = timeline.date.timeIntervalSinceReferenceDate
                
                for p in particles {
                    // Stateless update based on Time
                    var currentX = p.x
                    var currentY = p.y
                    
                    if currentConfig.style == .float {
                        let moveSpeed = 0.05 * p.speedMultiplier
                        currentX += cos(p.moveAngle) * moveSpeed * (now.remainder(dividingBy: 100))
                        currentY += sin(p.moveAngle) * moveSpeed * (now.remainder(dividingBy: 100))
                        currentX = currentX.truncatingRemainder(dividingBy: 1.2)
                        currentY = currentY.truncatingRemainder(dividingBy: 1.2)
                    } else {
                        let t = now.remainder(dividingBy: 1000) 
                        currentX += p.speedX * t * 60 
                        currentY += p.speedY * t * 60
                        currentX = (currentX).truncatingRemainder(dividingBy: 1.4)
                        if currentX < -0.2 { currentX += 1.4 }
                        currentY = (currentY).truncatingRemainder(dividingBy: 1.4)
                        if currentY < -0.2 { currentY += 1.4 }
                    }
                    
                    let dx = abs(currentX - 0.5)
                    let dy = abs(currentY - 0.5)
                    let distX = max(0, dx - 0.35)
                    let distY = max(0, dy - 0.35)
                    let edgeFadeX = max(0, 1.0 - (distX / 0.25))
                    let edgeFadeY = max(0, 1.0 - (distY / 0.25))
                    let edgeFade = min(edgeFadeX, edgeFadeY)
                    
                    let breathing = 0.5 + 0.5 * sin(now * 1.5 + p.phase)
                    let baseAlpha = 0.05 + 0.2 * breathing
                    let finalOpacity = baseAlpha * edgeFade
                    
                    if finalOpacity < 0.01 { continue }
                    
                    let cx = currentX * size.width
                    let cy = currentY * size.height
                    let r = p.radius
                    
                    var pContext = context
                    pContext.translateBy(x: cx, y: cy)
                    pContext.rotate(by: .degrees(p.rotation + p.rotationSpeed * now))
                    
                    let scaleFactor = (r * 2.0) / 100.0
                    pContext.scaleBy(x: scaleFactor, y: scaleFactor)
                    pContext.opacity = finalOpacity

                    let particleColor = p.color
                    switch currentConfig.shape {
                    case .circle:
                        pContext.fill(Path(ellipseIn: CGRect(x: -50, y: -50, width: 100, height: 100)), with: .color(particleColor))
                    case .ring:
                        pContext.stroke(Path(ellipseIn: CGRect(x: -50, y: -50, width: 100, height: 100)), with: .color(particleColor), lineWidth: 5)
                    case .square:
                        pContext.fill(Path(CGRect(x: -50, y: -50, width: 100, height: 100)), with: .color(particleColor))
                    case .triangle:
                        var tri = Path()
                        tri.move(to: CGPoint(x: 0, y: -50))
                        tri.addLine(to: CGPoint(x: 50, y: 36.6))
                        tri.addLine(to: CGPoint(x: -50, y: 36.6))
                        tri.closeSubpath()
                        pContext.fill(tri, with: .color(particleColor))
                    case .diamond:
                        var diamond = Path()
                        diamond.move(to: CGPoint(x: 0, y: -50))
                        diamond.addLine(to: CGPoint(x: 35, y: 0))
                        diamond.addLine(to: CGPoint(x: 0, y: 50))
                        diamond.addLine(to: CGPoint(x: -35, y: 0))
                        diamond.closeSubpath()
                        pContext.fill(diamond, with: .color(particleColor))
                    case .hexagon:
                        var hex = Path()
                        for i in 0..<6 {
                            let angle = Angle.degrees(Double(i) * 60 - 30)
                            let px = cos(angle.radians) * 50
                            let py = sin(angle.radians) * 50
                            if i == 0 { hex.move(to: CGPoint(x: px, y: py)) }
                            else { hex.addLine(to: CGPoint(x: px, y: py)) }
                        }
                        hex.closeSubpath()
                        pContext.fill(hex, with: .color(particleColor))
                    case .star:
                        var star = Path()
                        let points = 5
                        for i in 0..<points * 2 {
                            let radius = (i % 2 == 0) ? 50.0 : 20.0
                            let angle = Angle.degrees(Double(i) * (360.0 / Double(points * 2)) - 90)
                            let px = cos(angle.radians) * radius
                            let py = sin(angle.radians) * radius
                            if i == 0 { star.move(to: CGPoint(x: px, y: py)) }
                            else { star.addLine(to: CGPoint(x: px, y: py)) }
                        }
                        star.closeSubpath()
                        pContext.fill(star, with: .color(particleColor))
                    }
                }
            })
        }
        .background(
            ZStack {
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(hex: 0x1A0B2E),
                        Color(hex: 0x0A1F30),
                        Color(hex: 0x050510)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                
                GeometryReader { geo in
                    ZStack {
                        Circle()
                            .fill(Color(hex: 0xFFC1CC).opacity(0.12))
                            .frame(width: geo.size.width * 0.8)
                            .blur(radius: 80)
                            .offset(x: -geo.size.width * 0.2, y: -geo.size.height * 0.1)
                        Circle()
                            .fill(Color(hex: 0xB2EBF2).opacity(0.1))
                            .frame(width: geo.size.width * 0.9)
                            .blur(radius: 100)
                            .offset(x: geo.size.width * 0.3, y: geo.size.height * 0.4)
                    }
                }
            }
        )
        .ignoresSafeArea()
        .onAppear {
            particles = (0..<currentConfig.count).map { _ in ThemedParticle(config: currentConfig) }
        }
        .onChangeCompat(of: theme) { newTheme in
            currentConfig = ParticleThemeUtils.getParticleTheme(theme: newTheme)
            particles = (0..<currentConfig.count).map { _ in ThemedParticle(config: currentConfig) }
        }
    }
}

struct ThemedParticle {
    let x: Double
    let y: Double
    let radius: Double
    let speedX: Double
    let speedY: Double
    let rotation: Double
    let rotationSpeed: Double
    let phase: Double
    let color: Color
    let moveAngle: Double
    let speedMultiplier: Double
    
    init(config: ParticleThemeConfig) {
        self.x = Double.random(in: 0...1)
        self.y = Double.random(in: 0...1)
        self.radius = Double(config.sizeBase) * Double.random(in: 0.5...1.5) * 0.3
        self.color = config.colors.randomElement() ?? .white
        self.rotation = Double.random(in: 0...360)
        self.rotationSpeed = Double.random(in: -30...30)
        self.phase = Double.random(in: 0...6.28)
        self.moveAngle = Double.random(in: 0...2 * .pi)
        self.speedMultiplier = config.speedMultiplier
        
        switch config.style {
        case .float:
            self.speedX = 0
            self.speedY = 0
        case .rise:
            self.speedX = (Double.random(in: 0...1) - 0.5) * 0.05
            self.speedY = -0.05 * Double.random(in: 0.5...1.5) * config.speedMultiplier
        case .fall:
            self.speedX = (Double.random(in: 0...1) - 0.5) * 0.05
            self.speedY = 0.05 * Double.random(in: 0.5...1.5) * config.speedMultiplier
        case .orbit:
            self.speedX = 0
            self.speedY = 0
        }
    }
}
