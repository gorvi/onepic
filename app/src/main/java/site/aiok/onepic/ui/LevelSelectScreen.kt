package site.aiok.onepic.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.aiok.onepic.data.LevelRepository
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.components.MeshGradientBackground
import site.aiok.onepic.ui.components.getChapterColors
import site.aiok.onepic.ui.components.*

import site.aiok.onepic.R
import androidx.compose.ui.res.stringResource

// 全局共享动画状态 - 避免每个组件创建独立动画
data class SharedAnimationState(
    val pulseScale: Float,
    val pulseAlpha: Float,
    val floatOffset: Float,
    val breatheScale: Float
)

// 自定义平滑缓冲滑动效果
@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    return remember(decayAnimationSpec) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(
                initialVelocity: Float
            ): Float {
                val adjustedVelocity = initialVelocity * 0.75f
                if (abs(adjustedVelocity) < 50f) return adjustedVelocity
                var lastValue = 0f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = adjustedVelocity,
                ).animateDecay(decayAnimationSpec) {
                    val delta = value - lastValue
                    lastValue = value
                    val consumed = scrollBy(delta)
                    if (abs(delta - consumed) > 0.5f) {
                        cancelAnimation()
                    }
                }
                return 0f 
            }
        }
    }
}

@Composable
fun rememberSharedAnimations(): SharedAnimationState {
    val infiniteTransition = rememberInfiniteTransition(label = "shared_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_alpha"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "float_offset"
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe_scale"
    )
    return SharedAnimationState(pulseScale, pulseAlpha, floatOffset, breatheScale)
}

@Composable
fun LevelSelectScreen(
    currentStageIndex: Int = 0,
    scrollTriggerKey: Int = 0,
    lastPlayedLevelIndex: Int? = null,
    targetScrollLevelId: Int? = null,
    completedLevelsBeforeGame: Set<Int> = emptySet(),
    cachedLevels: List<LevelConfig>, // Performance: Pass pre-loaded levels
    onLevelSelected: (LevelConfig, Int, String, () -> Unit) -> Unit,
    preloadedNativeAd: com.google.android.gms.ads.nativead.NativeAd? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Performance: Use passed cachedLevels directy
    val allLevels = cachedLevels
    
    var unlockedLevels: Set<Int> by remember { 
        mutableStateOf(
            if (completedLevelsBeforeGame.isNotEmpty()) {
                // Reconstruct "Old" unlocked state: Completed levels + their next levels (and level 0)
                completedLevelsBeforeGame.flatMap { setOf(it, it + 1) }.toSet() + 0
            } else {
                site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(context)
            }
        ) 
    }
    var renderingUnlockedLevels: Set<Int> by remember { mutableStateOf(unlockedLevels) }
    var completedLevels: Set<Int> by remember { 
        mutableStateOf(
            if (completedLevelsBeforeGame.isNotEmpty()) {
                completedLevelsBeforeGame
            } else {
                site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
            }
        )
    }

    var unlockedAscendedIds by remember { mutableStateOf(site.aiok.onepic.data.LevelProgressManager.getUnlockedAscendedLevels(context)) }
    var completedAscendedIds by remember { mutableStateOf(site.aiok.onepic.data.LevelProgressManager.getCompletedAscendedLevels(context)) }

    val onUnlockLevel: (Int) -> Unit = { id -> unlockedLevels = unlockedLevels + id }
    val onCompleteLevel: (Int) -> Unit = { id -> completedLevels = completedLevels + id }
    val onCompleteAscendedLevel: (Int) -> Unit = { id -> completedAscendedIds = completedAscendedIds + id }
    
    var shouldDelayUpdateState by remember { mutableStateOf(false) }
    var pendingCompletedLevels by remember { mutableStateOf<Set<Int>?>(null) }
    var pendingUnlockedLevels by remember { mutableStateOf<Set<Int>?>(null) }
    var newlyCompletedLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isProcessingReturn by remember { mutableStateOf(false) }
    
    // Performance: Load stats asynchronously to avoid main thread IO block
    val totalMergeScore by produceState(initialValue = 0, key1 = scrollTriggerKey) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            value = site.aiok.onepic.data.LevelProgressManager.getTotalMergeScore(context)
        }
    }
    val totalStars by produceState(initialValue = 0, key1 = scrollTriggerKey) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            value = site.aiok.onepic.data.LevelProgressManager.getTotalStars(context)
        }
    }
    
    val nextToPlayIndex = remember(unlockedLevels, completedLevels) {
        allLevels.indices.firstOrNull { index ->
            val isUnlocked = index in unlockedLevels || index == 0
            val isCompleted = index in completedLevels
            isUnlocked && !isCompleted
        } ?: 0
    }

    val levelHeightPx = with(density) { 152.dp.toPx().toInt() }
    val centerOffset = (screenHeightPx - levelHeightPx) / 2
    
    // Use a stable initial index that doesn't change during ON_RESUME Refresh
    val initialJumpIndex = remember { nextToPlayIndex }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialJumpIndex,
        initialFirstVisibleItemScrollOffset = (-centerOffset).toInt()
    )

    val initialScrollIndex = nextToPlayIndex
    var previousNextToPlayIndex by remember { mutableStateOf(nextToPlayIndex) }
    
    var hasInitialScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(allLevels.size) {
        if (allLevels.size > 1 && !hasInitialScrolled) {
            // Give a tiny delay for layout to stabilize
            kotlinx.coroutines.delay(100)
            listState.scrollToItem(
                index = nextToPlayIndex,
                scrollOffset = (-centerOffset).toInt()
            )
            hasInitialScrolled = true
        }
    }
    
    var lastScrolledToIndex by remember { mutableStateOf(initialScrollIndex) }
    var shouldAnimateScroll by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(targetScrollLevelId) {
        targetScrollLevelId?.let { id ->
            val lookupId = if (id > 60) id - 60 else id
            val index = allLevels.indexOfFirst { 
                val numericId = it.levelId.filter { ch -> ch.isDigit() }.toIntOrNull()
                numericId == lookupId
            }
            if (index != -1) {
                listState.scrollToItem((index - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(nextToPlayIndex) {
        if (nextToPlayIndex != previousNextToPlayIndex) {
            kotlinx.coroutines.delay(1200)
            previousNextToPlayIndex = nextToPlayIndex
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val latestUnlockedFromStorage = site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(context)
                if (scrollTriggerKey == 0) {
                     unlockedLevels = latestUnlockedFromStorage
                     completedLevels = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
                     if (!shouldDelayUpdateState) {
                        renderingUnlockedLevels = latestUnlockedFromStorage
                     }
                } else {
                    // When scrollTriggerKey is active, we FREEZE state updates here
                    // Handling is delegated to LaunchedEffect(scrollTriggerKey)
                    isProcessingReturn = true
                }
                unlockedAscendedIds = site.aiok.onepic.data.LevelProgressManager.getUnlockedAscendedLevels(context)
                completedAscendedIds = site.aiok.onepic.data.LevelProgressManager.getCompletedAscendedLevels(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val sharedAnim = rememberSharedAnimations()
    val smoothFlingBehavior = rememberSmoothFlingBehavior()
    
    val scrollToCurrentLevel: () -> Unit = {
        coroutineScope.launch {
            listState.animateScrollToItem(
                index = nextToPlayIndex,
                scrollOffset = (-centerOffset).toInt()
            )
        }
    }

    val currentStageIndex by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val middleItem = visibleItems[visibleItems.size / 2]
                val index = middleItem.index
                if (index == 0) 0 else (index - 1) / 5 + 1
            }
        }
    }
    
    LaunchedEffect(scrollTriggerKey) {
        if (scrollTriggerKey > 0) {
            // 1. Identify what happened (new progress vs replay)
            val latestCompletedFromStorage = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
            val latestUnlockedFromStorage = site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(context)
            val newCompletions = latestCompletedFromStorage - completedLevelsBeforeGame
            
            // 2. Decide Target Scroll Position IMMEDIATELY
            // Use current index as fallback for replays, or next index for progress
            val currentFruitierIndex = allLevels.indices.firstOrNull { index ->
                val isUnlocked = index in latestUnlockedFromStorage || index == 0
                val isCompleted = index in latestCompletedFromStorage
                isUnlocked && !isCompleted
            } ?: 0

            var targetIndex = currentFruitierIndex
            // Only override if we didn't just complete a new level (Replay logic)
            if (newCompletions.isEmpty() && lastPlayedLevelIndex != null && lastPlayedLevelIndex < currentFruitierIndex) {
                 // Replay case: Stay on what we just played
                 targetIndex = lastPlayedLevelIndex
            }

            // 3. Handle State Updates
            if (newCompletions.isNotEmpty()) {
                // Progress case: show animation sequence
                completedLevels = completedLevelsBeforeGame
                val beforeGameUnlocked = completedLevelsBeforeGame.map { it + 1 }.toSet() + 0
                unlockedLevels = beforeGameUnlocked
                renderingUnlockedLevels = beforeGameUnlocked
                
                pendingCompletedLevels = latestCompletedFromStorage
                pendingUnlockedLevels = latestUnlockedFromStorage
                newlyCompletedLevels = newCompletions
                shouldDelayUpdateState = true
            } else {
                // Replay case: swift update
                completedLevels = latestCompletedFromStorage
                unlockedLevels = latestUnlockedFromStorage
                newlyCompletedLevels = emptySet()
            }
            
            // 4. Force synchronization of composition before scrolling
            kotlinx.coroutines.yield() 
            
            // 5. Atomic Snap + Anim
            try {
                if (newCompletions.isNotEmpty() && lastPlayedLevelIndex != null) {
                     // Progress Mode: Visual Journey (Old -> New)
                     // 1. Start at the level we just finished
                     listState.scrollToItem(
                         index = lastPlayedLevelIndex,
                         scrollOffset = (-centerOffset).toInt()
                     )
                     
                     // 2. Brief pause to let user recognize "Oh, here I am"
                     kotlinx.coroutines.delay(300)
                     
                     // 3. Smoothly travel to the new frontier
                     listState.animateScrollToItem(
                         index = targetIndex,
                         scrollOffset = (-centerOffset).toInt()
                     )
                } else {
                    // Replay/Init Mode: Instantly appear at target
                    listState.scrollToItem(
                        index = targetIndex,
                        scrollOffset = (-centerOffset).toInt()
                    )
                    // Micro-adjust alignment
                    listState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = (-centerOffset).toInt()
                    )
                }
            } catch (e: Exception) {}
            
            // 6. Release UI Freeze
            kotlinx.coroutines.delay(200) // Small stability buffer
            isProcessingReturn = false
            
            // 7. Finalize state
            if (newCompletions.isEmpty()) {
                renderingUnlockedLevels = latestUnlockedFromStorage
            }
        }
    }
    
    // Start unlock animation sequence when pending data is ready
    LaunchedEffect(pendingUnlockedLevels) {
        if (pendingUnlockedLevels != null && pendingCompletedLevels != null) {
            // 1. Wait for scroll animation (triggered by scrollTriggerKey) to settle
            kotlinx.coroutines.delay(400)
            
            // 2. silently update the logical state (checkmark appears on previous level)
            completedLevels = pendingCompletedLevels!!
            unlockedLevels = pendingUnlockedLevels!! 
            
            // 3. Wait for simple user recognition of "Locked" state
            kotlinx.coroutines.delay(1200) 
            
            // 4. Trigger visual unlock (Explosion!)
            renderingUnlockedLevels = pendingUnlockedLevels!!
            
            // 5. Cleanup
            pendingCompletedLevels = null
            pendingUnlockedLevels = null
            shouldDelayUpdateState = false
        }
    }

    val atmosphereTheme = remember(currentStageIndex) {
        when (currentStageIndex) {
            0 -> "light"
            1 -> "nature"
            2 -> "abyss"
            3 -> "fire"
            4 -> "magic"
            5 -> "ruins"
            6 -> "ice"
            7 -> "mechanical"
            8 -> "time"
            9 -> "cosmos"
            10 -> "void"
            11 -> "light"
            12 -> "unity"
            else -> "magic"
        }
    }
    
    val chapterColors: List<Color> = remember<List<Color>>(currentStageIndex) { getChapterColors(currentStageIndex) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Z-Index 0: Background Layer
        MeshGradientBackground(colors = chapterColors) {
            ChapterAtmosphere(
                theme = atmosphereTheme, 
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }
            )
        }

        // Z-Index 1: Main Content Layer
        Column(modifier = Modifier.fillMaxSize()) {
            // Content
            val levelsCount = allLevels.size
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                         // Use graphicsLayer here to push this content to its own layer
                         // This can help isolate this fast-scrolling part from the background
                         clip = true
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 100.dp, bottom = 180.dp),
                flingBehavior = smoothFlingBehavior,
                reverseLayout = true
            ) {
                items(allLevels.size) { index ->
                    val level = allLevels[index]
                    
                    // Show level node
                    LevelRow(
                        idx = index,
                        level = level,
                        unlockedLevels = renderingUnlockedLevels,
                        completedLevels = completedLevels,
                        completedAscendedIds = completedAscendedIds,
                        newlyCompletedLevels = newlyCompletedLevels,
                        previousNextToPlayIndex = previousNextToPlayIndex,
                        sharedAnim = sharedAnim,
                        onLevelSelected = onLevelSelected,
                        onCompleteLevel = onCompleteLevel,
                        onUnlockLevel = onUnlockLevel,
                        onCompleteAscendedLevel = onCompleteAscendedLevel
                    )

                    // Every 5 levels, insert a native ad (skipping demo level index 0)
                    if (index % 5 == 0 && index > 0 && index != allLevels.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        site.aiok.onepic.ui.components.NativeAdView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            preloadedNativeAd = preloadedNativeAd,
                            loadDelayMillis = if (preloadedNativeAd != null) 0L else 1500L
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                item {
                    val lastIndex = levelsCount - 1
                    val lastOffsetX = remember(lastIndex) {
                        when (lastIndex % 4) {
                            0 -> 0.dp; 1 -> 60.dp; 2 -> 0.dp; 3 -> (-60).dp; else -> 0.dp
                        }
                    }
                    val comingSoonOffsetX = remember(levelsCount) {
                        when (levelsCount % 4) {
                            0 -> 0.dp; 1 -> 60.dp; 2 -> 0.dp; 3 -> (-60).dp; else -> 0.dp
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (levelsCount > 0) {
                            PathConnector(fromOffset = lastOffsetX, toOffset = comingSoonOffsetX, isCompleted = completedLevels.contains(lastIndex), stageIndex = 12)
                        }
                        ComingSoonNode(offsetX = comingSoonOffsetX, sharedAnim = sharedAnim, stageIndex = 12)
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
        
        // Z-Index 10: HUD Layer (Absolute positioning on top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "🪙", style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
                    Text(text = totalMergeScore.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 20.sp), color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                    Text(text = totalStars.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 20.sp), color = Color.White)
                }
            }
        }

        // 3. FAB Layer Visibility Logic
        val scrollInfo by remember {
            derivedStateOf {
                val firstVisible = listState.firstVisibleItemIndex
                val distance = abs(firstVisible - nextToPlayIndex)
                val isBelow = firstVisible > nextToPlayIndex
                Triple(distance > 3, isBelow, firstVisible)
            }
        }
        val (showFab, isBelowTarget, _) = scrollInfo

        // Z-Index 1000: UI Freeze Overlay (Hides flickering during game return)
        if (isProcessingReturn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1000f)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f))
            )
        }

        // Z-Index 100: Top Layer FAB (Isolated from all Column/Background logic)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            contentAlignment = Alignment.BottomStart
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.padding(start = 16.dp).align(Alignment.CenterStart) // Left edge center
            ) {
                // Directional icon based on scroll position relative to target
                val fabIcon = if (isBelowTarget) Icons.Default.KeyboardDoubleArrowDown else Icons.Default.KeyboardDoubleArrowUp
                
                // Tech FAB
                TechFloatingActionButton(
                    onClick = scrollToCurrentLevel,
                    icon = fabIcon, 
                    contentDescription = stringResource(R.string.cd_scroll_to_current),
                )
            }
        }
    }
}
