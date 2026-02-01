package site.aiok.onepic.logic

/**
 * AdMob Ad Configuration.
 * 
 * App ID (configured in AndroidManifest.xml): ca-app-pub-6888206339207009~1463751057
 */
object AdConfig {

    // ╔════════════════════════════════════════════════════════════════╗
    // ║  🔧 MASTER TOGGLE - Set to false before release!              ║
    // ╚════════════════════════════════════════════════════════════════╝
    const val USE_TEST_ADS = false  // <-- 开发时 true，发布前改为 false

    // --- Dynamic ID Getters (Automatically select based on toggle) ---
    val appOpenId: String get() = if (USE_TEST_ADS) Test.APP_OPEN else Production.APP_OPEN
    val rewardedId: String get() = if (USE_TEST_ADS) Test.REWARDED else Production.REWARDED_100_COINS
    val rewardedDoubleId: String get() = if (USE_TEST_ADS) Test.REWARDED else Production.REWARDED_DOUBLE_COINS
    val bannerHomeId: String get() = if (USE_TEST_ADS) Test.BANNER_ADAPTIVE else Production.BANNER_HOME
    val bannerGalleryId: String get() = if (USE_TEST_ADS) Test.BANNER_ADAPTIVE else Production.BANNER_GALLERY
    val interstitialId: String get() = if (USE_TEST_ADS) Test.INTERSTITIAL else Production.INTERSTITIAL
    val nativeHomeId: String get() = if (USE_TEST_ADS) Test.NATIVE else Production.NATIVE_HOME
    val nativeGalaxyId: String get() = if (USE_TEST_ADS) Test.NATIVE else Production.NATIVE_GALAXY
    val nativeAd3Id: String get() = if (USE_TEST_ADS) Test.NATIVE else Production.NATIVE_3

    // --- Production IDs ---
    object Production {
        const val APP_OPEN = "ca-app-pub-6888206339207009/1964874543"
        const val REWARDED_100_COINS = "ca-app-pub-6888206339207009/4450663003"
        const val REWARDED_DOUBLE_COINS = "ca-app-pub-6888206339207009/2591620574"
        const val BANNER_HOME = "ca-app-pub-6888206339207009/3352804378"
        const val BANNER_GALLERY = "ca-app-pub-6888206339207009/9616968004"
        const val NATIVE_HOME = "ca-app-pub-6888206339207009/2121621366" // 原生广告1
        const val NATIVE_GALAXY = "ca-app-pub-6888206339207009/6803463791" // 原生广告2
        const val NATIVE_3 = "ca-app-pub-6888206339207009/5223562896" // 原生广告3
        const val INTERSTITIAL = "" // TODO: Add production interstitial ID if needed
    }

    // --- Test IDs (Google Official Demo IDs) ---
    object Test {
        const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
        const val BANNER_ADAPTIVE = "ca-app-pub-3940256099942544/9214589741"
        const val BANNER_FIXED = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val INTERSTITIAL_REWARDED = "ca-app-pub-3940256099942544/5354046379"
        const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val NATIVE_VIDEO = "ca-app-pub-3940256099942544/1044960115"
    }
}

