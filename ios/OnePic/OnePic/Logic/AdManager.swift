import SwiftUI
import Combine
// import GoogleMobileAds // Commented out until SDK is added

class AdManager: NSObject, ObservableObject {
    static let shared = AdManager()
    
    // Placeholder IDs
    let interstitialId = "ca-app-pub-3940256099942544/4411468910" // Test ID
    let rewardedId = "ca-app-pub-3940256099942544/1712485313" // Test ID
    
    // private var interstitial: GADInterstitialAd?
    // private var rewardedAd: GADRewardedAd?
    
    override private init() {
        super.init()
        // GADMobileAds.sharedInstance().start(completionHandler: nil)
        loadInterstitial()
        loadRewarded()
    }
    
    func loadInterstitial() {
        /*
        let request = GADRequest()
        GADInterstitialAd.load(withAdUnitID: interstitialId, request: request) { [weak self] ad, error in
            if let error = error {
                print("Failed to load interstitial: \(error)")
                return
            }
            self?.interstitial = ad
        }
        */
        print("AdManager: Mock Load Interstitial")
    }
    
    func showInterstitial() {
        /*
        if let ad = interstitial {
            // Needed: Root View Controller
            if let root = UIApplication.shared.windows.first?.rootViewController {
                ad.present(fromRootViewController: root)
            }
        } else {
            print("Ad not ready")
            loadInterstitial()
        }
        */
        print("AdManager: Mock Show Interstitial")
    }
    
    func loadRewarded() {
        /*
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: rewardedId, request: request) { [weak self] ad, error in
            if let error = error {
                 print("Failed to load rewarded: \(error)")
                 return
            }
            self?.rewardedAd = ad
        }
        */
         print("AdManager: Mock Load Rewarded")
    }
    
    func showRewarded(onReward: @escaping () -> Void) {
        /*
        if let ad = rewardedAd {
             if let root = UIApplication.shared.windows.first?.rootViewController {
                 ad.present(fromRootViewController: root) {
                     onReward()
                 }
             }
        } else {
            print("Ad not ready")
            loadRewarded()
        }
        */
        print("AdManager: Mock Show Rewarded - Grating Reward")
        onReward()
    }
}
