import SwiftUI
import UIKit
import Combine

/// Android parity: Particle system for merge burst and win fireworks
struct Particle: Identifiable {
    let id = UUID()
    var x: CGFloat
    var y: CGFloat
    var vx: CGFloat
    var vy: CGFloat
    var color: Color
    var alpha: CGFloat
    var life: CGFloat
    let decay: CGFloat
    let size: CGFloat
}

struct FloatingTextItem: Identifiable {
    let id = UUID()
    var x: CGFloat
    var y: CGFloat
    let text: String
    let color: Color
    var alpha: CGFloat
    var scale: CGFloat // Added for pop effect
    var life: Int
    let lifeMax: Int // Added to calculate progress
}

class ParticleSystem: ObservableObject {
    @Published private(set) var particles: [Particle] = []
    @Published private(set) var floatingTexts: [FloatingTextItem] = []
    private var particlesBacking: [Particle] = []
    private var floatingTextsBacking: [FloatingTextItem] = []
    private var displayLink: CADisplayLink?
    private var runLoop: RunLoop?
    private var tickCount = 0
    
    var isFireworksMode = false {
        didSet {
            if isFireworksMode {
                startUpdateLoopIfNeeded()
                fireworksBurstsLeft = 2
                fireworksTimer = 20
            }
        }
    }
    private var fireworksTimer = 0
    private var fireworksBurstsLeft = 0
    private var width: CGFloat = 400
    private var height: CGFloat = 400
    
    func setDimensions(width: CGFloat, height: CGFloat) {
        self.width = width
        self.height = height
    }
    
    func clear() {
        stopUpdateLoop()
        particles = []
        floatingTexts = []
        particlesBacking = []
        floatingTextsBacking = []
        isFireworksMode = false
    }
    
    private func startUpdateLoopIfNeeded() {
        guard displayLink == nil, isActive else { return }
        let link = CADisplayLink(target: self, selector: #selector(tick))
        link.add(to: .main, forMode: .common)
        displayLink = link
        runLoop = .main
    }
    
    private func stopUpdateLoop() {
        displayLink?.invalidate()
        displayLink = nil
    }
    
    @objc private func tick() {
        update()
        tickCount += 1
        if !isActive { stopUpdateLoop() }
    }
    
    private let MAX_PARTICLES = 80

    func emit(x: CGFloat, y: CGFloat, count: Int, colors: [Color] = [.red, .yellow, .blue, .green]) {
        var next = particlesBacking
        let addCount = min(count, MAX_PARTICLES - next.count)
        guard addCount > 0 else { return }
        for _ in 0..<addCount {
            let angle = CGFloat.random(in: 0...(2 * .pi))
            let speed = CGFloat.random(in: 2...8)
            let vx = cos(angle) * speed
            let vy = sin(angle) * speed
            let color = colors.randomElement() ?? .red
            let decay = CGFloat.random(in: 0.03...0.06)
            let size = CGFloat.random(in: 2...5)
            next.append(Particle(
                x: x, y: y, vx: vx, vy: vy,
                color: color, alpha: 1, life: 1, decay: decay, size: size
            ))
        }
        particlesBacking = next
        particles = next
        startUpdateLoopIfNeeded()
    }
    
    func addFloatingText(x: CGFloat, y: CGFloat, text: String, color: Color) {
        // Start small, pop up
        let item = FloatingTextItem(x: x, y: y, text: text, color: color, alpha: 1, scale: 0.5, life: 60, lifeMax: 60)
        floatingTextsBacking.append(item)
        floatingTexts = floatingTextsBacking
        startUpdateLoopIfNeeded()
    }
    
    func update() {
        if isFireworksMode {
            fireworksTimer += 1
            if fireworksTimer > 20 {
                fireworksTimer = 0
                if fireworksBurstsLeft > 0 {
                    let x = CGFloat.random(in: 0...width)
                    let y = CGFloat.random(in: 0...(height * 0.5))
                    emit(x: x, y: y, count: 14)
                    fireworksBurstsLeft -= 1
                } else {
                    isFireworksMode = false
                }
            }
        }
        
        particlesBacking = particlesBacking.compactMap { p -> Particle? in
            var next = p
            next.x += next.vx
            next.y += next.vy
            next.vy += 0.5
            next.life -= next.decay
            next.alpha = max(0, min(1, next.life))
            return next.life > 0 ? next : nil
        }
        
        floatingTextsBacking = floatingTextsBacking.compactMap { t -> FloatingTextItem? in
            var next = t
            next.life -= 1
            
            // Bounce/Pop Animation
            let progress = 1.0 - (CGFloat(next.life) / CGFloat(next.lifeMax)) // 0 -> 1
            
            // 1. Movement: Float up slowly, accelerating slightly
            next.y -= (0.5 + progress * 2.0)
            
            // 2. Scale: Elastic pop (Overshoot then settle)
            // 0 -> 0.2: Rapid scale up to 1.5
            // 0.2 -> 0.4: Scale down to 1.0
            // 0.4 -> 1.0: Stay at 1.0 (or slight pulse)
            if progress < 0.2 {
                let p = progress / 0.2
                next.scale = 0.5 + 1.0 * p // 0.5 -> 1.5
            } else if progress < 0.4 {
                let p = (progress - 0.2) / 0.2
                next.scale = 1.5 - 0.5 * p // 1.5 -> 1.0
            } else {
                next.scale = 1.0
            }
            
            // 3. Alpha: Fade out in last 20%
            if progress > 0.8 {
                next.alpha = (1.0 - progress) / 0.2
            } else {
                next.alpha = 1.0
            }
            
            return next.life > 0 ? next : nil
        }
        
        particles = particlesBacking
        floatingTexts = floatingTextsBacking
        self.objectWillChange.send()
    }
    
    var isActive: Bool {
        !particlesBacking.isEmpty || isFireworksMode || !floatingTextsBacking.isEmpty
    }
}
