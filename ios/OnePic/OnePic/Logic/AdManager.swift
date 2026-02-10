import SwiftUI
import Combine
import GoogleMobileAds

/**
 * AdManager: 广告生命周期管理器
 * 适配 GoogleMobileAds SDK V11+ (移除 GAD 前缀)
 */
class AdManager: NSObject, ObservableObject {
    static let shared = AdManager()
    
    // 广告实例
    private var interstitial: InterstitialAd?
    private var rewardedAd: RewardedAd?
    private var appOpenAd: AppOpenAd?
    
    // 原生广告存储
    private var nativeAdHome: NativeAd?
    private var nativeAdGalaxy: NativeAd?
    private var adLoader: AdLoader?
    
    // 加载状态
    @Published var isInterstitialReady = false
    @Published var isRewardedReady = false
    @Published var isNativeHomeReady = false
    
    override private init() {
        super.init()
        // 1. 初始化 SDK (对齐 13.0.0 极致精简规范)
        MobileAds.shared.start(completionHandler: nil)
        
        // 2. 预加载核心广告
        loadInterstitial()
        loadRewarded()
        loadNativeAd(for: .home)
    }
    
    // MARK: - Native Ads (原生广告)
    
    enum NativeScene {
        case home, galaxy
        var adUnitID: String {
            switch self {
            case .home: return AdConfig.nativeHomeId
            case .galaxy: return AdConfig.nativeGalaxyId
            }
        }
    }
    
    func loadNativeAd(for scene: NativeScene) {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        let root = windowScene?.windows.first?.rootViewController
        
        adLoader = AdLoader(adUnitID: scene.adUnitID,
                           rootViewController: root,
                           adTypes: [.native],
                           options: nil)
        adLoader?.delegate = self
        adLoader?.load(Request())
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
            loadNativeAd(for: .galaxy)
        }
        return ad
    }
    
    // MARK: - App Open Ad (开屏广告)
    
    func loadAppOpenAd() {
        AppOpenAd.load(with: AdConfig.appOpenId, request: Request()) { [weak self] ad, error in
            if let error = error {
                print("❌ AdManager: Failed to load WebOpen AD: \(error.localizedDescription)")
                return
            }
            self?.appOpenAd = ad
            print("✅ AdManager: App Open Ad Loaded")
        }
    }
    
    func showAppOpenAdIfAvailable() {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        guard let ad = appOpenAd, 
              let root = windowScene?.windows.first?.rootViewController else {
            loadAppOpenAd()
            return
        }
        ad.present(from: root)
        appOpenAd = nil
    }
    
    // MARK: - Interstitial (插屏广告)
    
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
    
    func showInterstitial() {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        guard let ad = interstitial, 
              let root = windowScene?.windows.first?.rootViewController else {
            print("⚠️ AdManager: Interstitial not ready")
            loadInterstitial()
            return
        }
        ad.present(from: root)
        interstitial = nil
        isInterstitialReady = false
        loadInterstitial()
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
}

// MARK: - AdLoaderDelegate
extension AdManager: NativeAdLoaderDelegate {
    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        if adLoader.adUnitID == AdConfig.nativeHomeId {
            self.nativeAdHome = nativeAd
            self.isNativeHomeReady = true
        } else {
            self.nativeAdGalaxy = nativeAd
        }
        print("✅ AdManager: Native Ad Loaded for \(adLoader.adUnitID)")
    }
    
    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        print("❌ AdManager: Native Ad failed: \(error.localizedDescription)")
    }
}
