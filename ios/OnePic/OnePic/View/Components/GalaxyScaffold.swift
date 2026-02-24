import SwiftUI

struct GalaxyScaffold<Background: View, Content: View>: View {
    let atmosphereTheme: String
    let visitorManager: CelestialVisitorManager?
    let showsParticles: Bool
    let background: () -> Background
    let content: () -> Content
    @State private var lastTimelineDate: TimeInterval? = nil
    
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
                    .onChangeCompat(of: date) { newDate in
                        let delta = (lastTimelineDate.map { newDate - $0 }) ?? 0
                        visitorManager.update(time: newDate, deltaTime: delta)
                        lastTimelineDate = newDate
                    }
                }
            } else {
                content()
            }
        }
        .ignoresSafeArea()
    }
}
