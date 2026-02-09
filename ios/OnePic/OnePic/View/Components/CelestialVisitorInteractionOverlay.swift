import SwiftUI

struct CelestialVisitorInteractionOverlay: View {
    @ObservedObject var manager: CelestialVisitorManager
    // GestureState tracks the ACTIVE gesture identity. Resets to nil automatically when ended/cancelled.
    @GestureState private var draggingVisitorId: UUID? = nil
    
    var body: some View {
        refinedBody
            .onChange(of: draggingVisitorId) { oldId, newId in
                if newId == nil {
                    manager.forceRelease()
                }
            }
    }
}


extension CelestialVisitorInteractionOverlay {
    // Specialized Refined Overlay Body
    var refinedBody: some View {
        GeometryReader { geo in
            ZStack {
                ForEach(manager.activeVisitors) { visitor in
                     // Hit Target
                     Circle()
                        .fill(Color.white.opacity(0.001))
                        .frame(width: max(visitor.size * visitor.currentScale * 6.0, 80), height: max(visitor.size * visitor.currentScale * 6.0, 80))
                        .position(x: visitor.currentX * geo.size.width, y: visitor.currentY * geo.size.height)
                        .gesture(
                            DragGesture(minimumDistance: 0, coordinateSpace: .named("GalaxyInteractionSpace"))
                                .onChanged { value in
                                    // Handle Start manually if needed or rely on Manager state
                                    // Start:
                                    if visitor.movementMode != .interacting {
                                        // First frame of interaction essentially
                                        manager.handleDragStart(location: value.startLocation, viewSize: geo.size)
                                    }
                                    
                                    // Update
                                    manager.handleDragUpdate(location: value.location, viewSize: geo.size)
                                }
                                .onEnded { value in
                                    manager.handleDragEnd(velocity: value.velocity, viewSize: geo.size)
                                }
                        )
                }
            }
        }
        .coordinateSpace(name: "GalaxyInteractionSpace")
    }
}
