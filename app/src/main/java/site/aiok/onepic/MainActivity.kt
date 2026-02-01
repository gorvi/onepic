package site.aiok.onepic

import kotlinx.coroutines.withContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.theme.OnepicTheme
import site.aiok.onepic.ui.components.BottomNavigationBar
import site.aiok.onepic.ui.components.BottomNavItem
import site.aiok.onepic.utils.LocaleHelper
import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(site.aiok.onepic.utils.LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        site.aiok.onepic.logic.AdManager.initialize(this)
        
        // Initialize TRANS helper
        site.aiok.onepic.utils.TRANS.init(this)

        // 处理从通知点击进入的情况
        handleNotificationIntent(intent)

        enableEdgeToEdge()
        setContent {
            OnepicTheme {
                // 底部导航栏的当前路由
                var currentBottomNavRoute by remember { mutableStateOf(BottomNavItem.Home.route) }
                
                // 首次启动检测
                val isFirstLaunch = remember { site.aiok.onepic.data.LevelProgressManager.isFirstLaunch(this) }
                // 游戏相关的导航状态
                var currentScreen by remember { mutableStateOf(if (isFirstLaunch) "intro" else "level_select") }
                
                var selectedLevel by remember { mutableStateOf<site.aiok.onepic.model.LevelConfig?>(null) }
                var levelCompleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var levelIndex by remember { mutableStateOf(0) }
                var levelMode by remember { mutableStateOf("classic") } // classic or gallery
                // 用于触发关卡选择页面重新检查滚动
                var lastGameExitKey by remember { mutableStateOf(0) }
                var lastPlayedLevelIndex by remember { mutableStateOf<Int?>(null) }
                // 保存进入游戏前的 completedLevels，用于检测是否完成了新关卡
                var completedLevelsBeforeGame by remember { mutableStateOf<Set<Int>>(emptySet()) }
                
                // Target level ID to scroll to (from Gallery)
                var targetLevelId by remember { mutableStateOf<Int?>(null) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Pre-load Native Ads at App level for singleton reuse
                    var homeNativeAdState by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
                    var galleryNativeAdState by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
                    
                    val context = LocalContext.current
                    DisposableEffect(Unit) {
                        fun loadAdWithFallback(primaryId: String, onResult: (com.google.android.gms.ads.nativead.NativeAd?) -> Unit, isRetry: Boolean = false) {
                            val adId = if (isRetry) site.aiok.onepic.logic.AdConfig.nativeAd3Id else primaryId
                            val loader = com.google.android.gms.ads.AdLoader.Builder(context, adId)
                                .forNativeAd { onResult(it) }
                                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                        if (!isRetry) {
                                            // Silently fallback to backup ID
                                            loadAdWithFallback(primaryId, onResult, isRetry = true)
                                        }
                                    }
                                })
                                .build()
                            loader.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                        }

                        loadAdWithFallback(site.aiok.onepic.logic.AdConfig.nativeHomeId, { homeNativeAdState = it })
                        loadAdWithFallback(site.aiok.onepic.logic.AdConfig.nativeGalaxyId, { galleryNativeAdState = it })
                        
                        onDispose {
                            homeNativeAdState?.destroy()
                            galleryNativeAdState?.destroy()
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            // 只在首页、飞升等非核心体验路径显示底部导航栏
                            // 游戏页面和启动宣传页 (Intro) 应保持全屏沉浸
                            if (currentScreen != "game" && currentScreen != "intro") {
                                Column {
                                    // Native Ad above navigation bar (Galaxy tab uses in-list ads)
                                    // when (currentBottomNavRoute) { ... } removed as both Home and Galaxy now use in-list ads
                                    
                                    BottomNavigationBar(
                                        currentRoute = currentBottomNavRoute,
                                        onNavigate = { route ->
                                            currentBottomNavRoute = route
                                            // 如果切换到首页，确保显示关卡选择页面
                                            if (route == BottomNavItem.Home.route) {
                                                currentScreen = "level_select"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        // Performance: Hoist level loading to Main scope to avoid re-parsing on tab switch
                        // Performance: Hoist level loading to Main scope to avoid re-parsing on tab switch
                        val currentLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(this@MainActivity)
                        val allLevels by produceState<List<LevelConfig>>(initialValue = site.aiok.onepic.data.LevelRepository.getClassicLevels(this@MainActivity), key1 = currentLanguage) {
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                // Main List should contain Tutorial + 60 levels
                                value = site.aiok.onepic.data.LevelRepository.getClassicLevels(this@MainActivity)
                            }
                        }


                        // 根据底部导航栏路由和游戏状态显示不同页面
                        // Use a dark Surface as base to prevent white flash during Crossfade
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF0D0221) // Matches GalaxyBackground start color
                        ) {
                                Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true }) {
                                    // Implement Crossfade for smoother tab transitions
                                    androidx.compose.animation.Crossfade(
                                        targetState = Pair(currentScreen, currentBottomNavRoute),
                                        animationSpec = tween(durationMillis = 300),
                                        label = "tab_switch"
                                    ) { (screen, route) ->
                                        when {
                                            screen == "intro" -> {
                                                site.aiok.onepic.ui.IntroScreen(
                                                    onStartJourney = {
                                                        site.aiok.onepic.data.LevelProgressManager.setFirstLaunchCompleted(this@MainActivity)
                                                        val classicLevels = allLevels
                                                        if (classicLevels.isNotEmpty()) {
                                                            // Find first unlocked and incomplete level (starting from index 0)
                                                            val unlocked = site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(this@MainActivity)
                                                            val completed = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(this@MainActivity)
                                                            val nextIndex = classicLevels.indices.firstOrNull { it in unlocked && it !in completed } ?: 0
                                                            
                                                            selectedLevel = classicLevels[nextIndex]
                                                            levelIndex = nextIndex
                                                            levelMode = "classic"
                                                            levelCompleteCallback = {
                                                                site.aiok.onepic.data.LevelProgressManager.markClassicLevelCompleted(this@MainActivity, nextIndex)
                                                                site.aiok.onepic.data.LevelProgressManager.unlockClassicLevel(this@MainActivity, nextIndex + 1)
                                                                
                                                                currentScreen = "level_select"
                                                                currentBottomNavRoute = BottomNavItem.Home.route
                                                                lastGameExitKey++
                                                            }
                                                            currentScreen = "game"
                                                        }
                                                    }
                                                )
                                            }
                                            screen == "game" -> {
                                                selectedLevel?.let { level ->
                                                    site.aiok.onepic.ui.GameScreen(
                                                        levelConfig = level,
                                                        levelIndex = levelIndex,
                                                        levelMode = levelMode,
                                                        onBack = {
                                                            currentScreen = "level_select"
                                                            currentBottomNavRoute = BottomNavItem.Home.route
                                                            lastPlayedLevelIndex = levelIndex
                                                            lastGameExitKey++
                                                        },
                                                        onLevelComplete = {
                                                            levelCompleteCallback?.invoke()
                                                        }
                                                    )
                                                }
                                            }
                                            route == BottomNavItem.Home.route -> {
                                                site.aiok.onepic.ui.LevelSelectScreen(
                                                    scrollTriggerKey = lastGameExitKey,
                                                    lastPlayedLevelIndex = lastPlayedLevelIndex,
                                                    targetScrollLevelId = targetLevelId,
                                                    completedLevelsBeforeGame = completedLevelsBeforeGame,
                                                    cachedLevels = allLevels, // Pass hoisted data
                                                    preloadedNativeAd = homeNativeAdState, // Pass shared singleton ad
                                                    onLevelSelected = { level, index, mode, onComplete ->
                                                        targetLevelId = null
                                                        completedLevelsBeforeGame = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(this@MainActivity)
                                                        selectedLevel = level
                                                        levelIndex = index
                                                        levelMode = mode
                                                        levelCompleteCallback = onComplete
                                                        currentScreen = "game"
                                                    }
                                                )
                                            }
                                        route == BottomNavItem.Galaxy.route -> {
                                            site.aiok.onepic.ui.GalleryScreen(
                                            preloadedNativeAd = galleryNativeAdState,
                                                onLocateLevel = { id ->
                                                    targetLevelId = id
                                                    currentBottomNavRoute = BottomNavItem.Home.route
                                                    currentScreen = "level_select"
                                                }
                                            )
                                        }
                                        route == BottomNavItem.CheckIn.route -> {
                                            site.aiok.onepic.ui.CheckInScreen()
                                        }
                                        route == BottomNavItem.More.route -> {
                                            site.aiok.onepic.ui.MoreScreen(onBack = { /* Handled by bottom nav */ })
                                        }
                                    }
                                }

                                // Render Celestial Visitors at the VERY TOP layer for all nav tabs
                                if (currentScreen != "game" && currentScreen != "intro") {
                                    site.aiok.onepic.ui.components.CelestialVisitorOverlay()
                            }
                        }
                    }
                    
                    
                    // 全局全息 HUD 倒计时 (置于最顶层)
                    GlobalBuffHUD(context = this@MainActivity)
                }
            }
        }
    }
}

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val slotIndex = intent?.getIntExtra(site.aiok.onepic.utils.NotificationHelper.EXTRA_SLOT_INDEX, -1) ?: -1
        if (slotIndex != -1) {
            // 用户通过该时段的通知进入了应用，记录为偏好
            site.aiok.onepic.data.LevelProgressManager.setPreferredReminderSlot(this, slotIndex)
        }
    }
}

@Composable
fun GlobalBuffHUD(context: android.content.Context) {
    var remainingSeconds by remember { mutableStateOf(0) }
    var isWarmingUp by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(false) }
    
    // Drag state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // 定时扫描状态
    LaunchedEffect(Unit) {
        while (true) {
            isWarmingUp = LevelProgressManager.isBuffWarmingUp(context)
            isActive = LevelProgressManager.isDoubleCoinsActive(context)
            remainingSeconds = if (isWarmingUp) {
                LevelProgressManager.getWarmUpRemainingSeconds(context)
            } else if (isActive) {
                LevelProgressManager.getDoubleCoinsRemainingSeconds(context)
            } else {
                0
            }
            delay(1000)
        }
    }
    
    if (isWarmingUp || isActive) {
        val themeColor = if (isWarmingUp) Color(0xFF00B0FF) else Color(0xFFFFD700)
        val statusText = if (isWarmingUp) stringResource(R.string.hud_buff_warming) else stringResource(R.string.hud_buff_active)
        val hintText = if (isWarmingUp) stringResource(R.string.hud_buff_hint_prepare) else stringResource(R.string.hud_buff_hint_overclock)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(themeColor.copy(alpha = 0.6f), Color.Transparent, themeColor.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(themeColor.copy(alpha = 0.1f), CircleShape)
                                .border(0.5.dp, themeColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWarmingUp) Icons.Default.Info else Icons.Default.Star,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = themeColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = hintText,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                // 进度条
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(if (isWarmingUp) (remainingSeconds / 10f).coerceAtMost(1f) else (remainingSeconds / 300f).coerceAtMost(1f))
                        .height(2.dp)
                        .background(themeColor)
                )
            }
        }
    }
}
