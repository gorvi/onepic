import Foundation

/**
 * AdMob 广告配置
 * 同步 Android AdConfig.kt 逻辑
 */
struct AdConfig {
    // 🔧 全局开关 - 开启后使用 Google 官方测试 ID
    static let USE_TEST_ADS = false 

    // iOS 应用 ID (admobIDs.md 提供: ca-app-pub-6888206339207009~9992118933)
    // 注意：Info.plist 中的 GADApplicationIdentifier 也需同步更新
    static let APP_ID = "ca-app-pub-6888206339207009~9992118933"

    // --- 动态 ID 获取器 ---
    
    // 开屏广告
    static var appOpenId: String {
        USE_TEST_ADS ? Test.APP_OPEN : Production.APP_OPEN
    }
    
    // 插屏广告
    static var interstitialId: String {
        USE_TEST_ADS ? Test.INTERSTITIAL : Production.INTERSTITIAL
    }
    
    // 激励广告 (100金币/提示)
    static var rewardedId: String {
        USE_TEST_ADS ? Test.REWARDED : Production.REWARDED_100_COINS
    }
    
    // 横幅广告 1 (首页)
    static var bannerHomeId: String {
        USE_TEST_ADS ? Test.BANNER : Production.BANNER_1
    }
    
    // 横幅广告 2 (关卡)
    static var bannerGameId: String {
        USE_TEST_ADS ? Test.BANNER : Production.BANNER_2
    }
    
    // 激励广告备份 (来自第二个 kaiping_ad ID)
    static var rewardedBackupId: String {
        USE_TEST_ADS ? Test.REWARDED : Production.REWARDED_BACKUP
    }
    
    // 原生广告 1 (首页)
    static var nativeHomeId: String {
        USE_TEST_ADS ? Test.NATIVE : Production.NATIVE_1
    }
    
    // 原生广告 2 (Galaxy)
    static var nativeGalaxyId: String {
        USE_TEST_ADS ? Test.NATIVE : Production.NATIVE_2
    }
    
    // 原生广告 3
    static var native3Id: String {
        USE_TEST_ADS ? Test.NATIVE : Production.NATIVE_3
    }
    
    // 原生开屏广告 (Native App Open)
    static var nativeAppOpenId: String {
        USE_TEST_ADS ? Test.NATIVE : Production.NATIVE_APP_OPEN
    }

    // --- 正式 ID (来自 admobIDs.md) ---
    private struct Production {
        static let INTERSTITIAL = "ca-app-pub-6888206339207009/8638966248" // chaye
        static let BANNER_1 = "ca-app-pub-6888206339207009/3855113662" // hengfuad1
        static let BANNER_2 = "ca-app-pub-6888206339207009/6124483547" // hengfuad2
        static let REWARDED_100_COINS = "ca-app-pub-6888206339207009/9195070253" // Hint_Reward_100_Coins
        static let APP_OPEN = "ca-app-pub-6888206339207009/5206997433" // kaiping_ad (开屏广告)
        static let REWARDED_BACKUP = "ca-app-pub-6888206339207009/7325884573" // kaiping_ad (激励广告)
        static let NATIVE_1 = "ca-app-pub-6888206339207009/6568906919" // yuansheng1
        static let NATIVE_2 = "ca-app-pub-6888206339207009/3942743575" // yuansheng2
        static let NATIVE_3 = "ca-app-pub-6888206339207009/7690416896" // yuansheng3
        static let NATIVE_APP_OPEN = "ca-app-pub-6888206339207009/6332969743" // Native App Open
        static let REWARDED_INTERSTITIAL = "ca-app-pub-6888206339207009/8480630671" // chayejili_100coins
    }

    // 插页激励广告 (100金币)
    static var rewardedInterstitialId: String {
        USE_TEST_ADS ? Test.REWARDED_INTERSTITIAL : Production.REWARDED_INTERSTITIAL
    }

    // --- Google 官方 iOS 测试 ID ---
    private struct Test {
        // Google 官方 iOS 测试 ID
        static let APP_OPEN = "ca-app-pub-3940256099942544/5662855259"
        static let BANNER = "ca-app-pub-3940256099942544/2934735716"
        static let INTERSTITIAL = "ca-app-pub-3940256099942544/4411468910"
        static let REWARDED = "ca-app-pub-3940256099942544/1712485313"
        static let REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/6978759866" // Google Test ID for Rewarded Interstitial
        static let NATIVE = "ca-app-pub-3940256099942544/3986624511"
        // 注意：AdMob 提供的原生测试 ID 只有一个，所以不同原生场景在测试时确实会共享 ID。
    }
}
