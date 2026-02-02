package site.aiok.onepic.logic

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * App Open Ad Manager
 * 
 * Shows ads when:
 * - Cold start: App launched from scratch
 * - Hot start: App returns from background (with cooldown)
 * 
 * Does NOT show ads when:
 * - User is in game (GameScreen)
 * - Less than 3 minutes since last ad
 */
class AppOpenAdManager(private val application: Application) : 
    Application.ActivityLifecycleCallbacks, 
    DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes between ads
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var lastAdShownTime: Long = 0
    private var isFirstLoad = true // Track if this is the initial load for cold start
    
    private var currentActivity: Activity? = null
    
    // Flag to prevent showing ads during gameplay
    var isGameActive: Boolean = false

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Load an app open ad */
    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val adId = AdConfig.appOpenId
        Log.d(TAG, "Loading App Open Ad: $adId")
        
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            adId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    
                    // Cold start optimization: 
                    // If this is the first load and we're currently in foreground, show it immediately
                    if (isFirstLoad) {
                        isFirstLoad = false
                        currentActivity?.let { activity ->
                            Log.d(TAG, "Cold start detected, showing ad immediately after load")
                            showAdIfAvailable(activity)
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                    isFirstLoad = false // Stop trying to force cold start show
                }
            }
        )
    }

    /** Check if ad is available and not expired (4 hours max) */
    private fun isAdAvailable(): Boolean {
        val wasLoadedLessThan4HoursAgo = Date().time - loadTime < 4 * 60 * 60 * 1000
        return appOpenAd != null && wasLoadedLessThan4HoursAgo
    }

    /** Check if cooldown period has passed */
    private fun isCooldownOver(): Boolean {
        return Date().time - lastAdShownTime > COOLDOWN_MS
    }

    /** Show the app open ad if available */
    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        // Don't show if already showing, in game, or on cooldown
        if (isShowingAd) {
            Log.d(TAG, "Already showing ad, skipping")
            onComplete()
            return
        }
        
        if (isGameActive) {
            Log.d(TAG, "Game is active, skipping ad")
            onComplete()
            return
        }
        
        if (!isCooldownOver()) {
            Log.d(TAG, "Cooldown not over, skipping ad")
            onComplete()
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "Ad not available, loading new one")
            loadAd()
            onComplete()
            return
        }

        Log.d(TAG, "Showing App Open Ad")
        isShowingAd = true

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                lastAdShownTime = Date().time
                loadAd() // Preload next ad
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad showed")
            }
        }

        appOpenAd?.show(activity)
    }

    // --- DefaultLifecycleObserver callbacks ---
    
    /** Called when app comes to foreground */
    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            showAdIfAvailable(activity)
        }
    }

    // --- ActivityLifecycleCallbacks ---
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    
    override fun onActivityStarted(activity: Activity) {
        // Don't show ad on game screen
        if (activity.localClassName.contains("GameScreen") || 
            activity.localClassName.contains("MainActivity")) {
            // Check if we're on the game route - this is handled by isGameActive flag
        }
        
        if (!isShowingAd) {
            currentActivity = activity
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
