import SwiftUI
import Combine
import GoogleMobileAds

/**
 * AdManager: 广告生命周期管理器
 * 适配 GoogleMobileAds SDK V11+ (移除 GAD 前缀)
 */
class AdManager: NSObject, ObservableObject, FullScreenContentDelegate {
    static let shared = AdManager()
    
    // 广告实例
    private var interstitial: InterstitialAd?
    private var rewardedAd: RewardedAd?
    private var appOpenAd: AppOpenAd?
    
    // 原生广告存储
    private var nativeAdHome: NativeAd?
    private var nativeAdGalaxy: NativeAd?
    var nativeAdOpen: NativeAd? // For Splash Screen, exposed internal for SplashView
    
    // Background tracking for Frequency Capping
    var lastBackgroundTime: Date?
    private let hotStartThreshold: TimeInterval = 5 * 60 // 5 minutes
    
    // Loaders & Mappings
    private var adLoaders: [AdLoader: NativeScene] = [:]
    
    // 加载状态
    @Published var isInterstitialReady = false
    @Published var isRewardedReady = false
    @Published var isNativeHomeReady = false
    @Published var isNativeGalaxyReady = false
    @Published var isNativeOpenReady = false

    // Expose publishers for external use (workaround for singleton @Published dynamicMember lookup issues)
    var nativeHomeReadyPublisher: Published<Bool>.Publisher { $isNativeHomeReady }
    var nativeGalaxyReadyPublisher: Published<Bool>.Publisher { $isNativeGalaxyReady }
    
    override private init() {
        super.init()
        // 1. 初始化 SDK (对齐 13.0.0 极致精简规范)
        // 必须在主线程启动，防止 Thread 8 类型的 malloc/free 并发崩溃
        if Thread.isMainThread {
            self.startSDK()
        } else {
            DispatchQueue.main.async {
                self.startSDK()
            }
        }
    }
    
    private func startSDK() {
        // 🔧 配置測試設備 ID (正式投放前，讓開發者手機以測試模式加載廣告)
        // 您可以在 Xcode 控制台日誌中找到您的測試設備 ID
        MobileAds.shared.requestConfiguration.testDeviceIdentifiers = [
            "9F89C84A559F573636A47FF8DAED0D33" // 用戶 iPhone 測試 ID
        ]
        
        MobileAds.shared.start { _ in
            // 2. 异步预加载核心广告，避免阻塞主线程初始化
            DispatchQueue.main.async {
                self.loadInterstitial()
                self.loadRewarded()
                self.loadNativeAd(for: .home)
                self.loadNativeAd(for: .galaxy)
                self.loadNativeAd(for: .appOpen) // Preload Native Open Ad
                self.loadRewardedInterstitial()
            }
        }
        
        // Double check loading for Rewarded Interstitial after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
            if !self.isRewardedInterstitialReady {
                print("🔄 AdManager: Retrying load for Rewarded Interstitial...")
                self.loadRewardedInterstitial()
            }
        }
    }
    
    // MARK: - Native Ads (原生广告)
    
    enum NativeScene {
        case home, galaxy, appOpen
        var adUnitID: String {
            switch self {
            case .home: return AdConfig.nativeHomeId
            case .galaxy: return AdConfig.nativeGalaxyId
            case .appOpen: return AdConfig.nativeAppOpenId
            }
        }
    }
    
    func loadNativeAd(for scene: NativeScene) {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        let root = windowScene?.windows.first?.rootViewController
        
        // 如果 root 尚未就绪（如启动初期），延迟加载以防阻塞渲染
        guard let validRoot = root else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.loadNativeAd(for: scene)
            }
            return
        }
        
        let loader = AdLoader(adUnitID: scene.adUnitID,
                             rootViewController: validRoot,
                             adTypes: [.native],
                             options: nil)
        loader.delegate = self
        adLoaders[loader] = scene
        loader.load(Request())
        print("🚀 AdManager: Loading Native Ad for \(scene) (ID: \(scene.adUnitID))")
    }
    
    func getNativeAd(for scene: NativeScene) -> NativeAd? {
        let ad: NativeAd?
        switch scene {
        case .home: 
            ad = nativeAdHome
            nativeAdHome = nil
            isNativeHomeReady = false
            loadNativeAd(for: .home)
        case .galaxy: 
            ad = nativeAdGalaxy
            nativeAdGalaxy = nil
            isNativeGalaxyReady = false
            loadNativeAd(for: .galaxy)
        case .appOpen:
            ad = nativeAdOpen
            // Don't auto-reload here immediately, we handle splash logic separately
        }
        return ad
    }
    
    // Flag to track if this is the first launch ever since installation
    var isFirstEverLaunch: Bool {
        let key = "isFirstEverLaunch"
        let isFirst = !UserDefaults.standard.bool(forKey: key)
        if isFirst {
            UserDefaults.standard.set(true, forKey: key)
        }
        return isFirst
    }
    
    // Flag to track if this is the first launch in current session (Cold Start)
    private var isFirstLaunch = true
    
    /// 判断是否满足热启动展示开屏的条件（进入后台 > 5分钟）
    func canShowHotStartAd() -> Bool {
        guard let lastTime = lastBackgroundTime else { return false }
        let elapsed = Date().timeIntervalSince(lastTime)
        return elapsed >= hotStartThreshold
    }

    // MARK: - App Open Ad (Native Implementation)
    
    // Replaced with Native Splash Logic. See loadNativeAd(for: .appOpen)
    
    // MARK: - Interstitial (插屏广告)
    
    // Callback closure for when interstitial is dismissed
    private var onInterstitialDismiss: (() -> Void)?
    
    func loadInterstitial() {
        InterstitialAd.load(with: AdConfig.interstitialId, request: Request()) { [weak self] ad, error in
            if let error = error {
                print("❌ AdManager: Failed to load Interstitial: \(error.localizedDescription)")
                self?.isInterstitialReady = false
                return
            }
            self?.interstitial = ad
            self?.isInterstitialReady = true
            print("✅ AdManager: Interstitial Loaded")
        }
    }
    
    func showInterstitial(completion: (() -> Void)? = nil) {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        guard let ad = interstitial, 
              let root = windowScene?.windows.first?.rootViewController else {
            print("⚠️ AdManager: Interstitial not ready")
            loadInterstitial()
            completion?() // 如果广告没准备好，直接执行回调
            return
        }
        
        self.onInterstitialDismiss = completion
        ad.fullScreenContentDelegate = self
        ad.present(from: root)
        
        // 注意：不在这里置 nil，而是在 dismiss 回调中置 nil 并重新加载
    }

    // MARK: - GADFullScreenContentDelegate
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("✅ AdManager: Ad Dismissed")
        // 触发回调
        onInterstitialDismiss?()
        onInterstitialDismiss = nil
        
        // 重新加载广告
        if ad is InterstitialAd {
            interstitial = nil
            isInterstitialReady = false
            loadInterstitial()
        } else if ad is RewardedInterstitialAd {
            rewardedInterstitialAd = nil
            isRewardedInterstitialReady = false
            loadRewardedInterstitial()
        }
    }
    
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("❌ AdManager: Ad Failed to Present: \(error.localizedDescription)")
        onInterstitialDismiss?()
        onInterstitialDismiss = nil
        
        if ad is InterstitialAd {
            interstitial = nil
            isInterstitialReady = false
            loadInterstitial()
        } else if ad is RewardedInterstitialAd {
            rewardedInterstitialAd = nil
            isRewardedInterstitialReady = false
            loadRewardedInterstitial()
        }
    }
    
    
    // MARK: - Rewarded Ad (激励广告)
    
    func loadRewarded() {
        RewardedAd.load(with: AdConfig.rewardedId, request: Request()) { [weak self] ad, error in
            if let error = error {
                print("❌ AdManager: Failed to load Rewarded AD: \(error.localizedDescription)")
                self?.isRewardedReady = false
                return
            }
            self?.rewardedAd = ad
            self?.isRewardedReady = true
            print("✅ AdManager: Rewarded Ad Loaded")
        }
    }
    
    func showRewarded(onReward: @escaping () -> Void) {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        guard let ad = rewardedAd, 
              let root = windowScene?.windows.first?.rootViewController else {
            print("⚠️ AdManager: Rewarded AD not ready")
            loadRewarded()
            return
        }
        
        ad.present(from: root) {
            let reward = ad.adReward
            print("💰 AdManager: User rewarded with \(reward.amount) \(reward.type)")
            onReward()
        }
        
        rewardedAd = nil
        isRewardedReady = false
        loadRewarded()
    }
    
    // MARK: - Rewarded Interstitial Ad (插页激励广告)
    
    private var rewardedInterstitialAd: RewardedInterstitialAd?
    @Published var isRewardedInterstitialReady = false
    
    func loadRewardedInterstitial() {
        let adUnitID = AdConfig.rewardedInterstitialId
        print("🚀 AdManager: Requesting Rewarded Interstitial with ID: \(adUnitID) (Test Mode: \(AdConfig.USE_TEST_ADS))")
        
        RewardedInterstitialAd.load(with: adUnitID, request: Request()) { [weak self] ad, error in
            if let error = error {
                print("❌ AdManager: Failed to load Rewarded Interstitial (ID: \(adUnitID)): \(error.localizedDescription)")
                self?.isRewardedInterstitialReady = false
                return
            }
            self?.rewardedInterstitialAd = ad
            self?.isRewardedInterstitialReady = true
            print("✅ AdManager: Rewarded Interstitial Ad Loaded")
        }
    }
    
    func showRewardedInterstitial(onAdDismissed: @escaping () -> Void, onReward: @escaping (Int) -> Void) {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        guard let ad = rewardedInterstitialAd,
              let root = windowScene?.windows.first?.rootViewController else {
            print("⚠️ AdManager: Rewarded Interstitial not ready")
            loadRewardedInterstitial()
            onAdDismissed() // Fallback: proceed as if dismissed
            return
        }
        
        // Disable FullScreenContentDelegate for this specific ad type if it conflicts, 
        // or ensure we handle it correctly. For simplicity, we use the completion handler of `present`.
        // However, RewardedInterstitialAd also uses FullScreenContentDelegate for dismissal.
        // We reuse the existing delegate logic for simplicity, but need to handle the specific callback.
        
        self.onInterstitialDismiss = onAdDismissed
        ad.fullScreenContentDelegate = self
        
        ad.present(from: root) { [weak self] in
            let reward = ad.adReward
            print("💰 AdManager: User rewarded (Interstitial) with \(reward.amount) \(reward.type)")
            // Hardcode 100 or use reward.amount (typically 1 in test)
            // User requested 100 coins. We can trust the callback or force it.
            // Let's pass 100 as per requirement.
            onReward(100) 
        }
    }
}

// MARK: - AdLoaderDelegate
extension AdManager: NativeAdLoaderDelegate {
    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        guard let scene = adLoaders[adLoader] else {
            print("⚠️ AdManager: Received ad for unknown loader")
            return
        }
        
        switch scene {
        case .home:
            self.nativeAdHome = nativeAd
            self.isNativeHomeReady = true
        case .galaxy:
            self.nativeAdGalaxy = nativeAd
            self.isNativeGalaxyReady = true
        case .appOpen:
            self.nativeAdOpen = nativeAd
            self.isNativeOpenReady = true
        }
        
        adLoaders.removeValue(forKey: adLoader)
        print("✅ AdManager: Native Ad Loaded and assigned to \(scene)")
    }
    
    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        print("❌ AdManager: Native Ad failed: \(error.localizedDescription)")
    }
}
