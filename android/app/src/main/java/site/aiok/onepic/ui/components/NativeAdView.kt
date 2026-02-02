package site.aiok.onepic.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import site.aiok.onepic.R
import site.aiok.onepic.logic.AdConfig

/**
 * Native Ad Composable with custom styling that matches the app's design.
 */
@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    preloadedNativeAd: com.google.android.gms.ads.nativead.NativeAd? = null,
    loadDelayMillis: Long = 2000L // Delay loading to avoid CPU spikes during screen transitions
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var hasError by remember { mutableStateOf(false) }

    val effectiveAd = preloadedNativeAd ?: nativeAd

    // Only load local ad if no pre-loaded ad is provided
    if (preloadedNativeAd == null) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(loadDelayMillis)
            val adLoader = AdLoader.Builder(context, AdConfig.nativeHomeId)
                .forNativeAd { ad ->
                    nativeAd = ad
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        hasError = true
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()
            
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    // Only destroy locally loaded ads
    DisposableEffect(nativeAd) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    // Use Crossfade for smooth appearance
    androidx.compose.animation.Crossfade(
        targetState = effectiveAd,
        animationSpec = tween(500),
        label = "native_ad_fade"
    ) { ad ->
        if (ad != null && !hasError) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    // Removed extra clip here to ensure native views get all touches
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A1A2E),
                                Color(0xFF16213E)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val inflater = LayoutInflater.from(ctx)
                        val adView = inflater.inflate(R.layout.native_ad_layout, null) as NativeAdView
                        
                        // Precision Binding: Only register when attached to window
                        // This prevents off-screen items in LazyColumn from stealing session/click registration
                        adView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: android.view.View) {
                                (v as? NativeAdView)?.let { view ->
                                    populateNativeAdView(ad, view)
                                }
                            }
                            override fun onViewDetachedFromWindow(v: android.view.View) {
                                // Destroy the view wrapper on detach to clear singleton binding
                                (v as? NativeAdView)?.destroy()
                            }
                        })
                        
                        adView
                    },
                    update = { adView ->
                        // If already attached, ensure it's up to date
                        if (androidx.core.view.ViewCompat.isAttachedToWindow(adView)) {
                            populateNativeAdView(ad, adView)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(modifier = modifier.height(1.dp).fillMaxWidth())
        }
    }
}

/**
 * Populate native ad view with ad data.
 */
private fun populateNativeAdView(nativeAd: NativeAd?, adView: NativeAdView) {
    if (nativeAd == null) return
    
    // Set media view
    adView.mediaView = adView.findViewById(R.id.ad_media)
    
    // Set other ad assets
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_icon)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    
    // ⚠️ 关键优化: 如果没有明确的点击区域或者为了提升体验，可以将整个容器也注册为可点击
    // 这样点击广告的任何地方（包含空白处）都能跳转
    val container = adView.findViewById<android.view.View>(R.id.ad_unit_container)
    if (adView.callToActionView == null && container != null) {
        adView.callToActionView = container
    }
    
    // Important: Set AdChoices view for policy compliance
    // The SDK will automatically populate the AdChoices icon here
    val adChoicesContainer = adView.findViewById<android.widget.FrameLayout>(R.id.ad_choices_container)
    if (adChoicesContainer != null) {
        // AdChoices will be rendered by the SDK in setNativeAd()
    }

    // Populate views with data
    (adView.headlineView as? TextView)?.text = nativeAd.headline
    
    nativeAd.mediaContent?.let { mediaContent ->
        adView.mediaView?.mediaContent = mediaContent
    }

    if (nativeAd.body == null) {
        adView.bodyView?.visibility = android.view.View.INVISIBLE
    } else {
        adView.bodyView?.visibility = android.view.View.VISIBLE
        (adView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = android.view.View.INVISIBLE
    } else {
        adView.callToActionView?.visibility = android.view.View.VISIBLE
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        adView.iconView?.visibility = android.view.View.GONE
    } else {
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView?.visibility = android.view.View.VISIBLE
    }

    if (nativeAd.starRating == null) {
        adView.starRatingView?.visibility = android.view.View.INVISIBLE
    } else {
        (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating?.toFloat() ?: 0f
        adView.starRatingView?.visibility = android.view.View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
        adView.advertiserView?.visibility = android.view.View.INVISIBLE
    } else {
        (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
        adView.advertiserView?.visibility = android.view.View.VISIBLE
    }

    // Register the ad view
    adView.setNativeAd(nativeAd)
}

/**
 * Compact Native Ad for Home screen.
 */
@Composable
fun HomeNativeAd(modifier: Modifier = Modifier) {
    NativeAdView(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

/**
 * Compact Native Ad for Gallery screen.
 */
@Composable
fun GalleryNativeAd(modifier: Modifier = Modifier) {
    NativeAdView(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}
