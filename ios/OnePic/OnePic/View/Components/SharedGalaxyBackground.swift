import SwiftUI

struct SharedGalaxyBackground: View {
    let atmosphereTheme: String
    @ObservedObject var visitorManager: CelestialVisitorManager
    var showsParticles: Bool = true
    
    var body: some View {
        ZStack {
            if showsParticles {
                ParticleBackgroundView(theme: atmosphereTheme)
                    .drawingGroup() // 性能优化
            }
            
            TimelineView(.animation) { timelineContext in
                let date = timelineContext.date.timeIntervalSinceReferenceDate
                
                GeometryReader { geo in
                    ZStack {
                        CelestialVisitorView(manager: visitorManager, layer: .background, currentTime: date)
                        // Foreground layer is usually handled by the parent overlaying content, 
                        // but for background purposes we only need the background layer here?
                        // Wait, GalaxyScaffold handles both background and foreground visitors.
                        // To keep it simple, this component only renders the BACKGROUND layers (Particles + Back Visitors).
                        // Foreground Visitors should be added via .overlay or ZStack (top) if needed.
                    }
                }
            }
        }
        .ignoresSafeArea()
    }
}
