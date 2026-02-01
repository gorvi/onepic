package site.aiok.onepic.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import site.aiok.onepic.logic.AdConfig

/**
 * Adaptive Banner Ad Composable.
 * 
 * Wraps AdMob AdView for Jetpack Compose with proper lifecycle management.
 * 
 * @param adUnitId The AdMob ad unit ID to use
 * @param modifier Modifier for the composable
 */
@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Create AdView and remember it across recompositions
    val adView = remember {
        AdView(context).apply {
            setAdSize(getAdaptiveBannerAdSize(context))
            this.adUnitId = adUnitId
            
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("BannerAdView", "Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("BannerAdView", "Banner ad failed to load: ${loadAdError.message}")
                }

                override fun onAdClicked() {
                    Log.d("BannerAdView", "Banner ad clicked")
                }
            }
        }
    }
    
    // Handle lifecycle
    DisposableEffect(adView) {
        // Load the ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        
        onDispose {
            adView.destroy()
        }
    }
    
    // Render the AdView
    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Calculate adaptive banner size based on screen width.
 */
private fun getAdaptiveBannerAdSize(context: Context): AdSize {
    val displayMetrics = context.resources.displayMetrics
    val adWidthPixels = displayMetrics.widthPixels.toFloat()
    val density = displayMetrics.density
    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
}

/**
 * Home/Level Select Banner
 */
@Composable
fun HomeBannerAd(modifier: Modifier = Modifier) {
    BannerAdView(
        adUnitId = AdConfig.bannerHomeId,
        modifier = modifier
    )
}

/**
 * Gallery Banner
 */
@Composable
fun GalleryBannerAd(modifier: Modifier = Modifier) {
    BannerAdView(
        adUnitId = AdConfig.bannerGalleryId,
        modifier = modifier
    )
}
