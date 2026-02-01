package site.aiok.onepic.logic

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // AD IDs are now dynamically resolved via AdConfig.USE_TEST_ADS toggle
    private val INTERSTITIAL_ID get() = AdConfig.interstitialId
    private val REWARDED_ID get() = AdConfig.rewardedId

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context) {
            loadInterstitial(context)
            loadRewarded(context)
        }
    }

    // --- Interstitial Ads ---

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${adError.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded.")
                    interstitialAd = ad
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed.")
                    // User dismissed the ad, proceed logic
                    interstitialAd = null
                    onAdClosed()
                    loadInterstitial(activity) // Preload next one
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    interstitialAd = null
                    onAdClosed() // Fail gracefully
                }
                
                override fun onAdShowedFullScreenContent() {
                     Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready yet.")
            loadInterstitial(activity) // Try loading again
            onAdClosed() // Proceed anyway
        }
    }

    // --- Rewarded Ads ---

    fun loadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Rewarded failed to load: ${adError.message}")
                    rewardedAd = null
                    isRewardedLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded.")
                    rewardedAd = ad
                    isRewardedLoading = false
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onUserEarnedReward: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed.")
                    rewardedAd = null
                    loadRewarded(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Rewarded Ad failed to show.")
                    rewardedAd = null
                    onAdClosed()
                }
            }
            
            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "User earned the reward: $rewardAmount $rewardType")
                onUserEarnedReward()
            }
        } else {
            Log.d(TAG, "Rewarded Ad not ready yet.")
            loadRewarded(activity)
            // Note: If ad is not ready, we generally tell user "Ad not available" or do nothing
            // For now, we just call onAdClosed so the app doesn't hang, but NO REWARD given.
            onAdClosed() 
        }
    }
}
