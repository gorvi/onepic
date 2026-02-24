import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

import OSLog

@main
struct OnePicApp: App {
    @Environment(\.scenePhase) private var scenePhase
    
    private let logger = Logger(subsystem: "site.aiok.OnePic", category: "AppLifeCycle")

    init() {
        // 解耦 AdManager 初始化到 onAppear，防止 App 启动初期窗口尚未建立时的死锁
    }
    
    @State private var showSplashAd = false
    @State private var isFirstLaunch = true
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
                
                // Native Splash Ad Overlay
                if showSplashAd {
                    SplashAdView(isPresented: $showSplashAd)
                        .transition(.opacity)
                        .zIndex(100)
                }
            }
            .onAppear {
                setWindowBackgroundBlack()
                // 在首屏加载后懒加载广告管理器，确保窗口环境已就绪
                // _ = AdManager.shared // Moved to explicit check below
                
                // Check for splash ad
                checkForSplashAd()
            }
            .onChangeCompat(of: scenePhase) { newPhase in
                switch newPhase {
                case .background:
                    setWindowBackgroundBlack()
                    // Record background time for frequency capping
                    AdManager.shared.lastBackgroundTime = Date()
                    
                case .active:
                    setWindowBackgroundBlack()
                    
                    // Hot Start Logic: Only show splash if it's NOT the cold start and meet threshold
                    if !isFirstLaunch, AdManager.supportsNativeAds {
                        if AdManager.shared.canShowHotStartAd() && AdManager.shared.nativeAdOpen != nil {
                            print("🚀 OnePicApp: [Hot Start] Showing splash ad (Frequency cap met).")
                            withAnimation {
                                showSplashAd = true
                            }
                        } else {
                            // Preload for next time if not showing now
                            AdManager.shared.loadNativeAd(for: .appOpen)
                        }
                    }
                default:
                    break
                }
            }
        }
    }
    
    private func checkForSplashAd() {
        guard isFirstLaunch else { return }
        
        guard AdManager.supportsNativeAds else {
            print("ℹ️ OnePicApp: Native splash disabled on iOS 15.x")
            isFirstLaunch = false
            return
        }
        
        // Initialize SDK
        _ = AdManager.shared
        
        // --- 逻辑优化：首播跳过 & 二次秒开 ---
        if AdManager.shared.isFirstEverLaunch {
            print("🆕 OnePicApp: [First Ever Launch] Skipping splash UI. Ad will pre-cache for next time.")
            isFirstLaunch = false
            return
        }
        
        print("🚀 OnePicApp: [Cold Start] Starting Native Splash Ad check...")
        
        // 后续启动：由于有第一次预留的资源缓存，这里尝试较短时间检测 (2.6s)
        // 增加到 2.6s 以给 SDK 初始化和素材准备留出足够空间
        var attempts = 0
        Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { timer in
            attempts += 1
            if AdManager.shared.nativeAdOpen != nil {
                timer.invalidate()
                print("🚀 OnePicApp: Native Splash Ad Ready! Showing after \(String(format: "%.1f", Double(attempts) * 0.2))s")
                withAnimation {
                    showSplashAd = true
                }
                isFirstLaunch = false
            } else if attempts >= 13 { // 2.6s timeout (13 * 0.2)
                timer.invalidate()
                print("⚠️ OnePicApp: Splash Ad Timeout (2.6s). Skipping to Home.")
                isFirstLaunch = false
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
