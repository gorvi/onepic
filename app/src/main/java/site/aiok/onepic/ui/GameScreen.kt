package site.aiok.onepic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import site.aiok.onepic.R
import site.aiok.onepic.logic.ImageSlicer
import site.aiok.onepic.model.ImageSource
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.view.GameBoardView
import site.aiok.onepic.ui.components.*

@Composable
fun GameScreen(
    levelConfig: LevelConfig, 
    levelIndex: Int,
    levelMode: String,
    onBack: () -> Unit, 
    onLevelComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Prevent App Open Ads during gameplay
    DisposableEffect(Unit) {
        (context.applicationContext as? site.aiok.onepic.OnePicApplication)?.appOpenAdManager?.isGameActive = true
        onDispose {
            (context.applicationContext as? site.aiok.onepic.OnePicApplication)?.appOpenAdManager?.isGameActive = false
        }
    }
    
    var elapsedTime by remember { mutableStateOf(0) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showPuzzleButtons by remember { mutableStateOf(false) }
    var isGameComplete by remember { mutableStateOf(false) } // NEW: Persistent flag
    var completionStars by remember { mutableStateOf(0) }
    var scoreGained by remember { mutableStateOf(0) }
    var currentScore by remember { mutableStateOf(0) }
    var displayScore by remember { mutableStateOf(site.aiok.onepic.data.LevelProgressManager.getTotalMergeScore(context)) } // Show TOTAL balance
    var extraCoinsAnim by remember { mutableStateOf(0) } // For +100 animation
    var sessionCoins by remember { mutableStateOf(0) } // NEW: Explicitly track coins for this session
    
    // Tutorial Step State
    var tutorialStep by remember { mutableStateOf(0) }
    
    // Hint Logic States
    var showHintConfirmDialog by remember { mutableStateOf(false) }
    var hasFreeHint by remember { mutableStateOf(site.aiok.onepic.data.LevelProgressManager.isFreeHintAvailable(context)) }
    var targetStars by remember { mutableStateOf(0) }
    var collectedStars by remember { mutableStateOf(0) }
    
    // HUD 缩放动画状态
    val coinHudScale = remember { Animatable(1f) }
    val starHudScale = remember { Animatable(1f) }
    
    val rewardController = rememberRewardController()
    var coinTargetPos by remember { mutableStateOf(Offset.Zero) }
    var starTargetPos by remember { mutableStateOf(Offset.Zero) }
    var puzzleCenterPos by remember { mutableStateOf(Offset.Zero) }
    var visibleStars by remember { mutableStateOf(0) }
    
    var gameBoardView: GameBoardView? by remember { mutableStateOf(null) }
    var imageHeight by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }
    var imageWidth by remember { mutableStateOf<androidx.compose.ui.unit.Dp?>(null) }
    var isTutorialVisible by remember { mutableStateOf(true) }
    var isBuffActive by remember { mutableStateOf(false) }

    // 移除不稳定的 LaunchedEffect 弹窗逻辑
    
    val bestTime = remember(levelIndex, levelMode) {
        when (levelMode) {
            "classic" -> site.aiok.onepic.data.LevelProgressManager.getClassicLevelBestTime(context, levelIndex)
            "gallery" -> site.aiok.onepic.data.LevelProgressManager.getGalleryLevelBestTime(context, levelIndex)
            // Ascended doesn't track time strictly yet, but return max
            else -> Int.MAX_VALUE
        }
    }
    
    var bestStars by remember(levelIndex, levelMode) {
        mutableStateOf(
            when (levelMode) {
                "classic" -> site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, levelIndex)
                "ascended" -> site.aiok.onepic.data.LevelProgressManager.getAscendedLevelStars(context, levelIndex)
                "gallery" -> site.aiok.onepic.data.LevelProgressManager.getGalleryLevelStars(context, levelIndex)
                else -> 0
            }
        )
    }
    
    // Load Bitmap
    val bitmap = remember(levelConfig) {
        val src = levelConfig.imageSource
        try {
            when (src) {
                is ImageSource.Asset -> BitmapFactory.decodeStream(context.assets.open(src.path))
                is ImageSource.UriSource -> {
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, src.uri)
                    } else {
                        val sourceDec = ImageDecoder.createSource(context.contentResolver, src.uri)
                        ImageDecoder.decodeBitmap(sourceDec) { decoder, _, _ -> decoder.isMutableRequired = true }
                    }
                }
                is ImageSource.Resource -> BitmapFactory.decodeResource(context.resources, src.resId)
                else -> null
            }?.let { raw ->
                 val maxDim = 1024
                 val scale = if (raw.width > maxDim || raw.height > maxDim) {
                     minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
                 } else 1f
                 if (scale < 1f) Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true) else raw
            } ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) { Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888) }
    }

    val pieces = remember(levelConfig) { ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols).shuffled() }
    val formattedTime = remember(elapsedTime) { formatTime(elapsedTime) }
    
    LaunchedEffect(Unit) {
        while (true) {
            // Update buff status first to ensure UI is correct immediately
            isBuffActive = site.aiok.onepic.data.LevelProgressManager.isDoubleCoinsActive(context)
            kotlinx.coroutines.delay(1000)
            gameBoardView?.let { elapsedTime = it.getElapsedSeconds() }
        }
    }

    MeshGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            ChapterAtmosphere(theme = levelConfig.animationTheme ?: "magic")

            Column(
                modifier = Modifier.fillMaxSize().then(if (showCompleteDialog) Modifier.blur(8.dp) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayTitle = if (levelConfig.levelId == "c1") "Level S" else if (levelConfig.isAscended) "Level $levelIndex+" else "Level $levelIndex"
                GlassTopBar(title = displayTitle, onBack = onBack, showBackground = false, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(icon = "⏱", value = formattedTime)
                    Box(modifier = Modifier.weight(1f)) {
                        StatChip(
                            icon = "🪙", 
                            value = displayScore.toString(), 
                            scale = coinHudScale.value,
                            isDouble = isBuffActive,
                            modifier = Modifier.onGloballyPositioned { coinTargetPos = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) }
                        )
                    }
                    StarChip(
                        bestStars = if (showCompleteDialog || targetStars > 0) visibleStars else bestStars, 
                        scale = starHudScale.value,
                        modifier = Modifier.onGloballyPositioned { starTargetPos = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    )
                }
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
                    val puzzleShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .then(if (imageHeight != null && imageWidth != null) Modifier.size(imageWidth!!, imageHeight!!) else Modifier.fillMaxSize())
                            .shadow(12.dp, puzzleShape).background(Color.White.copy(alpha = 0.05f), puzzleShape).border(2.dp, Color.White.copy(alpha = 0.5f), puzzleShape).clip(puzzleShape)
                            .onGloballyPositioned { puzzleCenterPos = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    ) {
                    AndroidView(
                        factory = { ctx: android.content.Context ->
                            GameBoardView(ctx).apply {
                                val isTutorial = levelConfig.levelId == "tutorial_0" || levelConfig.levelId == "c1"
                                
                                // Custom Shuffle for Level 0: 1, 4, 2, 3
                                // IDs: 1->0, 2->1, 3->2, 4->3
                                // Visual order: ID0, ID3, ID1, ID2
                                @Suppress("UNCHECKED_CAST")
                                val typedPieces = pieces as List<site.aiok.onepic.model.PuzzlePiece>
                                
                                val tutorialPieces = if (levelConfig.levelId == "tutorial_0" && typedPieces.size == 4) {
                                    // P1: ID 0, P2: ID 1, P3: ID 2, P4: ID 3
                                    // Desired order: 1, 4, 2, 3
                                    val map = typedPieces.associateBy { it.id }
                                    listOfNotNull(map[0], map[3], map[1], map[2])
                                } else {
                                    typedPieces
                                }
                                
                                // CRITICAL: Set tutorial mode BEFORE setPieces so ID mapping works!
                                isTutorialMode = isTutorial
                                setPieces(tutorialPieces)
                                gameBoardView = this
                                
                                onTutorialStepCompleted = { step ->
                                    tutorialStep = step
                                }

                                    onContentSizeChanged = { w, h ->
                                        val newW = with(density) { w.toDp() }
                                        val newH = with(density) { h.toDp() }
                                        if (newW > 0.dp && (imageWidth != newW || imageHeight != newH)) {
                                            imageWidth = newW
                                            imageHeight = newH
                                        }
                                    }
                                    
                                    onScoreChange = { gain, coinGain -> 
                                        // Only hide tutorial after double-tap instruction has been shown
                                        if (tutorialStep >= 1) {
                                            isTutorialVisible = false
                                        }
                                        
                                        // 检查双倍 Buff 状态
                                        val isBuffActive = LevelProgressManager.isDoubleCoinsActive(context)
                                        
                                        // ⚠️ 关键修复: LevelProgressManager.saveTotalMergeScore 内部已经处理了 Buff 的翻倍逻辑
                                        // 所以这里不应该再手动乘 2，否则会变成 4 倍金币。
                                        // 我们只需要计算用于 UI 展示的 actualGain 和 actualCoinGain
                                        
                                        val multiplier = if (isBuffActive) 2 else 1
                                        val actualGain = gain * multiplier
                                        val actualCoinGain = coinGain * multiplier
                                        


                                        currentScore += actualGain
                                        // 内部会根据 Buff 状态自动翻倍并缓存
                                        LevelProgressManager.saveTotalMergeScore(context, gain) 
                                        
                                        if (actualCoinGain > 0) {
                                            sessionCoins += actualCoinGain // Track session total immediately
                                            // 实时金币动效
                                            val rewardType = if (isBuffActive) RewardType.COIN_X2 else RewardType.COIN
                                            rewardController.emit(rewardType, actualCoinGain, puzzleCenterPos, coinTargetPos) {
                                                displayScore += 1 // Visually increment total
                                                // 撞击缩放动效
                                                scope.launch {
                                                    repeat(2) {
                                                        coinHudScale.animateTo(1.5f, tween(100, easing = FastOutSlowInEasing))
                                                        coinHudScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    onPuzzleComplete = { timeInSeconds ->
                                        elapsedTime = timeInSeconds
                                        completionStars = calculateStars(timeInSeconds, levelConfig.rows, levelConfig.cols, bestTime)
                                        // Ascended logic handled in LevelSelect
                                        // if (ascendedBitmap != null) { showAscendedImage = true }
                                        if (levelMode == "classic") {
                                            site.aiok.onepic.data.LevelProgressManager.saveClassicLevelBestTime(context, levelIndex, timeInSeconds)
                                            val currentBest = site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, levelIndex)
                                            if (completionStars > currentBest) { site.aiok.onepic.data.LevelProgressManager.saveClassicLevelStars(context, levelIndex, completionStars); site.aiok.onepic.data.LevelProgressManager.saveTotalStars(context, completionStars - currentBest); bestStars = completionStars }
                                        } else if (levelMode == "ascended") {
                                            // Ascended Mode: levelIndex is the ID
                                            val currentBest = site.aiok.onepic.data.LevelProgressManager.getAscendedLevelStars(context, levelIndex)
                                            if (completionStars > currentBest) { 
                                                site.aiok.onepic.data.LevelProgressManager.saveAscendedLevelStars(context, levelIndex, completionStars)
                                                // Ascended stars count towards total? Yes.
                                                site.aiok.onepic.data.LevelProgressManager.saveTotalStars(context, completionStars - currentBest)
                                                bestStars = completionStars 
                                            }
                                        } else {
                                            site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelBestTime(context, levelIndex, timeInSeconds)
                                            val currentBest = site.aiok.onepic.data.LevelProgressManager.getGalleryLevelStars(context, levelIndex)
                                            if (completionStars > currentBest) { site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelStars(context, levelIndex, completionStars); site.aiok.onepic.data.LevelProgressManager.saveTotalStars(context, completionStars - currentBest); bestStars = completionStars }
                                        }
                                        
                                        
                                        // 更鲁棒的 Demo 判定：星星为0 OR ID匹配教程/演示前缀
                                        val isDemo = completionStars == 0 || 
                                                     levelConfig.levelId.startsWith("tutorial") || 
                                                     levelConfig.levelId.startsWith("demo") ||
                                                     levelConfig.levelId == "c1"

                                        android.util.Log.d("InternalDiag", "onPuzzleComplete triggered. levelId=${levelConfig.levelId}, stars=$completionStars -> isDemo=$isDemo")
                                        
                                        scoreGained = sessionCoins
                                        visibleStars = 0
                                        collectedStars = 0
                                        isGameComplete = true
                                        
                                        targetStars = if (isDemo) 1 else completionStars
                                        android.util.Log.d("InternalDiag", "Final decision: targetStars=$targetStars, isDemo=$isDemo")
                                        android.util.Log.d("InternalDiag", "Set targetStars=$targetStars, isGameComplete=$isGameComplete")
                                        
                                        if (isDemo) {
                                            android.util.Log.d("InternalDiag", "Demo path: Emitting 1 dummy star")
                                            // Demo 关卡：直接发射一颗星星，并启动延迟后显示对话框
                                            rewardController.emit(RewardType.STAR, 1, puzzleCenterPos, starTargetPos) {
                                                visibleStars = 1
                                                collectedStars = 1
                                                android.util.Log.d("InternalDiag", "Dummy star emission finished")
                                            }
                                            
                                            scope.launch {
                                                android.util.Log.d("InternalDiag", "Demo delay starting...")
                                                kotlinx.coroutines.delay(1000) 
                                                android.util.Log.d("InternalDiag", "Demo delay ended. Setting showCompleteDialog = true")
                                                showCompleteDialog = true
                                                showPuzzleButtons = true
                                            }
                                        } else {
                                            android.util.Log.d("InternalDiag", "Normal path: Emitting $completionStars stars")
                                            // 普通关卡：发射星星，并在延迟后处理弹窗（确保星星动画基本完成）
                                            rewardController.emit(RewardType.STAR, completionStars, puzzleCenterPos, starTargetPos) { 
                                                visibleStars++
                                                collectedStars++
                                                scope.launch {
                                                    repeat(2) {
                                                        starHudScale.animateTo(1.5f, tween(100, easing = FastOutSlowInEasing))
                                                        starHudScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                                                    }
                                                }
                                            }
                                            
                                            scope.launch {
                                                android.util.Log.d("InternalDiag", "Normal delay starting...")
                                                kotlinx.coroutines.delay(1200) 
                                                android.util.Log.d("InternalDiag", "Normal delay ended. Checking ads...")
                                                if ((levelIndex + 1) % 3 == 0) {
                                                    (context as? android.app.Activity)?.let { activity ->
                                                        site.aiok.onepic.logic.AdManager.showInterstitial(activity) {
                                                            android.util.Log.d("InternalDiag", "Ad finished. Setting showCompleteDialog = true")
                                                            showCompleteDialog = true
                                                            showPuzzleButtons = true
                                                        }
                                                    } ?: run {
                                                        showCompleteDialog = true
                                                        showPuzzleButtons = true
                                                    }
                                                } else {
                                                    android.util.Log.d("InternalDiag", "No ad needed. Setting showCompleteDialog = true")
                                                    showCompleteDialog = true
                                                    showPuzzleButtons = true
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                    if ((levelConfig.levelId == "tutorial_0" || levelConfig.levelId == "c1") && !showPuzzleButtons && isTutorialVisible) {
                        TutorialOverlay(
                            step = tutorialStep,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Controls Area (Stable Layout)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(80.dp) // Fixed height to prevent layout jumps
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    // Gameplay Controls (Visible when NOT complete)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !showPuzzleButtons,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                         Row(modifier = Modifier.wrapContentSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            FloatingGameButton(
                                icon = Icons.Default.Info, 
                                color = Color(0xFF8E24AA), 
                                label = if (hasFreeHint) "FREE" else "-100", // Dynamic label
                                onClick = { 
                                    if (hasFreeHint) {
                                        // Use Free Hint Immediately
                                        site.aiok.onepic.data.LevelProgressManager.useFreeHint(context)
                                        hasFreeHint = false
                                        gameBoardView?.showHint()
                                        android.widget.Toast.makeText(context, context.getString(R.string.hint_use_free), android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Ask for payment
                                        showHintConfirmDialog = true
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            val canWatch = site.aiok.onepic.data.LevelProgressManager.canWatchAd(context)
                            val cooldownRemaining = site.aiok.onepic.data.LevelProgressManager.getAdCooldownRemaining(context)
                            val adReward = site.aiok.onepic.data.LevelProgressManager.calculateAdReward(context)
                            val buttonLabel = if (canWatch) "+$adReward" else "${cooldownRemaining / 60}:${String.format("%02d", cooldownRemaining % 60)}"
                            FloatingGameButton(
                                icon = Icons.Default.PlayArrow, // Video icon proxy
                                color = if (canWatch) Color(0xFF00C853) else Color.Gray,
                                label = buttonLabel,
                                onClick = {
                                    if (!site.aiok.onepic.data.LevelProgressManager.canWatchAd(context)) {
                                        val remaining = site.aiok.onepic.data.LevelProgressManager.getAdCooldownRemaining(context)
                                        android.widget.Toast.makeText(context, context.getString(R.string.ad_wait_cooldown, remaining / 60, remaining % 60), android.widget.Toast.LENGTH_SHORT).show()
                                        return@FloatingGameButton
                                    }
                                    val reward = site.aiok.onepic.data.LevelProgressManager.calculateAdReward(context)
                                    android.widget.Toast.makeText(context, context.getString(R.string.ad_reward_toast, reward), android.widget.Toast.LENGTH_SHORT).show()
                                    (context as? android.app.Activity)?.let { activity ->
                                        site.aiok.onepic.logic.AdManager.showRewarded(
                                            activity,
                                            onUserEarnedReward = {
                                                // Record timestamp, increment view count and add diminishing reward
                                                site.aiok.onepic.data.LevelProgressManager.recordAdView(context)
                                                site.aiok.onepic.data.LevelProgressManager.incrementDailyAdViews(context)
                                                site.aiok.onepic.data.LevelProgressManager.addCoins(context, reward)
                                                displayScore += reward
                                                // 播放获得金币动效
                                                rewardController.emit(RewardType.COIN, reward, puzzleCenterPos, coinTargetPos) {}
                                                android.widget.Toast.makeText(context, context.getString(R.string.ad_reward_received, reward), android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onAdClosed = {
                                                // Resume game or music if needed
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Completion Controls (Visible when complete)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPuzzleButtons,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut()
                    ) {
                        Row(modifier = Modifier.wrapContentSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            FloatingGameButton(icon = Icons.Default.Refresh, color = Color(0xFF4A90E2), onClick = { 
                                showCompleteDialog = false
                                showPuzzleButtons = false
                                isGameComplete = false // Reset
                                currentScore = 0
                                displayScore = 0
                                targetStars = 0
                                collectedStars = 0
                                gameBoardView?.setPieces(ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols).shuffled()) 
                            })
                            Spacer(modifier = Modifier.width(32.dp))
                            FloatingGameButton(icon = Icons.Default.ArrowForward, color = Color(0xFFFF6B35), isPrimary = true, onClick = { 
                                showCompleteDialog = false
                                showPuzzleButtons = false
                                isGameComplete = false // Reset
                                onBack() 
                            }) 
                        }
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
            if (showCompleteDialog) {
                val finalIsDemo = completionStars == 0 || 
                                  levelConfig.levelId.startsWith("tutorial") || 
                                  levelConfig.levelId.startsWith("demo") ||
                                  levelConfig.levelId == "c1"

                android.util.Log.d("InternalDiag", "Dialog rendering. levelId=${levelConfig.levelId}, finalIsDemo=$finalIsDemo")
                LevelCompleteDialog(
                    stars = if (finalIsDemo) 0 else completionStars,
                    isDemoLevel = finalIsDemo,
                    guideHint = if (levelMode == "ascended" && finalIsDemo) stringResource(R.string.guide_chapter1_hint) else null,
                    timeInSeconds = elapsedTime, 
                    scoreGained = scoreGained, 
                    levelTitle = levelConfig.title, 
                    storyText = levelConfig.storyText, 
                    onDoubleReward = {
                        (context as? android.app.Activity)?.let { activity ->
                            // Use the dedicated double-reward ad ID
                            site.aiok.onepic.logic.AdManager.showRewarded(
                                activity,
                                onUserEarnedReward = {
                                    // Grant bonus coins equal to original score
                                    site.aiok.onepic.data.LevelProgressManager.addCoins(context, scoreGained)
                                    displayScore += scoreGained
                                    rewardController.emit(RewardType.COIN, scoreGained, puzzleCenterPos, coinTargetPos) {}
                                    android.widget.Toast.makeText(context, "2X Reward! +$scoreGained 🪙", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onAdClosed = {}
                            )
                        }
                    },
                    onDismiss = { 
                        showCompleteDialog = false
                        scoreGained = 0
                        // 核心：在对话框关闭时再调用外部通知。这能确保在保存进度、切换导航状态前，对话框能被用户看到。
                        onLevelComplete()
                    }
                )
            }
            
            if (showHintConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showHintConfirmDialog = false },
                    title = { Text(stringResource(R.string.hint_confirm_title), fontWeight = FontWeight.Bold) },
                    text = { Text(stringResource(R.string.hint_confirm_msg, displayScore)) },
                    confirmButton = {
                        TextButton(onClick = {
                            if (site.aiok.onepic.data.LevelProgressManager.consumeCoins(context, 100)) {
                                displayScore -= 100
                                gameBoardView?.showHint()
                                showHintConfirmDialog = false
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.hint_not_enough), android.widget.Toast.LENGTH_SHORT).show()
                                showHintConfirmDialog = false
                            }
                        }) { Text(stringResource(R.string.hint_confirm_yes), fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showHintConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
                    },
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    textContentColor = Color.Black.copy(0.8f)
                )
            }

            RewardAnimationOverlay(controller = rewardController)
        }
    }
}

@Composable
fun TutorialOverlay(step: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "tut")
    val alpha by transition.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    val slideY by transition.animateFloat(0f, 40f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    val step0TextPart1 = stringResource(R.string.tut_welcome)
    val step0TextPart2 = stringResource(R.string.tut_step_1)
    val step1Text = stringResource(R.string.tut_step_double_tap)
    
    val currentText = if (step == 0) "🚀 $step0TextPart1 👉 $step0TextPart2" else "👆 $step1Text"
    
    Column(modifier = modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer { this.alpha = alpha }
                .background(Color.Black.copy(0.7f), RoundedCornerShape(20.dp))
                .border(2.dp, Color.Cyan.copy(0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
             Text(
                 text = currentText, 
                 color = Color.White, 
                 fontSize = 16.sp, 
                 fontWeight = FontWeight.Bold,
                 textAlign = TextAlign.Center
             )
        }
        if (step == 0) {
            Spacer(modifier = Modifier.height(20.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Cyan.copy(alpha), modifier = Modifier.size(48.dp).graphicsLayer { this.translationY = slideY })
        }
    }
}

@Composable
fun StatChip(icon: String, value: String, scale: Float = 1f, isDouble: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(42.dp).graphicsLayer { scaleX = scale; scaleY = scale }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp, modifier = Modifier.graphicsLayer { alpha = 0.8f })
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp), color = Color.White)
            if (isDouble) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "2X",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StarChip(bestStars: Int, scale: Float = 1f, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(42.dp).graphicsLayer { scaleX = scale; scaleY = scale }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i -> Icon(Icons.Default.Star, null, tint = if (i < bestStars) Color(0xFFFFD700) else Color.White.copy(0.15f), modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
fun FloatingGameButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String? = null, isPrimary: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val transition = rememberInfiniteTransition(label = "btn")
    val pulse by transition.animateFloat(1f, if (isPrimary) 1.08f else 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val scale by animateFloatAsState(if (pressed) 0.85f else pulse, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "s")
    Box(modifier = Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }.shadow(if (isPrimary) 20.dp else 12.dp, CircleShape, spotColor = color.copy(if (isPrimary) 0.8f else 0.4f)).background(if (isPrimary) Brush.verticalGradient(listOf(color.copy(0.9f), color, color.copy(0.8f))) else Brush.verticalGradient(listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))), CircleShape).then(if (!isPrimary) Modifier.blur(0.5.dp) else Modifier).border(2.dp, Brush.linearGradient(listOf(Color.White.copy(0.8f), Color.White.copy(0.2f), color.copy(0.5f))), CircleShape).clip(CircleShape).clickable(interactionSource = source, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        if (isPrimary) Box(Modifier.fillMaxSize().padding(4.dp).border(1.dp, Color.White.copy(0.3f), CircleShape))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (isPrimary) Color.White else color.copy(0.9f), modifier = Modifier.size(28.dp)) // Slightly smaller icon
            if (label != null) {
                Text(
                    text = label, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isPrimary) Color.White.copy(0.9f) else color.copy(0.8f)
                )
            }
        }
    }
}
