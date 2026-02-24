import SwiftUI
import Combine

// MARK: - Enums & Models

// MARK: - Enums & Models

enum VisitorType: CaseIterable {
    case meteor
    case satellite
    case ufo
    case alien // New type for ejected ET
}

// MARK: - Shapes

struct StarShape: Shape {
    let points: Int
    let innerRatio: Double
    
    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let angle = Double.pi / Double(points)
        let outerRadius = min(rect.width, rect.height) / 2
        let innerRadius = outerRadius * innerRatio
        
        var path = Path()
        for i in 0..<points * 2 {
            let r = i % 2 == 0 ? outerRadius : innerRadius
            let currAngle = CGFloat(i) * CGFloat(angle) - CGFloat.pi / 2
            let x = center.x + CGFloat(cos(Double(currAngle))) * CGFloat(r)
            let y = center.y + CGFloat(sin(Double(currAngle))) * CGFloat(r)
            
            if i == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        path.closeSubpath()
        return path
    }
}

enum VisitorMovementMode {
    case path // Deterministic interpolation (Start -> Target)
    case interacting // User holding it
    case physics // Free floating with velocity
}

struct CelestialVisitor: Identifiable {
    let id = UUID()
    let type: VisitorType
    let startX: Double // Normalized
    let startY: Double
    let targetX: Double
    let targetY: Double
    
    // Physics State
    var currentX: Double // Normalized 0-1
    var currentY: Double
    var velocityX: Double = 0.0 // Normalized per second
    var velocityY: Double = 0.0
    var movementMode: VisitorMovementMode = .path
    
    // Rotation for stumbling alien
    var rotation: Angle = .zero
    var rotationVelocity: Double = 0.0 // Rads per second
    
    let baseSpeed: Double
    let size: Double
    let color: Color
    
    // Depth / Scale properties
    let startScale: Double
    let targetScale: Double
    
    // Layering
    let isForeground: Bool
    
    // UFO Specific
    var hasPassenger: Bool = true
    
    // Flag Specific (Easter Egg)
    var isDisplayingFlag: Bool = false
    
    var progress: Double = 0.0
    
    var currentScale: Double {
        if movementMode == .interacting {
            return (startScale + (targetScale - startScale) * progress) * 1.2 // Pop effect
        }
        return startScale + (targetScale - startScale) * progress
    }
    
    let randomSeed: Double = Double.random(in: 0...1000)
}

enum VisitorLayer {
    case background
    case foreground
}

// MARK: - Manager (Logic)

class CelestialVisitorManager: ObservableObject {
    @Published var activeVisitors: [CelestialVisitor] = []
    
    // Curated Palette (Neon/Cyberpunk)
    private let visitorColors: [Color] = [
        Color(hex: 0x00E5FF), // Cyan
        Color(hex: 0xD500F9), // Purple
        Color(hex: 0xFF4081), // Pink
        Color(hex: 0xFFFFD600), // Gold
        Color(hex: 0x00E676)  // Green
    ]
    
    private var lastSpawnTime: TimeInterval = 0
    private var nextSpawnDelay: TimeInterval = 0
    private var lastUpdateTime: TimeInterval?
    
    // Interaction State
    private var interactingVisitorId: UUID?
    private var lastDragLocation: CGPoint?
    
    init() {
        scheduleNextSpawn()
    }
    
    func update(time: TimeInterval) {
        let delta = (lastUpdateTime.map { time - $0 }) ?? 0
        lastUpdateTime = time
        update(time: time, deltaTime: delta)
    }
    
    func update(time: TimeInterval, deltaTime: Double) {
        // 1. Spawn Logic
        if time - lastSpawnTime > nextSpawnDelay {
            if activeVisitors.count < 3 { // Max concurrent
                spawnVisitor()
            }
            lastSpawnTime = time
            scheduleNextSpawn()
        }
        
        // 2. Move Logic
        for i in activeVisitors.indices {
            var v = activeVisitors[i]
            
            switch v.movementMode {
            case .path:
                v.progress += deltaTime * v.baseSpeed
                // Update Pos from Interpolation
                v.currentX = v.startX + (v.targetX - v.startX) * v.progress
                v.currentY = v.startY + (v.targetY - v.startY) * v.progress
                
                if v.progress >= 1.0 {
                    // Remove later
                }
                
            case .interacting:
                // Safety Check: If this visitor thinks it's interacting, but the manager
                // isn't tracking it (e.g. drag cancelled), or tracking a different one, release it.
                if interactingVisitorId != v.id {
                    v.movementMode = .physics
                    // Give it a nudge to avoid static
                    v.velocityX = v.baseSpeed
                    v.velocityY = v.baseSpeed
                }
                break
                
            case .physics:
                // 1. Move (Integration)
                v.currentX += v.velocityX * deltaTime
                v.currentY += v.velocityY * deltaTime
                
                // Rotation (Tumbling)
                // v.rotationVelocity is radians/second

                // Property says "Rads per second".
                v.rotation += Angle(radians: v.rotationVelocity * deltaTime)
                
                // 2. Adaptive Friction / Cruising Logic
                let currentSpeed = hypot(v.velocityX, v.velocityY)
                let cruisingSpeed = v.baseSpeed
                
                if currentSpeed > cruisingSpeed {
                    // Decelerate to cruising speed
                    v.velocityX *= 0.99
                    v.velocityY *= 0.99
                    
                    if hypot(v.velocityX, v.velocityY) < cruisingSpeed {
                        let angle = atan2(v.velocityY, v.velocityX)
                        v.velocityX = cos(angle) * cruisingSpeed
                        v.velocityY = sin(angle) * cruisingSpeed
                    }
                } else {
                    // Cruising Mode
                    if currentSpeed < cruisingSpeed * 0.1 {
                        // Boost logic if stopped
                        let originalDx = v.targetX - v.startX
                        let originalDy = v.targetY - v.startY
                        let norm = hypot(originalDx, originalDy)
                        if norm > 0 {
                            v.velocityX = (originalDx / norm) * cruisingSpeed
                            v.velocityY = (originalDy / norm) * cruisingSpeed
                        } else {
                            v.velocityX = cruisingSpeed
                        }
                    } else if currentSpeed < cruisingSpeed {
                         // Normalize
                         let scale = cruisingSpeed / currentSpeed
                         v.velocityX *= scale
                         v.velocityY *= scale
                    }
                }
            }
            
            // Update Debug Log
            // let spd = hypot(v.velocityX, v.velocityY)
            // v.debugLog = String(format: "S:%.3f", spd)
            
            activeVisitors[i] = v
        }
        
        // 3. Cleanup (Out of bounds or finished path)
        activeVisitors.removeAll { v in
            if v.movementMode == .path {
                return v.progress >= 1.0
            } else {
                // Physics mode: remove if far out of bounds
                return v.currentX < -0.5 || v.currentX > 1.5 || v.currentY < -0.5 || v.currentY > 1.5
            }
        }
    }
    
    // MARK: - Interaction
    
    func handleDragStart(location: CGPoint, viewSize: CGSize) {
        // Find hit
        for i in activeVisitors.indices {
            let v = activeVisitors[i]
            let px = v.currentX * viewSize.width
            let py = v.currentY * viewSize.height
            // Hit box: size * scale * 2 (generous)
            let radius = v.size * v.currentScale * 3.0 
            
            if hypot(px - location.x, py - location.y) < radius {
                interactingVisitorId = v.id
                activeVisitors[i].movementMode = .interacting
                lastDragLocation = location
                
                // Haptic Feedback
                #if os(iOS)
                let generator = UIImpactFeedbackGenerator(style: .medium)
                generator.impactOccurred()
                #endif
                return // Only grab one
            }
        }
    }
    
    func handleDragUpdate(location: CGPoint, viewSize: CGSize) {
        guard let id = interactingVisitorId, let index = activeVisitors.firstIndex(where: { $0.id == id }), let lastLoc = lastDragLocation else { return }
        
        // Calculate Physics Delta
        let dx = (location.x - lastLoc.x) / viewSize.width
        let dy = (location.y - lastLoc.y) / viewSize.height
        
        // Shake Detection Logic
        let v = activeVisitors[index]
        if v.type == .ufo && v.hasPassenger {
            let pixelDist = hypot(location.x - lastLoc.x, location.y - lastLoc.y)
            if pixelDist > 40 {
                activeVisitors[index].hasPassenger = false
                let ejectVelocity = CGSize(width: (location.x - lastLoc.x) * 2.0, height: (location.y - lastLoc.y) * 2.0)
                spawnAlien(from: activeVisitors[index], velocity: ejectVelocity, viewSize: viewSize)
                
                #if os(iOS)
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.impactOccurred()
                #endif
            }
        } else if v.type == .satellite && !v.isDisplayingFlag {
            let pixelDist = hypot(location.x - lastLoc.x, location.y - lastLoc.y)
            if pixelDist > 40 {
                activeVisitors[index].isDisplayingFlag = true
                #if os(iOS)
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.impactOccurred()
                #endif
            }
        }
        
        activeVisitors[index].currentX += dx
        activeVisitors[index].currentY += dy
        
        lastDragLocation = location
    }
    
    private func spawnAlien(from ufo: CelestialVisitor, velocity: CGSize, viewSize: CGSize) {
        // Eject ET
        var vx = 0.0
        var vy = 0.0
        if viewSize.width > 0 {
            vx = velocity.width / viewSize.width
            vy = velocity.height / viewSize.height
        }
        
        let alien = CelestialVisitor(
            type: .alien,
            startX: ufo.currentX,
            startY: ufo.currentY,
            targetX: ufo.currentX + vx * 10, // Far away target
            targetY: ufo.currentY + vy * 10,
            currentX: ufo.currentX,
            currentY: ufo.currentY,
            velocityX: vx,
            velocityY: vy,
            movementMode: .physics,
            rotation: .zero,
            rotationVelocity: Double.random(in: -10...10), // Tumbling spin
            baseSpeed: 0.1,
            size: 14.0, // Alien size
            color: Color(hex: 0x4CAF50),
            startScale: ufo.currentScale,
            targetScale: ufo.currentScale,
            isForeground: ufo.isForeground
        )
        
        activeVisitors.append(alien)
    }
    
    func handleDragEnd(velocity: CGSize, viewSize: CGSize) {
        guard let id = interactingVisitorId, let index = activeVisitors.firstIndex(where: { $0.id == id }) else { return }
        releaseVisitor(index: index, velocity: velocity, viewSize: viewSize)
    }
    
    // Explicit safety valve called by View when gesture state resets unexpectedly
    func forceRelease() {
        guard let id = interactingVisitorId, let index = activeVisitors.firstIndex(where: { $0.id == id }) else { return }
        // Release with zero velocity -> Triggers Resume/Boost logic
        releaseVisitor(index: index, velocity: .zero, viewSize: .zero)
    }
    
    private func releaseVisitor(index: Int, velocity: CGSize, viewSize: CGSize) {
        // Ensure cleanup
        defer {
            interactingVisitorId = nil
            lastDragLocation = nil
        }
        
        let v = activeVisitors[index]
        
        // Convert screen velocity to normalized velocity if viewSize is valid
        var vx = 0.0
        var vy = 0.0
        if viewSize.width > 0 && viewSize.height > 0 {
            vx = velocity.width / viewSize.width
            vy = velocity.height / viewSize.height
        }
        
        // Threshold for fling
        if hypot(vx, vy) > 0.1 {
            // Apply Fling
            activeVisitors[index].movementMode = .physics
            activeVisitors[index].velocityX = vx
            activeVisitors[index].velocityY = vy
        } else {
            // Resume Logic: Give it a BOOST ("Jump Start")
            activeVisitors[index].movementMode = .physics
            
            // Resume previous general direction
            let originalDx = v.targetX - v.startX
            let originalDy = v.targetY - v.startY
            
            // Normalize direction
            let distance = hypot(originalDx, originalDy)
            let dirX = distance > 0 ? originalDx / distance : 0
            let dirY = distance > 0 ? originalDy / distance : 0
            
            // Apply Boost: 4x base speed initially
            let boostMultiplier = 4.0
            activeVisitors[index].velocityX = dirX * v.baseSpeed * boostMultiplier
            activeVisitors[index].velocityY = dirY * v.baseSpeed * boostMultiplier
        }
    }
    
    private func scheduleNextSpawn() {
        nextSpawnDelay = Double.random(in: 4.0...6.0)
    }
    
    private func spawnVisitor() {
        // Don't spawn aliens naturally
        let type = VisitorType.allCases.filter { $0 != .alien }.randomElement()!
        let side = Int.random(in: 0...3) // 0:Top, 1:Bottom, 2:Left, 3:Right
        
        var startX = 0.0
        var startY = 0.0
        var targetX = 0.0
        var targetY = 0.0
        var speed = 0.05
        var baseSize = 0.0
        
        // Randomize start/end positions based on side
        switch side {
        case 0: // Top -> Bottom
            startX = Double.random(in: 0...1); startY = -0.2
            targetX = Double.random(in: 0...1); targetY = 1.2
        case 1: // Bottom -> Top
            startX = Double.random(in: 0...1); startY = 1.2
            targetX = Double.random(in: 0...1); targetY = -0.2
        case 2: // Left -> Right
            startX = -0.2; startY = Double.random(in: 0...1)
            targetX = 1.2; targetY = Double.random(in: 0...1)
        case 3: // Right -> Left
            startX = 1.2; startY = Double.random(in: 0...1)
            targetX = -0.2; targetY = Double.random(in: 0...1)
        default: break
        }
        
        switch type {
        case .meteor:
            speed = 0.35
            baseSize = 10.0
        case .satellite:
            speed = 0.04
            baseSize = 15.0
        case .ufo:
            speed = 0.12
            baseSize = 14.0
        case .alien:
             speed = 0.1
             baseSize = 10.0
        }
        
        // Depth Logic
        let depthMode = Int.random(in: 0...2) // 0:Approach, 1:Recede, 2:PassBy
        var startScale = 1.0
        var targetScale = 1.0
        
        switch depthMode {
        case 0: // Approach
            startScale = Double.random(in: 0.4...0.6)
            targetScale = Double.random(in: 1.1...1.4)
        case 1: // Recede
            startScale = Double.random(in: 1.1...1.4)
            targetScale = Double.random(in: 0.4...0.6)
        default: // PassBy
            let s = Double.random(in: 0.7...1.1)
            startScale = s
            targetScale = s * Double.random(in: 0.9...1.1)
        }
        
        // Layer Logic: 20% chance to be foreground (Top Layer)
        let isForeground = Double.random(in: 0...1) < 0.2
        
        let newVisitor = CelestialVisitor(
            type: type,
            startX: startX, startY: startY,
            targetX: targetX, targetY: targetY,
            currentX: startX, currentY: startY, // Init Current
            movementMode: .path,
            baseSpeed: speed,
            size: baseSize,
            color: visitorColors.randomElement()!,
            startScale: startScale,
            targetScale: targetScale,
            isForeground: isForeground
        )
        
        // Ensure UI update on main thread
        DispatchQueue.main.async {
            self.activeVisitors.append(newVisitor)
        }
    }
}

// MARK: - Main View (Renderer)

struct CelestialVisitorView: View {
    @ObservedObject var manager: CelestialVisitorManager
    let layer: VisitorLayer
    let currentTime: TimeInterval
    
    var body: some View {
        Canvas(renderer: { context, size in
            // Filter visitors for this layer
            let visitors = manager.activeVisitors.filter {
                layer == .foreground ? $0.isForeground : !$0.isForeground
            }
            
            // Resolve Symbols
            let satSymbol = context.resolveSymbol(id: "sat_body")
            let satPanelsSymbol = context.resolveSymbol(id: "sat_panels")
            let hullSymbol = context.resolveSymbol(id: "ufo_hull")
            let domeSymbol = context.resolveSymbol(id: "ufo_dome")
            let alienSymbol = context.resolveSymbol(id: "alien")
            
            for visitor in visitors {
                drawVisitor(context: context, size: size, visitor: visitor, time: currentTime, satSymbol: satSymbol, satPanelsSymbol: satPanelsSymbol, hullSymbol: hullSymbol, domeSymbol: domeSymbol, alienSymbol: alienSymbol)
            }
            


        }, symbols: {
            // Define Symbols for Static Geometry (Optimization)
            // 1. Satellite Body (More Realistic space-tech look)
            ZStack {
                let bodyW = 100.0 / 1.5
                let bodyH = 100.0 / 1.8
                
                // Main Chassis with Gold Foil (MLI) Texture
                RoundedRectangle(cornerRadius: 4)
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color(hex: 0xFFD54F), // Bright Gold
                                Color(hex: 0xFFA000), // Deep Gold
                                Color(hex: 0x827717)  // Olive-y Shadow
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: bodyW, height: bodyH)
                    .shadow(color: .black.opacity(0.3), radius: 2, x: 1, y: 1)
                
                // Structural Ribbing / Details
                VStack(spacing: 8) {
                    ForEach(0..<3) { _ in
                        Rectangle()
                            .fill(Color.black.opacity(0.15))
                            .frame(width: bodyW - 10, height: 1)
                    }
                }
                
                // Optical Sensor / Camera Lens
                ZStack {
                    Circle()
                        .fill(Color(hex: 0x212121)) // Black casing
                        .frame(width: bodyW * 0.4, height: bodyW * 0.4)
                    
                    Circle()
                        .fill(RadialGradient(
                            gradient: Gradient(colors: [Color(hex: 0x01579B), .black]),
                            center: .center,
                            startRadius: 0,
                            endRadius: (bodyW * 0.4) / 2
                        ))
                        .frame(width: bodyW * 0.3, height: bodyW * 0.3)
                    
                    // Gloss highlight
                    Circle()
                        .fill(.white.opacity(0.3))
                        .frame(width: 4, height: 4)
                        .offset(x: -3, y: -3)
                }
                .offset(y: -4)
                
                // Side Detail (Sensor Port)
                Circle()
                    .fill(Color(hex: 0x455A64))
                    .frame(width: 8, height: 8)
                    .offset(x: bodyW/2 - 8, y: bodyH/2 - 8)
            }
            .frame(width: 100, height: 100)
            .tag("sat_body")
            
            // 1b. Satellite Panels (Black with Grid & Connector)
            Group {
                ZStack {
                    let panelW = 45.0
                    let panelH = 28.0
                    let gap = 18.0
                    
                    // Left Connector Bar
                    Rectangle()
                        .fill(Color(hex: 0xB0BEC5))
                        .frame(width: gap, height: 4)
                        .offset(x: -panelW/2 - gap/2)
                    
                    // Right Connector Bar
                    Rectangle()
                        .fill(Color(hex: 0xB0BEC5))
                        .frame(width: gap, height: 4)
                        .offset(x: panelW/2 + gap/2)
                        
                    // Left Panel
                    ZStack {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(.black)
                        // Grid pattern
                        VStack(spacing: 4) {
                            ForEach(0..<4) { _ in
                                Rectangle().fill(Color(hex: 0x1976D2).opacity(0.6)).frame(height: 2)
                            }
                        }
                        HStack(spacing: 6) {
                            ForEach(0..<5) { _ in
                                Rectangle().fill(Color(hex: 0x1976D2).opacity(0.6)).frame(width: 1)
                            }
                        }
                    }
                    .frame(width: panelW, height: panelH)
                    .offset(x: -panelW/2 - gap)
                    
                    // Right Panel
                    ZStack {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(.black)
                        // Grid pattern
                        VStack(spacing: 4) {
                            ForEach(0..<4) { _ in
                                Rectangle().fill(Color(hex: 0x1976D2).opacity(0.6)).frame(height: 2)
                            }
                        }
                        HStack(spacing: 6) {
                            ForEach(0..<5) { _ in
                                Rectangle().fill(Color(hex: 0x1976D2).opacity(0.6)).frame(width: 1)
                            }
                        }
                    }
                    .frame(width: panelW, height: panelH)
                    .offset(x: panelW/2 + gap)
                }
            }
            .frame(width: 150, height: 100)
            .drawingGroup() // Optimize complex grid rendering
            .tag("sat_panels")
            
            // 2. UFO Body

                // So Circle center y = 39 - 25 = 14.
                // Frame should be centered at y=14. Frame y = 14 - 25 = -11. 
                // Wait, let's just use simple offsets from 50,50 center.
                
                ZStack(alignment: .center) {
                    let ufoWidth = 100.0
                    let discHeight = ufoWidth * 0.35 // Restored to 0.35 for original thickness
                    // Body (Fully Opaque)
                    Ellipse()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color(hex: 0x263238), // Solid Charcoal
                                    Color(hex: 0x90A4AE), // Solid Steel
                                    Color(hex: 0x263238)  // Solid Charcoal
                                ]),
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: ufoWidth, height: discHeight)
                        .opacity(1.0) // Explicitly opaque
                }
            .frame(width: 100, height: 100)
            .drawingGroup()
            .tag("ufo_hull")
            
            // 3. UFO Dome (Symbol)
            Group {
                ZStack(alignment: .center) {
                    let ufoWidth = 100.0
                    let discHeight = ufoWidth * 0.22 // 22
                    let domeWidth = ufoWidth * 0.5 // 50
                    
                    // Dome (Maximum Clarity Glass Look)
                    Circle()
                        .trim(from: 0.5, to: 1.1)
                        .fill(
                            RadialGradient(
                                gradient: Gradient(colors: [
                                    Color(hex: 0x81D4FA).opacity(0.25), // Much more transparent
                                    Color(hex: 0x0288D1).opacity(0.35), 
                                    Color(hex: 0x01579B).opacity(0.5)  
                                ]),
                                center: .bottom,
                                startRadius: 0,
                                endRadius: domeWidth * 0.9
                            )
                        )
                        .frame(width: domeWidth, height: domeWidth)
                        .offset(y: -discHeight / 4) // Centered on the hull top edge
                }
            }
            .frame(width: 100, height: 100)
            .tag("ufo_dome")

            // 3c. Chinese Flag (Five-Star Red Flag)
            Group {
                ZStack {
                    let flagW = 60.0
                    let flagH = 40.0
                    
                    // Red Background
                    Rectangle()
                        .fill(.red)
                        .frame(width: flagW, height: flagH)
                    
                    // Stars (Native Shape implementations for reliability in Symbols)
                    ZStack {
                        // Big Star
                        StarShape(points: 5, innerRatio: 0.4)
                            .fill(.yellow)
                            .frame(width: 10, height: 10)
                            .offset(x: -flagW/4, y: -flagH/4)
                        
                        // Small Stars
                        VStack(spacing: 2) {
                            StarShape(points: 5, innerRatio: 0.4).fill(.yellow).frame(width: 3.5, height: 3.5).offset(x: -flagW/10, y: -flagH/10)
                            StarShape(points: 5, innerRatio: 0.4).fill(.yellow).frame(width: 3.5, height: 3.5).offset(x: -flagW/14, y: -flagH/20)
                            StarShape(points: 5, innerRatio: 0.4).fill(.yellow).frame(width: 3.5, height: 3.5).offset(x: -flagW/14, y: flagH/20)
                            StarShape(points: 5, innerRatio: 0.4).fill(.yellow).frame(width: 3.5, height: 3.5).offset(x: -flagW/10, y: flagH/10)
                        }
                        .offset(x: 2, y: -flagH/8)
                    }
                }
            }
            .frame(width: 100, height: 100)
            .tag("flag")



            // 4. Alien (Cute Cartoon Style - Matching Reference Image)
            Group {
                ZStack {
                    // Legs (Behind body)
                    HStack(spacing: 14) {
                        // Left Leg
                        RoundedRectangle(cornerRadius: 4)
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: 0x7CB342), Color(hex: 0x558B2F)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .frame(width: 10, height: 35)
                            .offset(y: 15)
                        
                        // Right Leg
                        RoundedRectangle(cornerRadius: 4)
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: 0x7CB342), Color(hex: 0x558B2F)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .frame(width: 10, height: 35)
                            .offset(y: 15)
                    }
                    .offset(y: 30)
                    
                    // Body (Slender torso)
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: 0x9CCC65), Color(hex: 0x7CB342), Color(hex: 0x558B2F)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 28, height: 40)
                        .overlay(
                            // Subtle muscle lines
                            VStack(spacing: 6) {
                                Rectangle().fill(Color(hex: 0x558B2F).opacity(0.3)).frame(width: 14, height: 1)
                                Rectangle().fill(Color(hex: 0x558B2F).opacity(0.3)).frame(width: 12, height: 1)
                            }
                            .offset(y: 2)
                        )
                        .offset(y: 12)
                    
                    // Arms
                    HStack(spacing: 30) {
                        // Left Arm
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: 0x9CCC65), Color(hex: 0x7CB342)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .frame(width: 8, height: 28)
                            .rotationEffect(.degrees(-25))
                            .offset(x: -4, y: 6)
                        
                        // Right Arm
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: 0x9CCC65), Color(hex: 0x7CB342)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .frame(width: 8, height: 28)
                            .rotationEffect(.degrees(25))
                            .offset(x: 4, y: 6)
                    }
                    
                    // Head (Large oval)
                    ZStack {
                        // Main head shape
                        Ellipse()
                            .fill(
                                RadialGradient(
                                    colors: [Color(hex: 0xAED581), Color(hex: 0x9CCC65), Color(hex: 0x7CB342)],
                                    center: .top,
                                    startRadius: 0,
                                    endRadius: 50
                                )
                            )
                            .frame(width: 55, height: 50)
                            .overlay(
                                // Outline stroke
                                Ellipse()
                                    .stroke(Color(hex: 0x558B2F), lineWidth: 1.5)
                            )
                        
                        // Eyes (Big almond shaped)
                        HStack(spacing: 8) {
                            // Left Eye
                            ZStack {
                                Ellipse()
                                    .fill(Color.black)
                                    .frame(width: 16, height: 20)
                                // White highlight
                                Circle()
                                    .fill(Color.white.opacity(0.9))
                                    .frame(width: 5, height: 5)
                                    .offset(x: -3, y: -4)
                            }
                            
                            // Right Eye
                            ZStack {
                                Ellipse()
                                    .fill(Color.black)
                                    .frame(width: 16, height: 20)
                                // White highlight
                                Circle()
                                    .fill(Color.white.opacity(0.9))
                                    .frame(width: 5, height: 5)
                                    .offset(x: -3, y: -4)
                            }
                        }
                        .offset(y: 2)
                        
                        // Subtle smile
                        Path { path in
                            path.move(to: CGPoint(x: -4, y: 0))
                            path.addQuadCurve(to: CGPoint(x: 4, y: 0), control: CGPoint(x: 0, y: 3))
                        }
                        .stroke(Color(hex: 0x558B2F), lineWidth: 1)
                        .frame(width: 10, height: 5)
                        .offset(y: 16)
                    }
                    .offset(y: -28)
                }
            }
            .frame(width: 100, height: 100)
            .drawingGroup()
            .tag("alien")
        })
        .drawingGroup() // Optimize rendering (Metal)
        .allowsHitTesting(false) // CRITICAL: Ensure clicks pass through (especially foreground)
    }
    
    // MARK: - Drawing Logic
    
    // Updated signature to accept symbols
    private func drawVisitor(context: GraphicsContext, size: CGSize, visitor: CelestialVisitor, time: TimeInterval, satSymbol: GraphicsContext.ResolvedSymbol?, satPanelsSymbol: GraphicsContext.ResolvedSymbol?, hullSymbol: GraphicsContext.ResolvedSymbol?, domeSymbol: GraphicsContext.ResolvedSymbol?, alienSymbol: GraphicsContext.ResolvedSymbol?) {
        let currentX = visitor.currentX
        let currentY = visitor.currentY
        let scale = visitor.currentScale
        
        let screenX = currentX * size.width
        let screenY = currentY * size.height
        
        // The movement mode drawing should probably also be done in a local context
        // to avoid affecting other visitor types if they don't explicitly translate.
        // For now, let's keep it as is, but note the potential for refinement.
        switch visitor.movementMode {
        case .path:
            break
        case .interacting:
            // Break from path logic, no extra drawing needed
            break
        case .physics:
            // Physics logic handled by manager, no debug drawing needed
            break
        }
        
        // Create a local context for drawing the visitor
        var visitorContext = context
        visitorContext.translateBy(x: screenX, y: screenY)
        visitorContext.scaleBy(x: scale, y: scale)
        
        switch visitor.type {
        case .meteor:
            drawMeteor(context: &visitorContext, visitor: visitor, time: time)
        case .satellite:
            drawSatellite(context: &visitorContext, visitor: visitor, time: time, symbol: satSymbol, panelsSymbol: satPanelsSymbol)
        case .ufo:
            drawUFO(context: &visitorContext, visitor: visitor, time: time, hullSymbol: hullSymbol, domeSymbol: domeSymbol, alienSymbol: alienSymbol)
        case .alien:
            var alienCtx = visitorContext
            let scaleFactor = (visitor.size * 4.4) / 100.0 // Doubled size for ejected alien
            alienCtx.scaleBy(x: scaleFactor, y: scaleFactor)
            drawDynamicAlien(context: &alienCtx, visitor: visitor, time: time, isInsideUFO: false)
        }
    }
    
    private func drawMeteor(context: inout GraphicsContext, visitor: CelestialVisitor, time: TimeInterval) {
        let size = visitor.size
        
        // Calculate angle based on movement mode
        let angle: Double
        if visitor.movementMode == .physics {
            // Use velocity for direction
            if abs(visitor.velocityX) > 0.001 || abs(visitor.velocityY) > 0.001 {
                angle = atan2(visitor.velocityY, visitor.velocityX)
            } else {
                angle = atan2(visitor.targetY - visitor.startY, visitor.targetX - visitor.startX)
            }
        } else {
            // Standard path direction
            angle = atan2(visitor.targetY - visitor.startY, visitor.targetX - visitor.startX)
        }
        
        context.withCGContext { _ in
            context.rotate(by: Angle(radians: angle))
        }
        
        let t = time + visitor.randomSeed
        let r = (visitor.color.components.red + sin(t) * 0.15).clamped(to: 0...1)
        let g = (visitor.color.components.green + cos(t * 0.9) * 0.15).clamped(to: 0...1)
        let b = (visitor.color.components.blue + sin(t * 1.1) * 0.15).clamped(to: 0...1)
        let dynamicColor = Color(red: r, green: g, blue: b)
        
        let trailLengthFactor = 1.0 + sin(t * 2.0) * 0.6
        for i in 0..<12 {
            let alpha = (1.0 - Double(i) / 12.0) * 0.5
            let offset = Double(i) * 18.0 * trailLengthFactor
            let radius = size * 2.0 * (1.0 - Double(i) / 15.0)
            
            let trailRect = CGRect(x: -offset - radius, y: -radius, width: radius * 2, height: radius * 2)
            context.fill(Path(ellipseIn: trailRect), with: .radialGradient(
                Gradient(colors: [dynamicColor.opacity(alpha), .clear]),
                center: CGPoint(x: trailRect.midX, y: trailRect.midY),
                startRadius: 0,
                endRadius: radius
            ))
        }
        
        context.fill(Path(ellipseIn: CGRect(x: -size*1.5, y: -size*1.5, width: size*3, height: size*3)),
                     with: .radialGradient(Gradient(colors: [.white.opacity(0.8), dynamicColor.opacity(0.4), .clear]), center: .zero, startRadius: 0, endRadius: size * 1.5))
        
        context.fill(Path(ellipseIn: CGRect(x: -size*0.8, y: -size*0.8, width: size*1.6, height: size*1.6)),
                     with: .color(.white))
    }
    
    private func drawSatellite(context: inout GraphicsContext, visitor: CelestialVisitor, time: TimeInterval, symbol: GraphicsContext.ResolvedSymbol?, panelsSymbol: GraphicsContext.ResolvedSymbol?) {
        let size = visitor.size
        let satSize = size * 1.8
        let t = time + visitor.randomSeed
        
        // 1. Solar Panels (Symbol) - With dynamic rotation
        if let panelsSymbol = panelsSymbol {
            var panelsCtx = context
            // Double the previous scale factor (1.8 -> 3.6)
            let scaleFactor = (satSize * 3.6) / 150.0 
            panelsCtx.scaleBy(x: scaleFactor, y: scaleFactor)
            
            // Add subtle panel oscillation/rotation for premium feel
            let panelOsc = sin(t * 1.5) * 8.0
            panelsCtx.rotate(by: .degrees(panelOsc))
            
            panelsCtx.draw(panelsSymbol, at: .zero)
        }
        
        // 2. Body (Symbol)
        if let symbol = symbol {
            var symbolCtx = context
            let scaleFactor = satSize / 100.0 // Symbol is 100 wide base
            symbolCtx.scaleBy(x: scaleFactor, y: scaleFactor)
            symbolCtx.draw(symbol, at: .zero)
        }
        
        // 3. Antenna
        let antennaOsc = sin(t * 4.0) * 5.0
        var antennaPath = Path()
        antennaPath.move(to: CGPoint(x: 0, y: -satSize/4))
        antennaPath.addLine(to: CGPoint(x: antennaOsc, y: -satSize/1.5))
        context.stroke(antennaPath, with: .color(Color(hex: 0xB0BEC5)), lineWidth: 1.5)
        
        // 4. LED
        let blinkAlpha = (sin(t * 3.0) + 1.0) / 2.0
        let ledRect = CGRect(x: -2.5, y: -2.5, width: 5, height: 5)
        context.fill(Path(ellipseIn: ledRect), with: .color(visitor.color.opacity(blinkAlpha)))
        
        // 5. Easter Egg: Five-Star Red Flag
        if visitor.isDisplayingFlag {
            // Draw flag above the satellite
            context.drawLayer { ctx in
                ctx.translateBy(x: 0, y: -satSize * 0.8)
                let flagScale = (satSize * 1.5) / 100.0
                ctx.scaleBy(x: flagScale, y: flagScale)
                if let flagSymbol = context.resolveSymbol(id: "flag") {
                    ctx.draw(flagSymbol, at: .zero)
                }
            }
        }
    }
    
    private func drawUFO(context: inout GraphicsContext, visitor: CelestialVisitor, time: TimeInterval, hullSymbol: GraphicsContext.ResolvedSymbol?, domeSymbol: GraphicsContext.ResolvedSymbol?, alienSymbol: GraphicsContext.ResolvedSymbol?) {
        let size = visitor.size
        let ufoWidth = size * 4.5
        let discHeight = ufoWidth * 0.22
        let t = time + visitor.randomSeed
        let rotationAngle = t * 3.5
        
        // 1. Back Lights
        drawUFOLights(context: &context, width: ufoWidth, height: discHeight, rotation: rotationAngle, time: t, color: visitor.color, drawBack: true)
        
        // 2. Beam
        let beamAlpha = 0.12 + (sin(t * 3.0) + 1.0) * 0.04
        var beamPath = Path()
        beamPath.move(to: CGPoint(x: -ufoWidth * 0.15, y: discHeight * 0.05))
        beamPath.addLine(to: CGPoint(x: ufoWidth * 0.15, y: discHeight * 0.05))
        beamPath.addLine(to: CGPoint(x: ufoWidth * 0.4, y: discHeight * 2.2))
        beamPath.addLine(to: CGPoint(x: -ufoWidth * 0.4, y: discHeight * 2.2))
        beamPath.closeSubpath()
        context.fill(beamPath, with: .linearGradient(Gradient(colors: [visitor.color.opacity(beamAlpha), .clear]), startPoint: CGPoint(x: 0, y: discHeight * 0.05), endPoint: CGPoint(x: 0, y: discHeight * 2.2)))
        
        // 3. Body (Hull) - Draw FIRST so it's behind the Alien
        if let hullSymbol = hullSymbol {
            var hullCtx = context
            let scaleFactor = ufoWidth / 100.0
            hullCtx.scaleBy(x: scaleFactor, y: scaleFactor)
            hullCtx.draw(hullSymbol, at: .zero)
        }
        
        // 3b. Cockpit Indentation (Seat)
        let cockpitWidth = ufoWidth * 0.45
        let cockpitHeight = discHeight * 0.6
        let cockpitRect = CGRect(x: -cockpitWidth/2, y: -cockpitHeight - discHeight * 0.1, width: cockpitWidth, height: cockpitHeight)
        context.fill(Path(ellipseIn: cockpitRect), with: .radialGradient(
            Gradient(colors: [Color.black.opacity(0.6), Color.black.opacity(0.1), .clear]),
            center: CGPoint(x: cockpitRect.midX, y: cockpitRect.midY),
            startRadius: 0,
            endRadius: cockpitWidth / 2
        ))

        // 4. Alien (Dynamic Drawing) - Draw ON TOP of the hull
        if visitor.hasPassenger {
            var alienCtx = context
            let targetAlienWidth = ufoWidth * 0.16 // Reduced from 0.20
            let symScale = targetAlienWidth / 100.0
            alienCtx.scaleBy(x: symScale, y: symScale)
            alienCtx.translateBy(x: 0, y: -discHeight * 0.23)
            
            drawDynamicAlien(context: &alienCtx, visitor: visitor, time: t, isInsideUFO: true)
        }
        
        // 5. Dome (Glass) - Draw ON TOP of everything
        if let domeSymbol = domeSymbol {
            var domeCtx = context
            let scaleFactor = ufoWidth / 100.0
            domeCtx.scaleBy(x: scaleFactor, y: scaleFactor)
            
            // IF ET is ejected, shift the dome to show it's "open", hinged on the LEFT
            if !visitor.hasPassenger {
                // Pivot around the left rim of the hull
                domeCtx.translateBy(x: -24, y: 0) // Move inward from the extreme edge
                domeCtx.rotate(by: .degrees(-40)) // Slightly less extreme tilt
                domeCtx.translateBy(x: 24, y: -2) // Re-seat on the rim
            }
            
            domeCtx.draw(domeSymbol, at: .zero)
        }

        // 6. Front Lights
        drawUFOLights(context: &context, width: ufoWidth, height: discHeight, rotation: rotationAngle, time: t, color: visitor.color, drawBack: false)
    }
    
    // MARK: - Dynamic Alien Animation Logic
    
    private func drawDynamicAlien(context: inout GraphicsContext, visitor: CelestialVisitor, time: TimeInterval, isInsideUFO: Bool) {
        let t = time + visitor.randomSeed
        
        // Struggling intensity: High if ejected or being dragged
        let struggleIntensity = (visitor.movementMode == .interacting || !visitor.hasPassenger) ? 1.0 : 0.2
        let struggleSpeed = 8.0
        let wave = sin(t * struggleSpeed) * struggleIntensity
        let kicks = cos(t * struggleSpeed * 0.8) * struggleIntensity * 8.0
        
        // Color palette (Randomized based on Seed)
        let seed = Int(visitor.randomSeed * 137.0) // Scramble seed
        let hue = Double(seed % 360) / 360.0
        
        // Random Alien Colors (Pastel/Organic feel)
        let skinBase = Color(hue: hue, saturation: 0.55, brightness: 0.85)
        let skinShadow = Color(hue: hue, saturation: 0.75, brightness: 0.55)
        let muscleLine = Color(hue: hue, saturation: 0.9, brightness: 0.25).opacity(0.6)
        
        // --- 1. Body & Anatomy (Humanoid Structure) ---
        
        let centerY: Double = 15
        let bodyW: Double = 30 // Wider body
        let bodyH: Double = 34
        
        // Neck (Visible now!)
        let neckW: Double = 10
        let neckLen: Double = 8
        let neckY = centerY - bodyH - neckLen + 4
        context.fill(Path(roundedRect: CGRect(x: -neckW/2, y: neckY, width: neckW, height: neckLen + 4), cornerRadius: 2), with: .color(skinShadow))
        
        // Body (Torso)
        var torsoPath = Path()
        // Shoulders
        let shoulderY = centerY - bodyH + 5
        torsoPath.move(to: CGPoint(x: -bodyW/2 - 2, y: shoulderY))
        torsoPath.addLine(to: CGPoint(x: bodyW/2 + 2, y: shoulderY))
        // Sides to Waist
        torsoPath.addLine(to: CGPoint(x: bodyW/3, y: centerY)) // Waist Right
        torsoPath.addLine(to: CGPoint(x: -bodyW/3, y: centerY)) // Waist Left
        torsoPath.closeSubpath()
        
        context.fill(torsoPath, with: .linearGradient(Gradient(colors: [skinBase, skinShadow]), startPoint: CGPoint(x: 0, y: shoulderY), endPoint: CGPoint(x: 0, y: centerY)))
        context.stroke(torsoPath, with: .color(muscleLine), lineWidth: 1)
        
        // Chest Muscles (Pecs) - Simplified
        var chestPath = Path()
        let chestY = shoulderY + 8
        chestPath.move(to: CGPoint(x: 0, y: shoulderY + 2))
        chestPath.addLine(to: CGPoint(x: 0, y: chestY + 6))
        
        // Pec Arcs
        var leftPec = Path()
        leftPec.move(to: CGPoint(x: 0, y: shoulderY + 2))
        leftPec.addCurve(to: CGPoint(x: -2, y: chestY), control1: CGPoint(x: -8, y: shoulderY + 4), control2: CGPoint(x: -8, y: chestY + 4))
        context.stroke(leftPec, with: .color(muscleLine), lineWidth: 0.8)
        
        var rightPec = Path()
        rightPec.move(to: CGPoint(x: 0, y: shoulderY + 2))
        rightPec.addCurve(to: CGPoint(x: 2, y: chestY), control1: CGPoint(x: 8, y: shoulderY + 4), control2: CGPoint(x: 8, y: chestY + 4))
        context.stroke(rightPec, with: .color(muscleLine), lineWidth: 0.8)

        
        // --- 2. Head (Bulbous Alien Shape) ---
        var headCtx = context
        // Move Head WAY UP
        let headBaseY = neckY - 2 
        headCtx.translateBy(x: 0, y: headBaseY - 26 + (cos(t * 4) * struggleIntensity * 2))
        
        let headW: Double = 62
        let headH: Double = 56
        
        var headPath = Path()
        // Chin (Narrower)
        headPath.move(to: CGPoint(x: 0, y: headH/2)) 
        // Cheeks -> Forehead (Bulbous top)
        headPath.addCurve(to: CGPoint(x: 0, y: -headH * 0.55),
                          control1: CGPoint(x: headW/2, y: headH/2 - 10), // Wide cheek
                          control2: CGPoint(x: headW/2 + 5, y: -headH/2)) // Top wide
        headPath.addCurve(to: CGPoint(x: 0, y: headH/2),
                          control1: CGPoint(x: -headW/2 - 5, y: -headH/2),
                          control2: CGPoint(x: -headW/2, y: headH/2 - 10))
        headPath.closeSubpath()
        
        headCtx.fill(headPath, with: .radialGradient(Gradient(colors: [skinBase, skinShadow]), center: CGPoint(x: -15, y: -20), startRadius: 0, endRadius: 60))
        headCtx.stroke(headPath, with: .color(muscleLine), lineWidth: 1.5)
        
        // Eyes (Huge, Black, Glossy) - Fix Position (Higher up)
        let eyeW: Double = 24
        let eyeH: Double = 17
        let eyeY = 2.0 // Distinctly on the face
        let eyeX = 13.0
        
        for i in [-1.0, 1.0] {
            let ex = i * eyeX
            let ey = eyeY
            
            // Create a rotated context for the eye
            var eyeCtx = headCtx
            eyeCtx.translateBy(x: ex, y: ey)
            // Fix: Rotate so outer corners are HIGHER than inner corners (Classic Alien)
            // i=-1 (Left): Need positive angle (CW) to tilt \ 
            // i=1 (Right): Need negative angle (CCW) to tilt /
            eyeCtx.rotate(by: Angle(degrees: Double(-i) * 25)) 
            eyeCtx.translateBy(x: -ex, y: -ey)
            
            // Shadow/Socket (drawn on eyeCtx)
            let socketRect = CGRect(x: ex - eyeW/2 - 1, y: ey - eyeH/2 - 1, width: eyeW + 2, height: eyeH + 2)
            let socketPath = Path(ellipseIn: socketRect)
            eyeCtx.stroke(socketPath, with: .color(muscleLine), lineWidth: 0.5)

            // Eye Ball
            let eyeRect = CGRect(x: ex - eyeW/2, y: ey - eyeH/2, width: eyeW, height: eyeH)
            let eyePath = Path(ellipseIn: eyeRect)
            eyeCtx.fill(eyePath, with: .color(.black))
            
            // Highlights
            // Big reflection
            let bigReflectRect = CGRect(x: ex - 5, y: ey - 5, width: 7, height: 4)
            eyeCtx.fill(Path(ellipseIn: bigReflectRect), with: .color(.white))
            
            // Small reflection
            let smallReflectRect = CGRect(x: ex + 4, y: ey + 2, width: 2, height: 2)
            eyeCtx.fill(Path(ellipseIn: smallReflectRect), with: .color(.white.opacity(0.6)))
        }
        
        // Sutures
        var stitchPath = Path()
        stitchPath.move(to: CGPoint(x: 0, y: -headH/2 + 5))
        stitchPath.addLine(to: CGPoint(x: 0, y: -headH/2 + 15))
        headCtx.stroke(stitchPath, with: .color(muscleLine), style: StrokeStyle(lineWidth: 0.5, dash: [2]))

        // Nose (Tiny dots)
        headCtx.fill(Path(ellipseIn: CGRect(x: -1.5, y: eyeY + 10, width: 1, height: 1)), with: .color(muscleLine))
        headCtx.fill(Path(ellipseIn: CGRect(x: 1.5, y: eyeY + 10, width: 1, height: 1)), with: .color(muscleLine))
        
        // Mouth (Small slit/curve)
        var mouthPath = Path()
        mouthPath.move(to: CGPoint(x: -3, y: eyeY + 16))
        mouthPath.addQuadCurve(to: CGPoint(x: 3, y: eyeY + 16), control: CGPoint(x: 0, y: eyeY + 17.5)) 
        headCtx.stroke(mouthPath, with: .color(muscleLine), style: StrokeStyle(lineWidth: 0.8, lineCap: .round))
        
        
        // --- 3. Limbs (Articulated with Muscles) ---
        
        // --- 3. Limbs (Articulated with Muscles) ---
        // Pass local colors and context to helpers
        drawDynamicArms(context: &context, t: t, struggleIntensity: struggleIntensity, shoulderY: shoulderY, bodyW: bodyW, wave: wave, skinBase: skinBase, skinShadow: skinShadow)
        
        drawDynamicLegs(context: &context, t: t, struggleIntensity: struggleIntensity, centerY: centerY, kicks: kicks, skinBase: skinBase, skinShadow: skinShadow)
    }
    
    // Extracted Alien Drawing Logic (Deprecated/Renamed)
    private func drawAlien(context: inout GraphicsContext) {
        // Keeping for symbol generation or future use
    }
    
    private func drawUFOLights(context: inout GraphicsContext, width: Double, height: Double, rotation: Double, time: Double, color: Color, drawBack: Bool) {
        let lightCount = 8
        for i in 0..<lightCount {
            let baseAngle = Double(i) * 2.0 * .pi / Double(lightCount)
            let totalAngle = baseAngle + rotation
            let z = sin(totalAngle)
            
            let shouldDraw = drawBack ? (z <= 0) : (z > 0)
            
            if shouldDraw {
                let lx = cos(totalAngle) * (width * 0.48)
                let ly = sin(totalAngle) * (height * 0.5)
                let depthScale = 0.7 + (z + 1.0) * 0.35
                let depthAlpha = 0.3 + (z + 1.0) * 0.4
                let lightPulse = (sin(time / 0.5 + Double(i) * 1.2) + 1.0) / 2.0
                
                // Individual light color shift
                let r = (color.components.red + sin(Double(i)) * 0.05).clamped(to: 0...1)
                let g = (color.components.green + cos(Double(i)) * 0.05).clamped(to: 0...1)
                let b = (color.components.blue + sin(Double(i) * 1.5) * 0.05).clamped(to: 0...1)
                let indColor = Color(red: r, green: g, blue: b)
                
                let lightRadius = 3.2 * depthScale
                let glowRadius = 9.0 * depthScale
                
                if !drawBack {
                    let glowRect = CGRect(x: lx - glowRadius, y: ly - glowRadius, width: glowRadius*2, height: glowRadius*2)
                    context.fill(Path(ellipseIn: glowRect),
                                 with: .radialGradient(Gradient(colors: [indColor.opacity(0.45 * depthAlpha * lightPulse), .clear]), center: CGPoint(x: glowRect.midX, y: glowRect.midY), startRadius: 0, endRadius: glowRadius))
                }
                
                let coreRect = CGRect(x: lx - lightRadius, y: ly - lightRadius, width: lightRadius*2, height: lightRadius*2)
                context.fill(Path(ellipseIn: coreRect),
                             with: .color(indColor.opacity((0.5 + 0.5 * lightPulse) * depthAlpha)))
            }
        }
    }
    
    // --- Extracted Helpers (Compiler Optimization) ---
    
    // --- Extracted Helpers (Compiler Optimization) ---
    
    private func drawDynamicArms(context: inout GraphicsContext, t: Double, struggleIntensity: Double, shoulderY: Double, bodyW: Double, wave: Double, skinBase: Color, skinShadow: Color) {
        // Convert to CGFloat for strict type safety
        let t = CGFloat(t)
        let intensity = CGFloat(struggleIntensity)
        let shoulderY = CGFloat(shoulderY)
        let bodyW = CGFloat(bodyW)
        let wave = CGFloat(wave)
        
        let armLenLower: CGFloat = 12.0
        let armLenUpper: CGFloat = 10.0
        
        // Helper inline
        func lerp(_ a: CGPoint, _ b: CGPoint, _ factor: CGFloat) -> CGPoint {
            return CGPoint(x: a.x + (b.x - a.x) * factor, y: a.y + (b.y - a.y) * factor)
        }
        
        // Target Positions
        // 1. Relaxed (Down)
        let lShoulder = CGPoint(x: -bodyW/2 + 2, y: shoulderY + 2)
        let rShoulder = CGPoint(x: bodyW/2 - 2, y: shoulderY + 2)
        
        let lElbowDown = CGPoint(x: lShoulder.x - 8 - wave * 4, y: shoulderY + armLenUpper - wave * 8)
        let lHandDown = CGPoint(x: lElbowDown.x - 4 - wave * 4, y: lElbowDown.y + armLenLower)
        
        let rElbowDown = CGPoint(x: rShoulder.x + 8 + wave * 4, y: shoulderY + armLenUpper - wave * 8)
        let rHandDown = CGPoint(x: rElbowDown.x + 4 + wave * 4, y: rElbowDown.y + armLenLower)
        
        // 2. Struggling (Up/Panic)
        let lElbowUp = CGPoint(x: lShoulder.x - 12, y: shoulderY - 5)
        let lHandUp = CGPoint(x: lShoulder.x - 16, y: shoulderY - 18)
        
        let rElbowUp = CGPoint(x: rShoulder.x + 12, y: shoulderY - 5)
        let rHandUp = CGPoint(x: rShoulder.x + 16, y: shoulderY - 18)
        
        // Interpolate
        let lElbow = lerp(lElbowDown, lElbowUp, intensity)
        let lHand = lerp(lHandDown, lHandUp, intensity)
        var rElbow = lerp(rElbowDown, rElbowUp, intensity)
        var rHand = lerp(rHandDown, rHandUp, intensity)
        
        // 3. Pointing Logic
        let isPointingTime = (Int(t * 1.5) % 5 == 0)
        let pointFactor: CGFloat = (intensity > 0.2 && isPointingTime) ? 1.0 : 0.0
        
        let rElbowPoint = CGPoint(x: rShoulder.x + 5, y: shoulderY + 5)
        let rHandPoint = CGPoint(x: 5, y: shoulderY - 2)
        
        if pointFactor > 0.5 {
            rElbow = lerp(rElbow, rElbowPoint, 0.8)
            rHand = lerp(rHand, rHandPoint, 0.8)
        }
        
        // Draw Limbs
        drawSegmentedLimb(context: &context, start: lShoulder, joint: lElbow, end: lHand, width: 4.5, skinBase: skinBase, skinShadow: skinShadow)
        drawSegmentedLimb(context: &context, start: rShoulder, joint: rElbow, end: rHand, width: 4.5, skinBase: skinBase, skinShadow: skinShadow)

        // Hands & Fingers
        // Left
        context.fill(Path(ellipseIn: CGRect(x: lHand.x - 3, y: lHand.y - 3, width: 6, height: 6)), with: .color(skinBase))
        let lIsUp = lHand.y < shoulderY
        let lYMult: CGFloat = lIsUp ? -1.0 : 1.0
        
        var lP1 = Path(); lP1.move(to: lHand); lP1.addLine(to: CGPoint(x: lHand.x - 4, y: lHand.y + 6 * lYMult))
        context.stroke(lP1, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
        var lP2 = Path(); lP2.move(to: lHand); lP2.addLine(to: CGPoint(x: lHand.x + 0, y: lHand.y + 7 * lYMult + (lIsUp ? -2.0 : 0.0)))
        context.stroke(lP2, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
        var lP3 = Path(); lP3.move(to: lHand); lP3.addLine(to: CGPoint(x: lHand.x + 4, y: lHand.y + 6 * lYMult))
        context.stroke(lP3, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
        
        // Right
        context.fill(Path(ellipseIn: CGRect(x: rHand.x - 3, y: rHand.y - 3, width: 6, height: 6)), with: .color(skinBase))
        if pointFactor > 0.5 {
            // Pointing
            context.fill(Path(ellipseIn: CGRect(x: rHand.x + 2, y: rHand.y + 2, width: 3, height: 3)), with: .color(skinBase))
            context.fill(Path(ellipseIn: CGRect(x: rHand.x - 2, y: rHand.y + 2, width: 3, height: 3)), with: .color(skinBase))
            let tipRect = CGRect(x: rHand.x - 3, y: rHand.y - 3, width: 6, height: 6)
            context.fill(Path(ellipseIn: tipRect), with: .color(skinBase))
            context.fill(Path(ellipseIn: tipRect.insetBy(dx: -4, dy: -4)), with: .color(.yellow.opacity(0.7)))
            context.fill(Path(ellipseIn: tipRect.insetBy(dx: 1, dy: 1)), with: .color(.white))
            
            // SCREEN CRACK EFFECT
            // Draw jagged lines radiating from the finger
            drawScreenCrack(context: &context, center: rHand, seed: t)
        } else {
            // Normal
            let rIsUp = rHand.y < shoulderY
            let rYMult: CGFloat = rIsUp ? -1.0 : 1.0
            var rP1 = Path(); rP1.move(to: rHand); rP1.addLine(to: CGPoint(x: rHand.x - 4, y: rHand.y + 6 * rYMult))
            context.stroke(rP1, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
            var rP2 = Path(); rP2.move(to: rHand); rP2.addLine(to: CGPoint(x: rHand.x + 0, y: rHand.y + 7 * rYMult + (rIsUp ? -2.0 : 0.0)))
            context.stroke(rP2, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
            var rP3 = Path(); rP3.move(to: rHand); rP3.addLine(to: CGPoint(x: rHand.x + 4, y: rHand.y + 6 * rYMult))
            context.stroke(rP3, with: .color(skinBase), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
             if Int.random(in: 0...40) == 0 {
                  context.fill(Path(ellipseIn: CGRect(x: rHand.x + 4 - 3, y: rHand.y + 6 * rYMult - 3, width: 6, height: 6)), with: .color(.yellow.opacity(0.6)))
            }
        }
    }
    
    private func drawDynamicLegs(context: inout GraphicsContext, t: Double, struggleIntensity: Double, centerY: Double, kicks: Double, skinBase: Color, skinShadow: Color) {
        let t = CGFloat(t)
        let intensity = CGFloat(struggleIntensity)
        let centerY = CGFloat(centerY)
        let kicks = CGFloat(kicks)
        
        let hipY = centerY
        let legLenUpper: CGFloat = 12.0
        let legLenLower: CGFloat = 10.0
        let legSwing = CGFloat(sin(Double(t) * 12.0) * 6.0) * intensity
        
        let lHip = CGPoint(x: -7, y: hipY)
        let lKnee = CGPoint(x: lHip.x - 3 + legSwing, y: hipY + legLenUpper + kicks)
        let lFootPos = CGPoint(x: lKnee.x - 2 + legSwing, y: lKnee.y + legLenLower)
        drawSegmentedLimb(context: &context, start: lHip, joint: lKnee, end: lFootPos, width: 5, skinBase: skinBase, skinShadow: skinShadow)
        
        let rHip = CGPoint(x: 7, y: hipY)
        let rKnee = CGPoint(x: rHip.x + 3 - legSwing, y: hipY + legLenUpper - kicks)
        let rFootPos = CGPoint(x: rKnee.x + 2 - legSwing, y: rKnee.y + legLenLower)
        drawSegmentedLimb(context: &context, start: rHip, joint: rKnee, end: rFootPos, width: 5, skinBase: skinBase, skinShadow: skinShadow)
        
        // Flat Feet
        let footW: CGFloat = 14.0
        let footH: CGFloat = 5.0
        context.fill(Path(ellipseIn: CGRect(x: lFootPos.x - footW/2 - 2, y: lFootPos.y - 2, width: footW, height: footH)), with: .color(skinBase))
        context.fill(Path(ellipseIn: CGRect(x: rFootPos.x - footW/2 + 2, y: rFootPos.y - 2, width: footW, height: footH)), with: .color(skinBase))
    }
    
    private func drawSegmentedLimb(context: inout GraphicsContext, start: CGPoint, joint: CGPoint, end: CGPoint, width: Double, skinBase: Color, skinShadow: Color) {
        let width = CGFloat(width)
        var upperPath = Path()
        upperPath.move(to: start)
        upperPath.addLine(to: joint)
        context.stroke(upperPath, with: .color(skinShadow), style: StrokeStyle(lineWidth: width, lineCap: .round))
        
        var lowerPath = Path()
        lowerPath.move(to: joint)
        lowerPath.addLine(to: end)
        context.stroke(lowerPath, with: .color(skinBase), style: StrokeStyle(lineWidth: width - 1.0, lineCap: .round))
        
        context.fill(Path(ellipseIn: CGRect(x: joint.x - width/2, y: joint.y - width/2, width: width, height: width)), with: .color(skinBase))
    }
    
    private func drawScreenCrack(context: inout GraphicsContext, center: CGPoint, seed: CGFloat) {
        let crackRadius: CGFloat = 35.0
        let branches = 6
        
        // Use a stable random based on seed (time) but quantized to flicker
        let stableSeed = Int(seed * 1.5) 
        
        // Helper for pseudo random
        func pseudoRandom(_ input: Int) -> CGFloat {
            return CGFloat((Double(input * 2654435761 % 4294967296) / 4294967296.0))
        }
        
        // Context setup for "Tech/Holo" look
        let cyanColor = Color(red: 0.0, green: 0.9, blue: 1.0)
        let deepBlueColor = Color(red: 0.0, green: 0.2, blue: 0.8)
        let whiteHot = Color.white
        
        // 1. Digital Ripple (Shockwave)
        // Draw concentric thin rings that fade out
        let rippleCount = 3
        for r in 1...rippleCount {
            let rippleRad = crackRadius * (CGFloat(r) / CGFloat(rippleCount)) * (0.8 + 0.2 * pseudoRandom(stableSeed + r))
            let opacity = 0.6 * (1.0 - CGFloat(r)/CGFloat(rippleCount))
            context.stroke(Path(ellipseIn: CGRect(x: center.x - rippleRad, y: center.y - rippleRad, width: rippleRad*2, height: rippleRad*2)),
                           with: .color(cyanColor.opacity(opacity)),
                           lineWidth: 0.8)
        }
        
        // 2. Circuit/Crystal Fracture Lines
        for i in 0..<branches {
            let angleBase = (CGFloat(i) / CGFloat(branches)) * 2 * .pi
            // Use quantized angle steps for "digital" feel (e.g., 45-degree increments roughly)
            let angleVar = (pseudoRandom(stableSeed + i * 10) - 0.5) * 0.5
            let angle = angleBase + angleVar
            
            var path = Path()
            path.move(to: center)
            
            var currentPos = center
            var currentDist: CGFloat = 0.0
            let maxDist = crackRadius * (0.8 + 0.4 * pseudoRandom(stableSeed + i * 20))
            
            // Draw angular path
            let steps = 4
            for j in 0..<steps {
                if currentDist >= maxDist { break }
                
                let stepLen = maxDist / CGFloat(steps)
                // Angular jitter: prefer straight lines or 45 degree turns
                let turn = (Int(pseudoRandom(stableSeed + i * 100 + j) * 4.0) - 2) // -2, -1, 0, 1
                let turnAngle = CGFloat(turn) * (CGFloat.pi / 4.0)
                let finalAngle = angle + turnAngle
                
                let nextPos = CGPoint(
                    x: currentPos.x + cos(finalAngle) * stepLen,
                    y: currentPos.y + sin(finalAngle) * stepLen
                )
                path.addLine(to: nextPos)
                currentPos = nextPos
                currentDist += stepLen
            }
            
            // Draw main crack line
            context.stroke(path, with: .color(whiteHot.opacity(0.9)), lineWidth: 1.2)
            context.stroke(path, with: .color(cyanColor.opacity(0.6)), lineWidth: 2.5) // Glow
            
            // 3. Glitch Particles at the end of some branches
            if pseudoRandom(stableSeed + i * 99) > 0.4 {
                let pSize = 2.0 + 2.0 * pseudoRandom(stableSeed + i * 77)
                let pRect = CGRect(x: currentPos.x - pSize/2, y: currentPos.y - pSize/2, width: pSize, height: pSize)
                context.fill(Path(pRect), with: .color(cyanColor.opacity(0.8)))
            }
        }
        
        // 4. Central Impact flash
        context.fill(Path(ellipseIn: CGRect(x: center.x - 4, y: center.y - 4, width: 8, height: 8)), with: .color(whiteHot))
        context.fill(Path(ellipseIn: CGRect(x: center.x - 8, y: center.y - 8, width: 16, height: 16)), with: .color(deepBlueColor.opacity(0.3)))
    }
}

// Helpers (Keep at bottom)
extension Color {
    var components: (red: Double, green: Double, blue: Double, alpha: Double) {
        let uiColor = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Double(r), Double(g), Double(b), Double(a))
    }
}
extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        return min(max(self, range.lowerBound), range.upperBound)
    }
}
