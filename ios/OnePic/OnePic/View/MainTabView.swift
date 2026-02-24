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
    @State private var targetLevelId: String? = nil // For locate feature
    @State private var tabBarLanguage: String = LevelProgressManager.shared.currentLanguage
    @State private var tabBarCheckInVersion: Int = LevelProgressManager.shared.checkInVersion
    
    var body: some View {
        ZStack {
            // 全屏黑色背景，避免底部安全区露出白色
            Color.black.ignoresSafeArea()
            // iOS 15 稳定模式：禁用全局时间线驱动，避免首页抖动
            if AppRuntimePolicy.supportsRealtimeAnimationDriver {
                TimelineView(.animation) { timelineContext in
                    Color.clear
                        .task(id: timelineContext.date.timeIntervalSinceReferenceDate) {
                            visitorManager.update(time: timelineContext.date.timeIntervalSinceReferenceDate)
                        }
                }
                .allowsHitTesting(false)
            }
            
            ZStack(alignment: .top) { // 改为 .top，确保悬浮窗默认在顶部
                // Tab content
                ZStack(alignment: .bottom) { // 内部保留 .bottom 用于 TabBar
                    ZStack {
                        // Keep Home mounted to preserve scroll/state when switching tabs.
                        HomeView(visitorManager: visitorManager, targetLevelId: $targetLevelId)
                            .opacity(selection == 0 ? 1 : 0)
                            .allowsHitTesting(selection == 0)
                        
                        Group {
                            switch selection {
                            case 1:
                                GalaxyView(visitorManager: visitorManager, onLocateLevel: { levelId in
                                    targetLevelId = levelId
                                    selection = 0
                                })
                            case 2:
                                CheckInView(visitorManager: visitorManager)
                            case 3:
                                MoreView(visitorManager: visitorManager)
                            default:
                                EmptyView()
                            }
                        }
                        .opacity(selection == 0 ? 0 : 1)
                        .allowsHitTesting(selection != 0)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.black.ignoresSafeArea())
                    .environmentObject(tabBarVisibility)
                    
                    if !tabBarVisibility.hideTabBar {
                        MainTabBar(selection: $selection)
                            .id("\(tabBarLanguage)_\(tabBarCheckInVersion)")
                    }
                }
                
                // Global Interaction Overlay
                if AppRuntimePolicy.supportsVisitorOverlay {
                    CelestialVisitorInteractionOverlay(manager: visitorManager).ignoresSafeArea()
                }
                
                // 3. 全局双倍收益悬浮窗 (Floating Buff Window)
                if AppRuntimePolicy.supportsFloatingBuffWindow {
                    FloatingBuffWindow()
                        .zIndex(100)
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .levelProgressDidChange)) { _ in
            let manager = LevelProgressManager.shared
            if tabBarLanguage != manager.currentLanguage {
                tabBarLanguage = manager.currentLanguage
            }
            if tabBarCheckInVersion != manager.checkInVersion {
                tabBarCheckInVersion = manager.checkInVersion
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
