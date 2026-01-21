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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.aiok.onepic.data.LevelRepository
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.components.MeshGradientBackground

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
                // 降低初始速度，使滑动更平滑、更有阻尼感
                val adjustedVelocity = initialVelocity * 0.75f
                
                if (abs(adjustedVelocity) < 50f) return adjustedVelocity
                
                var velocityLeft = adjustedVelocity
                var lastValue = 0f
                
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = adjustedVelocity,
                ).animateDecay(decayAnimationSpec) {
                    val delta = value - lastValue
                    lastValue = value
                    val consumed = scrollBy(delta)
                    
                    // 如果滚动被消耗的量与期望不同，说明到达边界
                    if (abs(delta - consumed) > 0.5f) {
                        cancelAnimation()
                    }
                    velocityLeft = velocity
                }
                
                return velocityLeft
            }
        }
    }
}

@Composable
fun rememberSharedAnimations(): SharedAnimationState {
    val infiniteTransition = rememberInfiniteTransition(label = "shared_anim")
    
    // 脉冲缩放 - 更大的范围，更快的速度
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,  // 扩大到1.35倍
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),  // 加快速度
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_scale"
    )
    
    // 脉冲透明度 - 更明显的变化
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,  // 起始更亮
        targetValue = 0.1f,   // 结束更淡
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
    scrollTriggerKey: Int = 0, // 用于触发重新检查滚动的 key
    completedLevelsBeforeGame: Set<Int> = emptySet(), // 进入游戏前的 completedLevels
    onLevelSelected: (LevelConfig, Int, String, onComplete: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    // 合并 Classic 和 Gallery 关卡
    val allLevels = remember {
        LevelRepository.classicLevels + LevelRepository.getGalleryLevels(context)
    }
    
    // 从持久化存储加载解锁和完成状态
    // 如果 completedLevelsBeforeGame 不为空，说明从游戏返回，先使用完成前的状态
    var unlockedLevels by remember { 
        mutableStateOf(site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(context)) 
    }
    var completedLevels by remember { 
        mutableStateOf(
            if (completedLevelsBeforeGame.isNotEmpty()) {
                // 从游戏返回，先使用完成前的状态
                completedLevelsBeforeGame
            } else {
                // 首次进入，使用最新状态
                site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
            }
        )
    }
    
    // 标记是否应该延迟更新状态（用于显示动画效果）
    var shouldDelayUpdateState by remember { mutableStateOf(false) }
    var pendingCompletedLevels by remember { mutableStateOf<Set<Int>?>(null) }
    var pendingUnlockedLevels by remember { mutableStateOf<Set<Int>?>(null) }
    
    // 追踪刚刚完成的关卡（用于触发对号动画）
    var newlyCompletedLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    // 获取总合并得分（游戏中合并拼图块获得的分数）
    val totalMergeScore = remember {
        site.aiok.onepic.data.LevelProgressManager.getTotalMergeScore(context)
    }
    
    // 获取总星星数
    val totalStars = remember {
        site.aiok.onepic.data.LevelProgressManager.getTotalStars(context)
    }
    
    // 获取最后玩的关卡，作为初始滚动位置
    val initialScrollIndex = remember {
        site.aiok.onepic.data.LevelProgressManager.getLastPlayedLevel(context)
            .coerceIn(0, allLevels.size - 1)
    }
    
    // 使用初始滚动位置创建 listState，这样启动时就直接显示最后玩的关卡，不需要动画
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex
    )
    
    // 计算下一个可玩关卡的索引
    val nextToPlayIndex = remember(unlockedLevels, completedLevels) {
        allLevels.indices.firstOrNull { index ->
            val isUnlocked = index in unlockedLevels || index == 0
            val isCompleted = index in completedLevels
            isUnlocked && !isCompleted
        } ?: 0
    }
    
    // 追踪上一个 nextToPlayIndex，用于圆环移动动画
    var previousNextToPlayIndex by remember { mutableStateOf(nextToPlayIndex) }
    
    // 当 nextToPlayIndex 变化时，延迟更新 previousNextToPlayIndex（让动画先执行）
    LaunchedEffect(nextToPlayIndex) {
        if (nextToPlayIndex != previousNextToPlayIndex) {
            // 延迟更新，让圆环移动动画先完成（淡出200ms + 淡入1000ms = 1200ms）
            kotlinx.coroutines.delay(1200)
            previousNextToPlayIndex = nextToPlayIndex
        }
    }
    
    // 记住上次滚动到的关卡索引，用于判断是否需要重新滚动
    var lastScrolledToIndex by remember { mutableStateOf(initialScrollIndex) }
    // 标记是否应该执行滚动动画
    var shouldAnimateScroll by remember { mutableStateOf(false) }
    
    // 检查当前关卡是否在可见区域（响应滚动变化）
    // 使用 derivedStateOf 减少重组，只在滚动时计算
    val isCurrentLevelVisible = remember {
        derivedStateOf {
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
            nextToPlayIndex in visibleIndices
        }
    }
    
    // 共享动画状态 - 只创建一次
    val sharedAnim = rememberSharedAnimations()
    
    // 自定义缓冲滑动效果 - 更平滑的减速
    val smoothFlingBehavior = rememberSmoothFlingBehavior()
    
    // 自动滚动到目标关卡
    // 计算一个关卡的高度：节点容器(110dp) + 连接线(26dp) + 间距(16dp) ≈ 152dp
    val levelHeightPx = with(density) { 152.dp.toPx().toInt() }
    
    // 定位到当前关卡
    val scrollToCurrentLevel: () -> Unit = {
        coroutineScope.launch {
            listState.animateScrollToItem(
                index = nextToPlayIndex,
                scrollOffset = -200 - levelHeightPx * 2
            )
        }
    }
    
    // 使用 LaunchedEffect 监听 scrollTriggerKey 变化
    // 当 scrollTriggerKey 增加时（> 0），说明从游戏返回且可能完成了关卡
    LaunchedEffect(scrollTriggerKey, completedLevelsBeforeGame) {
        // 当 scrollTriggerKey > 0 时，说明从游戏返回（因为初始值是 0）
        if (scrollTriggerKey > 0 && completedLevelsBeforeGame.isNotEmpty()) {
            // 先重置为完成前的状态（确保显示完成前的状态）
            completedLevels = completedLevelsBeforeGame
            // 计算完成前的 unlockedLevels（基于完成前的 completedLevels）
            val beforeGameUnlockedLevels = completedLevelsBeforeGame.map { it + 1 }.toSet() + 0
            unlockedLevels = beforeGameUnlockedLevels
            
            // 从持久化存储读取最新的状态
            val newCompletedLevels = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
            val newUnlockedLevels = site.aiok.onepic.data.LevelProgressManager.getUnlockedClassicLevels(context)
            
            // 检查是否有新完成的关卡
            val newCompleted = newCompletedLevels - completedLevelsBeforeGame
            
            if (newCompleted.isNotEmpty()) {
                // 保存待更新的状态
                pendingCompletedLevels = newCompletedLevels
                pendingUnlockedLevels = newUnlockedLevels
                // 设置延迟更新标志
                shouldDelayUpdateState = true
            } else {
                // 没有新完成的关卡，立即更新状态
                completedLevels = newCompletedLevels
                unlockedLevels = newUnlockedLevels
            }
        }
    }
    
    // 延迟更新状态，实现动画效果
    LaunchedEffect(shouldDelayUpdateState) {
        if (shouldDelayUpdateState && pendingCompletedLevels != null && pendingUnlockedLevels != null) {
            // 延迟 300ms，让用户看到完成前的状态
            kotlinx.coroutines.delay(300)
            
            // 更新状态，这会触发 nextToPlayIndex 的变化，从而触发圆环移动动画
            completedLevels = pendingCompletedLevels!!
            unlockedLevels = pendingUnlockedLevels!!
            
            // 设置标志，等待页面完全加载后再执行滚动动画
            shouldAnimateScroll = true
            
            // 重置标志
            shouldDelayUpdateState = false
            pendingCompletedLevels = null
            pendingUnlockedLevels = null
        }
    }
    
    // 使用 LaunchedEffect 监听 shouldAnimateScroll 标志
    // 当页面完全加载后，执行滚动动画
    LaunchedEffect(shouldAnimateScroll) {
        if (shouldAnimateScroll) {
            // 等待页面完全加载
            kotlinx.coroutines.delay(300)
            
            // 重新计算 nextToPlayIndex（因为 completedLevels 和 unlockedLevels 已更新）
            val currentNextToPlayIndex = allLevels.indices.firstOrNull { index ->
                val isUnlocked = index in unlockedLevels || index == 0
                val isCompleted = index in completedLevels
                isUnlocked && !isCompleted
            } ?: 0
            
            val currentVisibleIndex = listState.firstVisibleItemIndex
            
            // 如果下一关索引和当前可见索引相同或接近，不需要滚动
            val indexDiff = kotlin.math.abs(currentNextToPlayIndex - currentVisibleIndex)
            if (currentNextToPlayIndex == currentVisibleIndex || indexDiff <= 1) {
                shouldAnimateScroll = false
                return@LaunchedEffect
            }
            
            // 从当前关卡位置平滑滚动到下一关（有动画）
            try {
                listState.animateScrollToItem(
                    index = currentNextToPlayIndex,
                    scrollOffset = 0
                )
            } catch (e: Exception) {
                // 忽略错误
            }
            
            shouldAnimateScroll = false
        }
    }

    MeshGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题和总分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：星星图标和总星星数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.3f),
                                    Color(0xFFFFD700).copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = totalStars.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color(0xFFFFD700)
                    )
                }

                // 右侧：总合并得分显示（使用硬币emoji）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.3f),
                                    Color(0xFFFFD700).copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🪙",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                    )
                    Text(
                        text = totalMergeScore.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            // 阶梯式关卡路径
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                flingBehavior = smoothFlingBehavior
            ) {
                itemsIndexed(allLevels, key = { index, _ -> index }) { index, level ->
                    val isUnlocked = index in unlockedLevels || index == 0
                    val isCompleted = index in completedLevels
                    val stars = remember(index) { 
                        site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, index) 
                    }
                    
                    val isNextToPlay = isUnlocked && !isCompleted && 
                        (index == 0 || completedLevels.contains(index - 1))
                    
                    val offsetX = remember(index) {
                        when (index % 4) {
                            0 -> 0.dp
                            1 -> 60.dp
                            2 -> 0.dp
                            3 -> (-60).dp
                            else -> 0.dp
                        }
                    }
                    
                    // 第一个分隔在第1关（S关）后面，即index=0后面（index=1时显示）
                    // 第1关（index=0）是stage 0，第2-6关（index=1-5）是stage 1，第7-11关（index=6-10）是stage 2...
                    val stageIndex = if (index == 0) 0 else (index - 1) / 5 + 1
                    val isFirstOfStage = index == 0 || (index > 0 && (index - 1) % 5 == 0)
                    
                    // 在第1关（S关）后面显示第一个分隔
                    if (index == 1) {
                        StageDivider(
                            stageNumber = 1, // 第一个stage，显示为stage 1
                            sharedAnim = sharedAnim
                        )
                    } else if (isFirstOfStage && index > 1) {
                        StageDivider(
                            stageNumber = stageIndex,
                            sharedAnim = sharedAnim
                        )
                    }
                    
                    if (index > 0 && !isFirstOfStage) {
                        PathConnector(
                            fromOffset = when ((index - 1) % 4) {
                                0 -> 0.dp
                                1 -> 60.dp
                                2 -> 0.dp
                                3 -> (-60).dp
                                else -> 0.dp
                            },
                            toOffset = offsetX,
                            isCompleted = completedLevels.contains(index - 1),
                            stageIndex = stageIndex
                        )
                    }
                    
                    // 只在特定位置显示装饰，减少渲染
                    if (index % 5 == 2) {
                        CartoonDecoration(
                            stageIndex = stageIndex, 
                            side = "left",
                            sharedAnim = sharedAnim
                        )
                    }
                    if (index % 5 == 3) {
                        CartoonDecoration(
                            stageIndex = stageIndex, 
                            side = "right",
                            sharedAnim = sharedAnim
                        )
                    }
                    
                    // 关卡编号：第1关（index=0）显示"S"，第2关开始显示1, 2, 3...
                    val displayLevelNumber = if (index == 0) "S" else index.toString()
                    
                    LevelNode(
                        level = level,
                        levelNumber = index, // 使用实际index，内部会转换为显示文本
                        displayLevelNumber = displayLevelNumber, // 显示的关卡编号
                        isLocked = !isUnlocked,
                        isCompleted = isCompleted,
                        isNextToPlay = isNextToPlay,
                        stars = stars,
                        offsetX = offsetX,
                        stageIndex = stageIndex,
                        sharedAnim = sharedAnim,
                        previousNextToPlayIndex = previousNextToPlayIndex, // 传递上一个 nextToPlayIndex
                        isNewlyCompleted = index in newlyCompletedLevels, // 传递是否刚刚完成
                        onClick = {
                            if (isUnlocked) {
                                onLevelSelected(level, index, "classic") {
                                    site.aiok.onepic.data.LevelProgressManager.markClassicLevelCompleted(context, index)
                                    site.aiok.onepic.data.LevelProgressManager.unlockClassicLevel(context, index + 1)
                                    completedLevels = completedLevels + index
                                    unlockedLevels = unlockedLevels + (index + 1)
                                }
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            }
            
            // 定位按钮 - 当当前关卡不在可见区域时显示
            if (!isCurrentLevelVisible.value) {
                FloatingActionButton(
                    onClick = scrollToCurrentLevel,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位到当前关卡",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// 模式主题色
fun getStageColors(stageIndex: Int): List<Color> {
    return when (stageIndex % 6) {
        0 -> listOf(Color(0xFF81C784), Color(0xFF66BB6A)) // 绿色森林
        1 -> listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)) // 蓝色海洋
        2 -> listOf(Color(0xFFFFB74D), Color(0xFFFFA726)) // 橙色沙漠
        3 -> listOf(Color(0xFFBA68C8), Color(0xFFAB47BC)) // 紫色魔法
        4 -> listOf(Color(0xFFE57373), Color(0xFFEF5350)) // 红色火山
        5 -> listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA)) // 青色冰川
        else -> listOf(Color(0xFF81C784), Color(0xFF66BB6A))
    }
}

// 模式装饰emoji
fun getStageEmoji(stageIndex: Int): String {
    return when (stageIndex % 6) {
        0 -> "🌲"  // 森林
        1 -> "🌊"  // 海洋
        2 -> "🏜️"  // 沙漠
        3 -> "✨"  // 魔法
        4 -> "🌋"  // 火山
        5 -> "❄️"  // 冰川
        else -> "🌟"
    }
}

// 模式名称
fun getStageName(stageIndex: Int): String {
    return when (stageIndex % 6) {
        0 -> "森林"
        1 -> "海洋"
        2 -> "沙漠"
        3 -> "魔法"
        4 -> "火山"
        5 -> "冰川"
        else -> "未知"
    }
}

@Composable
fun StageDivider(
    stageNumber: Int,
    sharedAnim: SharedAnimationState
) {
    val stageIndex = stageNumber - 1
    val colors = remember(stageIndex) { getStageColors(stageIndex) }
    val emoji = remember(stageIndex) { getStageEmoji(stageIndex) }
    val stageName = remember(stageIndex) { getStageName(stageIndex) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧渐变线 - 简化
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, colors[0])
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            // 模式标识
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(72.dp)
                    .scale(sharedAnim.breatheScale)
                    .clip(CircleShape)
                    .background(brush = Brush.verticalGradient(colors = colors))
                    .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                    modifier = Modifier.graphicsLayer {
                        rotationZ = sharedAnim.floatOffset * 0.5f
                    }
                )
            }
            
            // 右侧渐变线 - 简化
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(colors[0], Color.Transparent)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stageName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = colors[1]
        )
    }
}

@Composable
fun CartoonDecoration(
    stageIndex: Int, 
    side: String,
    sharedAnim: SharedAnimationState
) {
    val emoji = remember(stageIndex, side) {
        when (stageIndex % 6) {
            0 -> if (side == "left") "🌳" else "🍄"
            1 -> if (side == "left") "🐠" else "🐚"
            2 -> if (side == "left") "🌵" else "🦂"
            3 -> if (side == "left") "🦄" else "🔮"
            4 -> if (side == "left") "🔥" else "🪨"
            5 -> if (side == "left") "⛄" else "🐧"
            else -> "🌟"
        }
    }
    
    val offsetX = if (side == "left") (-100).dp else 100.dp
    val stageColors = remember(stageIndex) { getStageColors(stageIndex) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX, y = sharedAnim.floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Emoji容器 - 简化，移除外发光
        Box(
            modifier = Modifier
                .offset(y = (-16).dp)
                .size(56.dp)
                .scale(sharedAnim.breatheScale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f))
                .border(2.dp, stageColors[0].copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp)
            )
        }
    }
}

@Composable
fun PathConnector(
    fromOffset: androidx.compose.ui.unit.Dp, 
    toOffset: androidx.compose.ui.unit.Dp, 
    isCompleted: Boolean,
    stageIndex: Int = 0
) {
    val stageColors = remember(stageIndex) { getStageColors(stageIndex) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .offset(x = (fromOffset + toOffset) / 2),
        contentAlignment = Alignment.Center
    ) {
        // 虚线连接点
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                val dotSize = if (isCompleted) 6.dp else 5.dp
                val dotColor = if (isCompleted) {
                    // 完成的路径：渐变色点
                    stageColors[if (index == 1) 0 else 1].copy(alpha = 0.9f)
                } else {
                    Color.White.copy(alpha = 0.25f)
                }
                
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(dotColor, CircleShape)
                        .then(
                            if (isCompleted) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                )
            }
        }
    }
}

// 每个模式的emoji池
val stageEmojiPools = mapOf(
    0 to listOf("🌲", "🌳", "🌴", "🍀", "🌿", "🍃", "🌸", "🦋", "🐿️", "🦊"),  // 森林
    1 to listOf("🐋", "🐬", "🐠", "🦈", "🐙", "🦑", "🐡", "🦐", "🐳", "🌊"),  // 海洋
    2 to listOf("🌵", "🐪", "☀️", "🦂", "🏜️", "🌻", "🦎", "🐫", "🌅", "⭐"),   // 沙漠
    3 to listOf("✨", "🌙", "⭐", "🔮", "🦄", "🌈", "💎", "🪄", "🎭", "🌟"),   // 魔法
    4 to listOf("🔥", "🌋", "💥", "🐉", "☄️", "🦅", "🏔️", "⚡", "🔶", "🧨"),   // 火山
    5 to listOf("❄️", "⛄", "🐧", "🦭", "🏔️", "💠", "🌨️", "🐻‍❄️", "🧊", "💎")   // 冰川
)

// 根据关卡号获取随机emoji（但同一关卡始终相同）
fun getRandomStageEmoji(stageIndex: Int, levelNumber: Int): String {
    val pool = stageEmojiPools[stageIndex % 6] ?: listOf("⭐")
    // 使用关卡号作为种子，确保同一关卡emoji固定
    val index = (levelNumber * 7 + stageIndex * 13) % pool.size
    return pool[index]
}

// 获取锁定状态的emoji
fun getLockedEmoji(stageIndex: Int, levelNumber: Int): String {
    val pool = when (stageIndex % 6) {
        0 -> listOf("🌱", "🍂", "🌰", "🥜")      // 森林 - 种子
        1 -> listOf("🐚", "🦪", "🪸", "🫧")      // 海洋 - 贝壳
        2 -> listOf("🏜️", "🪨", "💨", "🌾")      // 沙漠
        3 -> listOf("💫", "🌠", "✨", "🔮")      // 魔法
        4 -> listOf("🌋", "🪨", "💎", "⚫")      // 火山
        5 -> listOf("🧊", "❄️", "🌬️", "💠")     // 冰川
        else -> listOf("🔮")
    }
    val index = (levelNumber * 11 + stageIndex * 17) % pool.size
    return pool[index]
}

@Composable
fun LevelNode(
    level: LevelConfig,
    levelNumber: Int,
    displayLevelNumber: String = levelNumber.toString(), // 显示的关卡编号，默认使用levelNumber
    isLocked: Boolean,
    isCompleted: Boolean,
    isNextToPlay: Boolean,
    stars: Int,
    offsetX: androidx.compose.ui.unit.Dp,
    stageIndex: Int = 0,
    sharedAnim: SharedAnimationState,
    previousNextToPlayIndex: Int = -1, // 上一个 nextToPlayIndex，用于动画
    isNewlyCompleted: Boolean = false, // 是否刚刚完成（用于触发动画）
    onClick: () -> Unit
) {
    // 判断是否应该显示圆环移动动画（从上一个位置移动到当前位置）
    val shouldShowMoveAnimation = isNextToPlay && previousNextToPlayIndex >= 0 && previousNextToPlayIndex != levelNumber
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 点击缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "scale"
    )
    
    val nodeColors = remember(stageIndex) { getStageColors(stageIndex) }
    val bgEmoji = remember(stageIndex, levelNumber) { getRandomStageEmoji(stageIndex, levelNumber) }
    val lockedEmoji = remember(stageIndex, levelNumber) { getLockedEmoji(stageIndex, levelNumber) }
    
    // 根据状态计算轻微浮动效果（只给活跃关卡）
    val floatY = if (isNextToPlay || (!isLocked && !isCompleted)) {
        sharedAnim.floatOffset * 0.3f
    } else 0f
    
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = floatY.dp)
            .size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // 脉冲圆环 - 只给下一个可玩关卡显示，带移动动画
        // 如果是从上一个位置移动过来的，使用更明显的动画效果
        AnimatedVisibility(
            visible = isNextToPlay,
            enter = if (shouldShowMoveAnimation) {
                // 从上一个位置移动过来：1秒动画，更明显的放大 + 淡入效果
                fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)) + 
                scaleIn(
                    initialScale = 0.2f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                )
            } else {
                // 首次显示：普通淡入
                fadeIn(animationSpec = tween(300)) + 
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.2f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            // 优化：使用 graphicsLayer 减少重组，只在动画时应用 scale
            // 确保所有圆环都是正圆，使用相同的 scaleX 和 scaleY
            val pulseScale = sharedAnim.pulseScale
            
            Box(
                modifier = Modifier
                    .size(120.dp)  // 从110.dp增加到120.dp，让呼吸环更大
                    .clip(CircleShape)  // 先裁剪为圆形，确保初始状态就是圆形
                    .graphicsLayer {
                        // 确保正圆：scaleX 和 scaleY 必须完全一致
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .border(
                        width = 14.dp,  // 从12.dp增加到14.dp，让边框更厚
                        color = nodeColors[0].copy(alpha = 1f),  // 保持不透明，不随动画变化
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)  // 从110.dp增加到120.dp，让呼吸环更大
                    .clip(CircleShape)  // 先裁剪为圆形，确保初始状态就是圆形
                    .graphicsLayer {
                        val innerScale = pulseScale * 0.85f
                        // 确保正圆：scaleX 和 scaleY 必须完全一致
                        scaleX = innerScale
                        scaleY = innerScale
                    }
                    .border(
                        width = 12.dp,  // 从10.dp增加到12.dp，让内环边框更厚
                        color = nodeColors[1].copy(alpha = 0.7f),  // 保持固定透明度，不随动画变化
                        shape = CircleShape
                    )
            )
        }
        
        
        // 关卡圆形节点 - 立体效果
        Box(
            modifier = Modifier
                .size(82.dp)
                .scale(scale)
                .shadow(
                    elevation = if (isLocked) 4.dp else 8.dp,  // 减少阴影高度，优化性能
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),  // 降低阴影透明度，优化性能
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .graphicsLayer {
                    // 3D立体效果 - 只在下一个可玩关卡时应用，减少滚动时的计算
                    if (isNextToPlay) {
                        rotationX = sharedAnim.floatOffset * 0.8f
                        rotationY = sharedAnim.floatOffset * 0.5f
                    }
                    // 移除解锁关卡的轻微立体感，减少滚动时的计算开销
                }
                .clip(CircleShape)
                .background(
                    brush = when {
                        isLocked -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6A6A6A),
                                Color(0xFF4A4A4A),
                                Color(0xFF3A3A3A)
                            )
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                nodeColors[0].copy(alpha = 0.95f),
                                nodeColors[0],
                                nodeColors[1]
                            )
                        )
                    }
                )
                .border(
                    width = if (isNextToPlay) 4.dp else 3.dp,
                    brush = when {
                        isCompleted -> Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                        )
                        isNextToPlay -> Brush.verticalGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0.7f))
                        )
                        isLocked -> Brush.verticalGradient(
                            colors = listOf(
                                nodeColors[0].copy(alpha = 0.4f),
                                nodeColors[1].copy(alpha = 0.2f)
                            )
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // 内部高光效果（立体感）
            if (!isLocked) {
                Box(
            modifier = Modifier
                .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.1f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
            
            // 背景Emoji - 立体旋转效果
            Text(
                text = if (isLocked) lockedEmoji else bgEmoji,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (isLocked) 42.sp else 50.sp
                ),
                modifier = Modifier
                .graphicsLayer { 
                    alpha = if (isLocked) 0.6f else 1f
                    // Emoji也有轻微3D效果 - 只在非锁定且是下一个可玩关卡时应用，减少滚动时的计算
                    if (!isLocked && isNextToPlay) {
                        rotationY = sharedAnim.floatOffset * 0.2f
                        scaleX = 1f + sharedAnim.breatheScale * 0.02f
                        scaleY = 1f + sharedAnim.breatheScale * 0.02f
                    }
                }
            )
            
            if (isLocked) {
                // 锁定状态 - 小锁图标在底部
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3A3A3A),
                                    Color(0xFF2A2A2A)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                // 解锁状态 - 数字叠加在emoji上，白色毛玻璃描边
                Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                    // 关卡数字 - 白色描边效果
            Text(
                        text = displayLevelNumber,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            letterSpacing = (-1).sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                blurRadius = 3f
                            )
                        ),
                        color = Color.White
                    )
                    // 网格大小 - 白色毛玻璃描边
            Text(
                        text = "${level.rows}×${level.cols}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.White.copy(alpha = 0.8f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 6f
                            )
                        ),
                        color = Color.White
                    )
                }
            }
        }
        
        // 完成标记 - 右上角勾（带烟花和对号动画）
        if (isCompleted) {
            // 烟花动画状态 - 只在刚刚完成时触发
            var showFirework by remember { mutableStateOf(false) }
            var showCheckmark by remember { mutableStateOf(false) }
            
            // 当关卡刚刚完成时，触发烟花和对号动画
            LaunchedEffect(isNewlyCompleted) {
                if (isNewlyCompleted) {
                    // 刚刚完成，显示烟花和对号动画
                    showFirework = true
                    // 烟花显示后，延迟显示对号
                    kotlinx.coroutines.delay(300)
                    showFirework = false
                    showCheckmark = true
                } else if (isCompleted) {
                    // 已经完成但不是刚刚完成，直接显示对号（无动画）
                    showCheckmark = true
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                // 烟花效果
                if (showFirework) {
                    val fireworkScale by animateFloatAsState(
                        targetValue = 1.5f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "firework_scale"
                    )
                    val fireworkAlpha by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        label = "firework_alpha"
                    )
                    
                    // 烟花粒子效果 - 使用多个小星星
                    repeat(8) { index ->
                        val angle = (index * 45f) * kotlin.math.PI / 180f
                        val distance = 20.dp
                        val offsetX = (kotlin.math.cos(angle) * distance.value).dp
                        val offsetY = (kotlin.math.sin(angle) * distance.value).dp
                        
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX * fireworkScale, y = offsetY * fireworkScale)
                                .size(6.dp)
                                .graphicsLayer {
                                    alpha = fireworkAlpha
                                    scaleX = fireworkScale
                                    scaleY = fireworkScale
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                
                // 对号标记 - 只在刚刚完成时有1秒动画，否则直接显示
                if (isNewlyCompleted) {
                    // 刚刚完成：使用动画
                    AnimatedVisibility(
                        visible = showCheckmark,
                        enter = fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0f,
                                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                                ),
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
                                )
                            )
                            .border(2.dp, Color.White, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // 已经完成：直接显示，无动画
                    if (showCheckmark) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 得分显示 - 底部（所有解锁关卡都显示）
        if (!isLocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 18.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.9f),
                                Color(0xFFFFA000).copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stars > 0) {
                    // 显示星星数 - 添加描边提高对比度
                    repeat(3) { index ->
                        val isFilled = index < stars
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 描边层
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            // 前景层
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isFilled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    // 未完成时显示占位
            Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                blurRadius = 1f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        // 下一关指示器 - 小箭头
        if (isNextToPlay) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 16.dp)
                    .size(20.dp)
                    .graphicsLayer {
                        // 箭头轻微跳动
                        translationY = -sharedAnim.floatOffset * 0.5f
                    }
            )
        }
    }
}
