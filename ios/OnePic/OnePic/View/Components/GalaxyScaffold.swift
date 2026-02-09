import SwiftUI

struct GalaxyScaffold<Background: View, Content: View>: View {
    let atmosphereTheme: String
    let visitorManager: CelestialVisitorManager?
    let showsParticles: Bool
    let background: () -> Background
    let content: () -> Content
    
    init(
        atmosphereTheme: String,
        visitorManager: CelestialVisitorManager? = nil,
        showsParticles: Bool = true,
        @ViewBuilder background: @escaping () -> Background = { EmptyView() },
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.atmosphereTheme = atmosphereTheme
        self.visitorManager = visitorManager
        self.showsParticles = showsParticles
        self.background = background
        self.content = content
    }
    
    var body: some View {
        ZStack {
            background()
            
            if showsParticles {
                ParticleBackgroundView(theme: atmosphereTheme)
                    .drawingGroup() // 性能优化：减少重绘开销
            }
            
            if let visitorManager {
                TimelineView(.animation) { timelineContext in
                    let date = timelineContext.date.timeIntervalSinceReferenceDate
                    
                    ZStack {
                        CelestialVisitorView(manager: visitorManager, layer: .background, currentTime: date)
                        content()
                        CelestialVisitorView(manager: visitorManager, layer: .foreground, currentTime: date)
                            .allowsHitTesting(false)
                    }
                    .onChange(of: date) { oldDate, newDate in
                        visitorManager.update(time: newDate, deltaTime: newDate - oldDate)
                    }
                }
            } else {
                content()
            }
        }
        .ignoresSafeArea()
    }
}
