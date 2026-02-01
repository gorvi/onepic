package site.aiok.onepic

import android.app.Application
import com.google.android.gms.ads.MobileAds
import site.aiok.onepic.logic.AdManager
import site.aiok.onepic.logic.AppOpenAdManager

/**
 * Application class for OnePic.
 * 
 * Initializes:
 * - MobileAds SDK
 * - App Open Ad Manager
 */
class OnePicApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob SDK
        MobileAds.initialize(this) { initializationStatus ->
            // SDK initialized, load app open ad
            appOpenAdManager = AppOpenAdManager(this)
            appOpenAdManager.loadAd()
        }
    }

    companion object {
        private var instance: OnePicApplication? = null
        
        fun getInstance(): OnePicApplication? = instance
    }

    init {
        instance = this
    }
}
