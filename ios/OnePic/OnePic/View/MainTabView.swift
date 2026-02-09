import SwiftUI
import Combine

/// 拼图全屏时隐藏底部 TabBar，由 GameBoardView onAppear/onDisappear 控制
class TabBarVisibility: ObservableObject {
    @Published var hideTabBar = false
}

struct MainTabView: View {
    @State private var selection = 0
    @StateObject private var visitorManager = CelestialVisitorManager()
    @StateObject private var tabBarVisibility = TabBarVisibility()
    @ObservedObject private var progressManager = LevelProgressManager.shared
    @State private var targetLevelId: String? = nil // For locate feature
    
    var body: some View {
        ZStack {
            // 全屏黑色背景，避免底部安全区露出白色
            Color.black.ignoresSafeArea()
            // Invisible Updater to drive the animation logic centrally
            TimelineView(.animation) { timelineContext in
                let date = timelineContext.date.timeIntervalSinceReferenceDate
                Color.clear
                    .onChange(of: date) { oldDate, newDate in
                        visitorManager.update(time: newDate, deltaTime: newDate - oldDate)
                    }
            }
            .allowsHitTesting(false)
            
            ZStack(alignment: .bottom) {
                // Tab content：底层先铺满黑色，避免透明 TabBar 下透出导航/系统白底
                Group {
                    switch selection {
                    case 0: HomeView(visitorManager: visitorManager, targetLevelId: $targetLevelId)
                    case 1: GalaxyView(visitorManager: visitorManager, onLocateLevel: { levelId in
                        targetLevelId = levelId
                        selection = 0
                    })
                    case 2: CheckInView(visitorManager: visitorManager)
                    case 3: MoreView(visitorManager: visitorManager)
                    default: HomeView(visitorManager: visitorManager, targetLevelId: $targetLevelId)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black.ignoresSafeArea())
                .background(Color.black.ignoresSafeArea())
                .environmentObject(tabBarVisibility)
                
                // Global Interaction Overlay (Covering all tabs, below TabBar)
                CelestialVisitorInteractionOverlay(manager: visitorManager).ignoresSafeArea()
                
                if !tabBarVisibility.hideTabBar {
                    MainTabBar(selection: $selection)
                        .id("\(progressManager.currentLanguage)_\(progressManager.checkInVersion)")
                }
            }
        }
    }
}

private struct ComingSoonPlaceholder: View {
    let title: String
    @ObservedObject var visitorManager: CelestialVisitorManager
    
    var body: some View {
        ZStack {
            SharedGalaxyBackground(atmosphereTheme: "cosmos", visitorManager: visitorManager)
            
            VStack {
                Spacer()
                Text("\(title) (\(TRANS.get("chapter_coming_soon", "Coming Soon")))")
                    .foregroundColor(.white.opacity(0.5))
                Spacer()
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private struct MainTabBar: View {
    @Binding var selection: Int
    
    var body: some View {
        HStack(spacing: 0) {
            TabBarItem(selection: $selection, tag: 0, icon: "house.fill", labelKey: "nav_home", fallback: "Home")
            TabBarItem(selection: $selection, tag: 1, icon: "star.fill", labelKey: "nav_galaxy", fallback: "Ascension")
            TabBarItem(selection: $selection, tag: 2, icon: "checkmark.circle.fill", labelKey: "nav_checkin", fallback: "Check-in", showBadge: LevelProgressManager.shared.shouldShowCheckInRedDot())
            TabBarItem(selection: $selection, tag: 3, icon: "person.fill", labelKey: "nav_personal_center", fallback: "Profile")
        }
        .padding(.horizontal, 8)
        .padding(.bottom, 20)
        .padding(.top, 8)
        .background(
            // 底部安全区用不透明黑色填满，彻底盖住系统白底
            Color.black.ignoresSafeArea(edges: .bottom)
        )
        .background(
            // Tab 栏本体保持半透明深色
            Color.black.opacity(0.25)
        )
    }
}

private struct TabBarItem: View {
    @Binding var selection: Int
    let tag: Int
    let icon: String
    let labelKey: String
    let fallback: String
    var showBadge: Bool = false
    
    var body: some View {
        Button(action: { selection = tag }) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .medium))
                    
                    if showBadge {
                        Circle()
                            .fill(Color.red)
                            .frame(width: 7, height: 7)
                            .overlay(Circle().stroke(Color.white, lineWidth: 0.5)) // 使用白色边框减少锯齿感
                            .offset(x: 10, y: -4) // 手动位移以获得更规则的视觉位置
                            .transition(.scale.combined(with: .opacity))
                    }
                }
                .animation(.spring(response: 0.3, dampingFraction: 0.7), value: showBadge)
                .foregroundColor(selection == tag ? .white : .white.opacity(0.5))
                
                Text(TRANS.get(labelKey, fallback))
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(selection == tag ? .white : .white.opacity(0.5))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }
}


