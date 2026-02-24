import SwiftUI

struct SharedGalaxyBackground: View {
    let atmosphereTheme: String
    @ObservedObject var visitorManager: CelestialVisitorManager
    var showsParticles: Bool = true
    
    var body: some View {
        ZStack {
            if AppRuntimePolicy.supportsRealtimeAnimationDriver {
                if showsParticles {
                    ParticleBackgroundView(theme: atmosphereTheme)
                }
                
                TimelineView(.animation) { timelineContext in
                    let date = timelineContext.date.timeIntervalSinceReferenceDate
                    
                    GeometryReader { _ in
                        ZStack {
                            CelestialVisitorView(manager: visitorManager, layer: .background, currentTime: date)
                        }
                    }
                }
            } else {
                // iOS 15 稳定模式：使用静态背景，避免 Timeline/Canvas 引起界面抖动
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(hex: 0x1A0B2E),
                        Color(hex: 0x0A1F30),
                        Color(hex: 0x050510)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .ignoresSafeArea()
    }
}
