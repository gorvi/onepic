import SwiftUI

/// Android parity: Renders particle system (ParticleSystem drives its own update loop)
struct ParticleOverlayView: View {
    @ObservedObject var particleSystem: ParticleSystem
    
    var body: some View {
        ZStack {
            // Layer 1: Particles (Canvas is efficient for many small dots)
            Canvas { ctx, size in
                particleSystem.setDimensions(width: size.width, height: size.height)
                
                for p in particleSystem.particles {
                    ctx.opacity = p.alpha
                    ctx.fill(
                        Path(ellipseIn: CGRect(x: p.x - p.size, y: p.y - p.size, width: p.size * 2, height: p.size * 2)),
                        with: .color(p.color)
                    )
                }
            }
            .allowsHitTesting(false)
            
            // Layer 2: Floating Texts (Native Views for rich styling: Gradient, Shadow, Stroke)
            // Since there are very few floating texts at once, extensive View modifiers are fine.
            ForEach(particleSystem.floatingTexts) { item in
                Text(item.text)
                    .font(.system(size: 32, weight: .heavy)) // Increased size
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color(hex: 0xFFD700), Color(hex: 0xFFA000)], // Gold Gradient
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .shadow(color: Color.black.opacity(0.5), radius: 2, x: 1, y: 1) // Hard shadow
                    .shadow(color: Color(hex: 0xFFD700).opacity(0.6), radius: 8, x: 0, y: 0) // Glow
                    .scaleEffect(item.scale)
                    .opacity(item.alpha)
                    .position(x: item.x, y: item.y - 20) // Slightly offset to center visually above finger
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .allowsHitTesting(false)
        .drawingGroup()
    }
}
