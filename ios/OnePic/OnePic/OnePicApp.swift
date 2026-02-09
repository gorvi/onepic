import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

@main
struct OnePicApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Initialize AdManager (mocked or real)
        _ = AdManager.shared
        // Initialize SoundManager properties if needed
        // SoundManager.shared.preload() // if implemented
    }
    
    @State private var showIntro = !UserDefaults.standard.bool(forKey: "hasLaunchedBefore")
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                Color.black.ignoresSafeArea()
                if showIntro {
                    IntroView {
                        // Completion Handler
                        UserDefaults.standard.set(true, forKey: "hasLaunchedBefore")
                        withAnimation {
                            showIntro = false
                        }
                    }
                } else {
                    MainTabView()
                }
            }
            .onAppear {
                setWindowBackgroundBlack()
            }
            .onChange(of: scenePhase) { _, newPhase in
                if newPhase == .active {
                    setWindowBackgroundBlack()
                }
            }
        }
    }
    
    /// 从系统层把窗口背景设为黑色，彻底消除底部安全区白条
    private func setWindowBackgroundBlack() {
        #if os(iOS)
        DispatchQueue.main.async {
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first {
                window.backgroundColor = .black
            }
        }
        #endif
    }
}
