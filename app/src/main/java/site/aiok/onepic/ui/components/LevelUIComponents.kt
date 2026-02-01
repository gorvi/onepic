package site.aiok.onepic.ui.components


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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import site.aiok.onepic.R
import androidx.compose.ui.draw.blur
import coil.compose.AsyncImage
import site.aiok.onepic.model.ImageSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.SharedAnimationState
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

fun getChapterColors(stageIndex: Int): List<Color> {
    return when (stageIndex) {
        1 -> listOf( // Nature
            Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF43A047), Color(0xFF000000)
        )
        2 -> listOf( // Abyss
            Color(0xFF000051), Color(0xFF1A237E), Color(0xFF0D47A1), Color(0xFF000000)
        )
        3 -> listOf( // Fire
            Color(0xFFBF360C), Color(0xFFD84315), Color(0xFFFF5722), Color(0xFF3E2723)
        )
        4 -> listOf( // Magic
            Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFF8E24AA), Color(0xFF311B92)
        )
        5 -> listOf( // Ruins
            Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF5D4037), Color(0xFF000000)
        )
        6 -> listOf( // Ice
            Color(0xFF01579B), Color(0xFF0277BD), Color(0xFF0288D1), Color(0xFFE1F5FE)
        )
        7 -> listOf( // Mechanical
            Color(0xFF263238), Color(0xFF37474F), Color(0xFF455A64), Color(0xFF000000)
        )
        8 -> listOf( // Time
            Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFFFFD700)
        )
        9 -> listOf( // Cosmos
            Color(0xFF000000), Color(0xFF12005E), Color(0xFF311B92), Color(0xFF000000)
        )
        10 -> listOf( // Void
            Color(0xFF212121), Color(0xFF000000), Color(0xFF311B92), Color(0xFF000000)
        )
        11 -> listOf( // Light
            Color(0xFFF57F17), Color(0xFFFFB300), Color(0xFFFFCA28), Color(0xFFFFF8E1)
        )
        12 -> listOf( // Unity
            Color(0xFF1A237E), Color(0xFF006064), Color(0xFF004D40), Color(0xFF880E4F)
        )
        else -> listOf( // Default/Intro
            Color(0xFF1A237E), Color(0xFF311B92), Color(0xFF0D47A1), Color(0xFF000000)
        )
    }
}

// --- Utilities ---

// 模式主题色
// 模式主题色
fun getStageColors(stageIndex: Int): List<Color> {
    val baseColors = getChapterColors(stageIndex)
    return if (baseColors.size >= 2) listOf(baseColors[0], baseColors[1]) else listOf(Color(0xFF81C784), Color(0xFF66BB6A))
}

@Composable
fun getStageName(stageIndex: Int): String {
    return when (stageIndex) {
        0 -> stringResource(R.string.tut_node_title) 
        1 -> stringResource(R.string.chapter_1)
        2 -> stringResource(R.string.chapter_2)
        3 -> stringResource(R.string.chapter_3)
        4 -> stringResource(R.string.chapter_4)
        5 -> stringResource(R.string.chapter_5)
        6 -> stringResource(R.string.chapter_6)
        7 -> stringResource(R.string.chapter_7)
        8 -> stringResource(R.string.chapter_8)
        9 -> stringResource(R.string.chapter_9)
        10 -> stringResource(R.string.chapter_10)
        11 -> stringResource(R.string.chapter_11)
        12 -> stringResource(R.string.chapter_12)
        else -> stringResource(R.string.chapter_coming_soon)
    }
}


// 每个模式的emoji池
val stageEmojiPools = mapOf(
    0 to listOf("🌲", "🌳", "🌴", "🍀", "🌿", "🍃", "🌸", "🦋", "🐿️", "🦊"),  // 森林
    1 to listOf("🐋", "🐬", "🐠", "🦈", "🐙", "🦑", "🐡", "🦐", "🐳", "🌊"),  // 海洋
    2 to listOf("🌵", "🐪", "☀️", "🦂", "🏜️", "🌻", "蜥蜴", "🐫", "🌅", "⭐"),   // 沙漠
    3 to listOf("✨", "🌙", "⭐", "🔮", "🦄", "🌈", "💎", "🪄", "🎭", "🌟"),   // 魔法
    4 to listOf("🔥", "火山", "💥", "🐉", "☄️", "🦅", "🏔️", "⚡", "🔶", "🧨"),   // 火山
    5 to listOf("❄️", "圣诞老人", "🐧", "🦭", "🏔️", "💠", "🌨️", "🐻‍❄️", "🧊", "💎")   // 冰川
)

// 根据关卡号获取随机emoji
fun getRandomStageEmoji(stageIndex: Int, levelNumber: Int): String {
    val pool = stageEmojiPools[stageIndex % 6] ?: listOf("⭐")
    val index = (levelNumber * 7 + stageIndex * 13) % pool.size
    return pool[index]
}

// --- Components ---

@Composable
fun PathConnector(
    fromOffset: androidx.compose.ui.unit.Dp, 
    toOffset: androidx.compose.ui.unit.Dp, 
    isCompleted: Boolean,
    stageIndex: Int = 0
) {
    val stageColors = remember(stageIndex) { getStageColors(stageIndex) }
    
    val connectorSymbol = remember(stageIndex) {
        when (stageIndex % 6) {
            0 -> "•" 
            1 -> "◦" 
            2 -> "•" 
            3 -> "✧" 
            4 -> "•" 
            5 -> "❄" 
            else -> "•"
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Increased from 40.dp to 60.dp to fit 3 symbols
            .offset(x = (fromOffset + toOffset) / 2),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                val color = if (isCompleted) {
                    stageColors[index % 2].copy(alpha = 0.8f)
                } else {
                    Color.White.copy(alpha = 0.4f)
                }
                
                Text(
                    text = connectorSymbol,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun StageDivider(
    stageIndex: Int,
    sharedAnim: SharedAnimationState
) {
    val colors = remember(stageIndex) { getStageColors(stageIndex) }
    val stageName = getStageName(stageIndex)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f))
                        )
                    )
            )
            
            Text(
                text = stageName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 4.sp,
                    lineHeight = 24.sp
                ),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )
        }
        
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(4.dp)
                .background(colors[0], CircleShape)
                .shadow(elevation = 10.dp, shape = CircleShape, spotColor = colors[0])
        )
    }
}

@Composable
fun LevelNode(
    level: LevelConfig,
    levelNumber: Int,
    displayLevelNumber: String = levelNumber.toString(), 
    isLocked: Boolean,
    isCompleted: Boolean,
    isNextToPlay: Boolean,
    stars: Int,
    ascendedStars: Int = 0, // Add param
    offsetX: androidx.compose.ui.unit.Dp,
    stageIndex: Int = 0,
    sharedAnim: SharedAnimationState,
    previousNextToPlayIndex: Int = -1, 
    isNewlyCompleted: Boolean = false,
    ascendedLevel: LevelConfig? = null,
    isAscendedUnlocked: Boolean = false,
    isAscendedCompleted: Boolean = false,
    onAscendedClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shouldShowMoveAnimation = isNextToPlay && previousNextToPlayIndex >= 0 && previousNextToPlayIndex != levelNumber
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "scale"
    )
    
    val nodeColors = remember(stageIndex) { getStageColors(stageIndex) }

    // State for Unlock Animation (Delayed Dissolve)
    var visualLocked by remember { mutableStateOf(isLocked) }
    var isShaking by remember { mutableStateOf(false) }
    
    // Shockwave Effect
    val shockwaveAnim = remember { Animatable(0f) }

    LaunchedEffect(isLocked) {
        if (!isLocked && visualLocked) {
            // Unlocking: Trigger Shake -> Delay -> [VIBRATE HERE] -> Dissolve -> Shockwave
            isShaking = true
            delay(1000) // 1 second struggle/shake
            isShaking = false
            
            // EXACT MOMENT: When the lock starts to dissolve and shockwave begins
            visualLocked = false
            
            // Trigger Shockwave
            launch {
                shockwaveAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = LinearOutSlowInEasing)
                )
                shockwaveAnim.snapTo(0f)
            }
        } else if (isLocked) {
            visualLocked = true
            isShaking = false
            shockwaveAnim.snapTo(0f)
        }
    }
    
    val floatY = if (isNextToPlay || (!isLocked && !isCompleted)) {
        sharedAnim.floatOffset * 0.3f
    } else 0f

    val imageModel = remember(level.imageSource) {
        when (val source = level.imageSource) {
            is ImageSource.Asset -> "file:///android_asset/${source.path}"
            is ImageSource.Resource -> source.resId
            is ImageSource.UriSource -> source.uri
            is ImageSource.Generated -> null // Fallback to gradient/color
        }
    }
    
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = floatY.dp)
            // Increase size slightly if needed for tap area, or keep 110. Nodes are 110dp.
            // Satellite will overlap TopEnd. We allow overflow.
            .size(110.dp),
        contentAlignment = Alignment.Center
    ) {


        AnimatedVisibility(
            visible = isNextToPlay,
            enter = if (shouldShowMoveAnimation) {
                fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)) + 
                scaleIn(
                    initialScale = 0.2f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                )
            } else {
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
            val pulseScale = sharedAnim.pulseScale
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .border(
                        width = 14.dp,
                        color = nodeColors[0].copy(alpha = 1f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .graphicsLayer {
                        val innerScale = pulseScale * 0.85f
                        scaleX = innerScale
                        scaleY = innerScale
                    }
                    .border(
                        width = 12.dp,
                        color = nodeColors[1].copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
        
        // Satellite Rings Effect (Orbital Rings)
        val isHighDifficulty = levelNumber >= 46
        val ringColors = if (isHighDifficulty) {
            // High energy warning colors for 4x4 (Chapter 10+)
            listOf(Color(0xFFFF4500), Color(0xFFFF1493)) // OrangeRed and DeepPink
        } else {
            nodeColors
        }

        Box(contentAlignment = Alignment.Center) {
            // Cache Brushes to avoid recreation on every frame
            val atmosphereBrush = remember(ringColors) {
                Brush.radialGradient(
                    colors = listOf(
                        ringColors[1].copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            }
            
            val outerRingBrush = remember(ringColors) {
                Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        ringColors[0].copy(alpha = 0.4f),
                        Color.Transparent,
                        ringColors[1].copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            }
            
            val innerRingBrush = remember(ringColors) {
                Brush.sweepGradient(
                    colors = listOf(
                        ringColors[1].copy(alpha = 0.3f),
                        Color.Transparent,
                        ringColors[0].copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            }

            // Layer 1: Outer Atmosphere Glow (Pulsing)
            // Optimization: Use graphicsLayer to isolate scale animation and cache background brush
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer {
                        // Read sharedAnim state inside graphicsLayer to avoid recomposition
                        val scaleVal = 1f + (1f - sharedAnim.pulseAlpha) * 0.05f
                        scaleX = scaleVal
                        scaleY = scaleVal
                    }
                    .background(
                        brush = atmosphereBrush,
                        shape = CircleShape
                    )
            )

            // Layer 2: Outer Satellite Ring (Slow)
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .graphicsLayer {
                        rotationZ = sharedAnim.floatOffset * 15f 
                    }
                    .border(
                        width = 1.dp,
                        brush = outerRingBrush,
                        shape = CircleShape
                    )
            )
            
            // Layer 3: Inner Satellite Ring (Fast, Reverse)
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .graphicsLayer {
                        rotationZ = -sharedAnim.floatOffset * 30f 
                    }
                    .border(
                        width = 0.8.dp,
                        brush = innerRingBrush,
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(82.dp)
                .scale(scale)
                .shadow(
                    elevation = if (isLocked) 4.dp else 20.dp,
                    shape = CircleShape,
                    ambientColor = nodeColors[0].copy(alpha = 0.6f),
                    spotColor = nodeColors[1].copy(alpha = 0.6f)
                )
                .graphicsLayer {
                    if (isNextToPlay) {
                        rotationX = sharedAnim.floatOffset * 0.8f
                        rotationY = sharedAnim.floatOffset * 0.5f
                    }
                }
                .clip(CircleShape)
                // Multi-layered border for "Coming Soon" style glow
                .then(
                    if (isCompleted) {
                        Modifier
                            .border(4.dp, Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700))), CircleShape)
                            .padding(2.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    } else if (isNextToPlay) {
                        Modifier
                            .border(4.dp, Brush.sweepGradient(listOf(nodeColors[0], nodeColors[1], nodeColors[0])), CircleShape)
                            .padding(2.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    } else {
                         Modifier.border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Victory Halo for Completed Levels
            if (isCompleted) {
                val infiniteTransition = rememberInfiniteTransition(label = "victory_halo")
                val haloScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo_scale"
                )
                val haloAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "halo_alpha"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                            alpha = haloAlpha
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }
            // Background Image/Thumbnail
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isLocked) Modifier.blur(10.dp) else Modifier)
                        .then(if (isCompleted) Modifier.completedLevelEffect() else Modifier) // Apply effect
                )
            } else if (levelNumber == 0) {
                // Special Holographic background for Origin Terminal
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.6f),
                                    Color(0xFF0D47A1).copy(alpha = 0.9f),
                                    Color.Black
                                )
                            )
                        )
                        .then(if (isCompleted) Modifier.completedLevelEffect() else Modifier) // Apply to Origin too
                ) {
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding((i * 4).dp)
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), if (i % 2 == 0) CircleShape else RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Overlay for locked/completed states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isLocked) {
                            Modifier.border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        } else Modifier
                    )
                    .background(
                        when {
                            isLocked -> Brush.radialGradient(
                                colors = listOf(
                                    nodeColors[0].copy(alpha = 0.4f),
                                    nodeColors[1].copy(alpha = 0.9f)
                                )
                            )
                            levelNumber == 0 -> Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                            isCompleted -> Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f), // Lighter overlay for completed to show sheen
                                    Color.Transparent
                                )
                            )
                            else -> Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        }
                    )
            )

            // Animate transition between Locked and Unlocked states
            AnimatedContent(
                targetState = visualLocked,
                transitionSpec = {
                    if (targetState) {
                        // Unlocked -> Locked (Should rarely happen)
                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f)).togetherWith(
                            fadeOut(animationSpec = tween(300))
                        )
                    } else {
                        // Locked -> Unlocked (EXPLOSIVE UNLOCK)
                        // Text enters: SLAM from 2.5x to 1x with heavy Spring
                        (fadeIn(animationSpec = tween(400, delayMillis = 100)) + 
                         scaleIn(
                             initialScale = 2.5f, 
                             animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                         ))
                            .togetherWith(
                                // Lock exits: EXPLODE towards camera (Scale 3x) + Fade Out
                                fadeOut(animationSpec = tween(500)) + 
                                scaleOut(targetScale = 3f, animationSpec = tween(500, easing =  FastOutLinearInEasing))
                            )
                    }.using(SizeTransform(clip = false))
                },
                label = "lock_transition"
            ) { locked ->
                if (locked) {
                // Shake and Breath Animation for the Lock
                val infiniteTransition = rememberInfiniteTransition(label = "lock_animation")
                
                // Shake (Rotation and subtle translation)
                val shakeOffset by infiniteTransition.animateFloat(
                    initialValue = -2f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(100, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lock_shake_offset"
                )
                
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -8f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(150, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lock_shake_rotation"
                )
                
                // Breath (Scale and Glow intensity)
                val breatheScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lock_breathe_scale"
                )
                
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lock_glow_alpha"
                )

                // Custom Sci-Fi Holographic Lock
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            val shakeMultiplier = if (isShaking) 3f else 1f
                            val scaleMultiplier = if (isShaking) 1.3f else 1f
                            
                            scaleX = breatheScale * scaleMultiplier
                            scaleY = breatheScale * scaleMultiplier
                            rotationZ = rotation * shakeMultiplier
                            translationX = shakeOffset * shakeMultiplier
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Outer Holographic Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                                radius = size.minDimension / 1.5f
                            ),
                            alpha = glowAlpha
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = size.minDimension / 2.2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        )
                    }

                    // 2. 3D-Look Core Symbol (Combination of shapes for "Sci-fi Lock")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Shackle (3D Tube look)
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp, height = 14.dp)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.4f))
                                    ),
                                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                )
                        )
                        // Body (Industrial Multi-layered look)
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 18.dp)
                                .offset(y = (-2).dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White, Color.White.copy(alpha = 0.7f))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        ) {
                            // "Core" Glow in the lock body
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(6.dp)
                                    .background(
                                        color = nodeColors[0].copy(alpha = glowAlpha),
                                        shape = CircleShape
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }
            } else {
                val isS = displayLevelNumber == "0" || displayLevelNumber == "S"
                Text(
                    text = if (isS) "START" else displayLevelNumber,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = if (isS) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = if (isS) 18.sp else 32.sp,
                        letterSpacing = if (isS) 2.sp else 0.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = if (isS) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.8f) ,
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = if (isS) 12f else 4f
                        )
                    ),
                    color = Color.White
                )
            }

            }
        }
        
        // Shockwave Overlay
        if (shockwaveAnim.value > 0f) {
             Canvas(modifier = Modifier.fillMaxSize()) {
                 val progress = shockwaveAnim.value
                 val maxRadius = size.minDimension * 1.2f
                 drawCircle(
                     color = Color.White.copy(alpha = (1f - progress) * 0.8f),
                     radius = maxRadius * progress,
                     style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10.dp.toPx() * (1f - progress))
                 )
             }
        }
        
        if (isCompleted) {
            var showFirework by remember { mutableStateOf(false) }
            // ... (Rest of checkmark logic)

            var showCheckmark by remember { mutableStateOf(false) }
            
            LaunchedEffect(isNewlyCompleted) {
                if (isNewlyCompleted) {
                    showFirework = true
                    kotlinx.coroutines.delay(300)
                    showFirework = false
                    showCheckmark = true
                } else if (isCompleted) {
                    showCheckmark = true
                }
            }
            
            val checkmarkAlignment = if (ascendedLevel != null) Alignment.TopStart else Alignment.TopEnd
            val checkmarkOffsetX = if (ascendedLevel != null) (-2).dp else 2.dp
            
            Box(
                modifier = Modifier
                    .align(checkmarkAlignment)
                    .offset(x = checkmarkOffsetX, y = (-2).dp)
                    .size(26.dp),
                contentAlignment = Alignment.Center
            ) {
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
                    
                    repeat(8) { index ->
                        val angle = (index * 45f) * kotlin.math.PI / 180f
                        val distance = 20.dp
                        val offsetX_f = (kotlin.math.cos(angle) * distance.value).dp
                        val offsetY_f = (kotlin.math.sin(angle) * distance.value).dp
                        
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX_f * fireworkScale, y = offsetY_f * fireworkScale)
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
                
                if (showCheckmark) {
                    val enterAnim = if (isNewlyCompleted) {
                        fadeIn(animationSpec = tween(1000, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0f,
                            animationSpec = tween(1000, easing = FastOutSlowInEasing)
                        )
                    } else fadeIn()

                    AnimatedVisibility(
                        visible = true,
                        enter = enterAnim,
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
                }
            }
        }
        
        // The following code block is assumed to be part of a larger composable,
        // likely a Column or similar, where `idx`, `isFirstOfStage`, `offsetX`,
        // `completedLevels`, and `stageIndex` are defined in its scope.
        // This insertion is based on the provided context in the instruction.
        // It is placed here as it's the most logical place given the surrounding
        // code snippets provided in the instruction.
        // This block is not directly present in the original content provided,
        // but the instruction implies its existence and placement.
        // If this is incorrect, please provide more context for the insertion point.
        // --- Start of inserted block based on instruction ---
        // This block is likely part of a loop or conditional rendering within a LevelRow composable.
        // The instruction implies this code should be inserted *after* the checkmark logic
        // and *before* the stars row, but the exact placement is ambiguous without the full
        // `LevelRow` context. Placing it here as a placeholder.
        // This code is not syntactically correct in this exact location without
        // the surrounding `LevelRow` composable and its parameters (`idx`, `isFirstOfStage`, etc.).
        // It is included as per the user's request to "make the change faithfully".
        // --- End of inserted block based on instruction ---
        
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
                    repeat(3) { index ->
                        val isFilled = index < stars
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isFilled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
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
                        translationY = -sharedAnim.floatOffset * 0.5f
                    }
            )
        }

        // --- Satellite Node (Ascended) - Render LAST to be on TOP ---
        if (ascendedLevel != null && isCompleted) {
             val satScale = if (isAscendedUnlocked) sharedAnim.pulseScale else 1f
             val satBorderColor = if (isAscendedCompleted) Color(0xFFFFD700) else Color.White // Gold border for completed, White for unlecked
             val satBrush = when {
                 isAscendedCompleted || isAscendedUnlocked -> Brush.radialGradient(
                     colors = listOf(
                         Color(0xFF8E24AA), // Purple
                         Color(0xFF311B92), // Deep Indigo
                         Color(0xFF000000)  // Black Space
                     )
                 )
                 else -> SolidColor(Color.Gray)
             }
            
             // Use a Column to stack the Sphere and the Stars vertically
             Column(
                 modifier = Modifier
                     .align(Alignment.TopEnd)
                     .offset(x = 16.dp, y = (-12).dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 // The Sphere
                 Box(
                     modifier = Modifier
                         .size(40.dp)
                         .scale(satScale)
                         .shadow(
                             elevation = if (isAscendedCompleted) 12.dp else 8.dp,
                             shape = CircleShape,
                             spotColor = if (isAscendedCompleted) Color(0xFF2E7D32) else Color(0xFFFF6B35)
                         )
                         .background(
                             brush = satBrush,
                             shape = CircleShape
                         )
                         .border(
                             width = if (isAscendedCompleted) 3.dp else 2.dp,
                             color = satBorderColor,
                             shape = CircleShape
                         )
                         .clickable(enabled = isAscendedUnlocked, onClick = onAscendedClick),
                     contentAlignment = Alignment.Center
                 ) {
                     // 1. Thumbnail Background
                     val imageModel = remember(ascendedLevel.imageSource) {
                         when (val src = ascendedLevel.imageSource) {
                             is ImageSource.Asset -> "file:///android_asset/${src.path}"
                             is ImageSource.Resource -> src.resId
                             is ImageSource.UriSource -> src.uri
                             else -> null // Handle Generated or others by showing background brush
                         }
                     }
                     
                     AsyncImage(
                         model = imageModel,
                         contentDescription = null,
                         modifier = Modifier
                             .fillMaxSize()
                             .clip(CircleShape),
                         contentScale = ContentScale.Crop
                     )
                     
                     // 2. Scrim (to make icons visible)
                     Box(
                         modifier = Modifier
                             .fillMaxSize()
                             .clip(CircleShape)
                             .background(Color.Black.copy(alpha = 0.3f))
                     )

                     if (isAscendedCompleted) {
                         // Completed -> Star (Gold)
                         Icon(
                             imageVector = Icons.Default.Star,
                             contentDescription = null,
                             tint = Color(0xFFFFD700), // Gold Star
                             modifier = Modifier
                                 .size(26.dp)
                                 .graphicsLayer {
                                     // Refined Shining Animation:
                                     // 1. Slow Rotation
                                     rotationZ = sharedAnim.floatOffset * 2f 

                                     // 2. Subtle Pulse (Scale) - "Amplitude small"
                                     val subtlePulse = 0.95f + (sharedAnim.pulseScale - 1f) * 0.2f
                                     scaleX = subtlePulse
                                     scaleY = subtlePulse

                                     // 3. Twinkle (Alpha) - "Bright and Dark"
                                     // Use sine wave based on floatOffset for independent speed
                                     alpha = 0.8f + (kotlin.math.sin(sharedAnim.floatOffset * 2f).toFloat() + 1f) * 0.1f 
                                 }
                         )
                     } else if (isAscendedUnlocked) {
                          // Unlocked -> Rocket (White)
                          // Unlocked -> Rocket (White)
                          // Wrapper Box for Rocket + Flame to animate together
                          Box(
                              modifier = Modifier
                                  .size(22.dp)
                                  .graphicsLayer {
                                      // "Launching" Animation: Hover diagonal Up-Right
                                      val offset = sharedAnim.floatOffset
                                      translationX = offset * 3f 
                                      translationY = -offset * 3f // Move up
                                      
                                      // Add slight thrust vibration/rotation
                                      rotationZ = sin(offset * 0.5f) * 5f
                                  }
                          ) {
                              // Flame Effect (Behind Rocket)
                              RocketFlame(
                                  modifier = Modifier
                                      .align(Alignment.BottomStart)
                                      .offset(x = 0.dp, y = 0.dp) // Adjust based on icon shape
                                      .graphicsLayer { rotationZ = 45f } // Ignite direction
                              )

                              Icon(
                                  imageVector = Icons.Filled.RocketLaunch,
                                  contentDescription = null,
                                  tint = Color.White,
                                  modifier = Modifier.fillMaxSize()
                              )
                          }
                      } else {
                           Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                      }
                 }
                 
                 // Display Ascended Star Count (Standard Style below Rocket) - Outside the Sphere Box
                 if (isAscendedCompleted) {
                     Spacer(modifier = Modifier.height(4.dp)) // Spacing between sphere and stars
                     Row(
                         modifier = Modifier
                             .background(
                                 brush = Brush.horizontalGradient(
                                     colors = listOf(
                                         Color(0xFFFFD700).copy(alpha = 0.9f),
                                         Color(0xFFFFA000).copy(alpha = 0.8f)
                                     )
                                 ),
                                 shape = RoundedCornerShape(8.dp)
                             )
                             .padding(horizontal = 4.dp, vertical = 1.dp),
                         horizontalArrangement = Arrangement.spacedBy(1.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         repeat(3) { index ->
                             val isFilled = index < ascendedStars
                             Box(
                                 modifier = Modifier.size(8.dp),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(
                                     imageVector = Icons.Default.Star,
                                     contentDescription = null,
                                     tint = Color.Black.copy(alpha = 0.6f),
                                     modifier = Modifier.size(8.dp)
                                 )
                                 Icon(
                                     imageVector = Icons.Default.Star,
                                     contentDescription = null,
                                     tint = if (isFilled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f), // Unified Gold Style
                                     modifier = Modifier.size(7.dp)
                                 )
                             }
                         }
                     }
                 }
             }
        }
    }
}


@Composable
fun ComingSoonNode(
    offsetX: androidx.compose.ui.unit.Dp,
    sharedAnim: SharedAnimationState,
    stageIndex: Int
) {
    val nodeColors = remember(stageIndex) { getStageColors(stageIndex) }
    
    // 脉冲动画
    val pulseAlpha = sharedAnim.pulseAlpha
    val floatOffset = sharedAnim.floatOffset

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = offsetX, y = floatOffset.dp * 0.5f)
            .padding(top = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Layer 1: Outer Glow (Pulsing)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(1f + (1f - pulseAlpha) * 0.1f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                nodeColors[1].copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Layer 2: Rotating Portal Ring (Slow)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        rotationZ = sharedAnim.floatOffset * 15f 
                    }
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                nodeColors[0],
                                Color.Transparent,
                                nodeColors[1],
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Layer 3: Inner Rotating Ring (Fast, Reverse)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        rotationZ = -sharedAnim.floatOffset * 30f 
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                nodeColors[1],
                                Color.Transparent,
                                nodeColors[0],
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Layer 4: Dark Void Core
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black,
                                nodeColors[1].copy(alpha = 0.9f)
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                 // Star Dust
                 Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2
                    repeat(12) {
                        val angle = (it * 30f + sharedAnim.floatOffset * 10f) * (PI / 180f).toFloat()
                        val dist = r * (0.3f + (it % 3) * 0.2f)
                        val alpha = (0.5f + sin((it + sharedAnim.floatOffset).toDouble()) * 0.4f).toFloat()
                        
                        drawCircle(
                            color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                            radius = (1.5f + (it%2)).dp.toPx(),
                            center = center + Offset(
                                (cos(angle) * dist),
                                (sin(angle) * dist)
                            )
                        )
                    }
                }
            }
            
            // Layer 5: Mystery Symbol
            Text(
                text = "?",
                style = MaterialTheme.typography.headlineMedium.copy(
                     fontWeight = FontWeight.ExtraLight,
                     fontSize = 32.sp
                ),
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "TO BE CONTINUED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun Modifier.completedLevelEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "completed_effect")
    
    // 1. Breathing Effect (Scale)
    // Subtle heartbeat: 1.0 -> 1.03 -> 1.0 over 3 seconds
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_breath"
    )
    
    // 2. Sheen Effect (Sweep)
    // A shiny white line sweeps across every 3 seconds
    val offset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_sweep"
    )

    return this
        .scale(scale)
        .drawWithContent {
            // 3. Dark Semi-transparent Base for Depth
            drawContent()
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f), // Darkens the image to make HUD elements pop
                blendMode = BlendMode.SrcOver
            )

            // 4. Sci-Fi Digital Grid (HUD Style)
            val gridSize = 8.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.12f)
            
            // Vertical lines
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
                x += gridSize
            }
            
            // Horizontal lines
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 0.5.dp.toPx()
                )
                y += gridSize
            }

            // 5. Enhanced Scanning Sheen
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = 0.35f),  // Pulse center
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(offset, offset),
                end = androidx.compose.ui.geometry.Offset(offset + 220f, offset + 220f),
                tileMode = TileMode.Decal
            )
            
            drawRect(
                brush = brush,
                blendMode = BlendMode.Overlay
            )
            
            // 6. Vignette for 3D sphere depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                    center = center,
                    radius = size.minDimension / 2f
                ),
                blendMode = BlendMode.Multiply
            )
        }
}

@Composable
fun RocketFlame(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rocket_flame")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_alpha"
    )

    Canvas(modifier = modifier.size(10.dp)) {
        // Draw a flame shape pointing down-left
        // Since we are at BottomStart, we draw relative to center or top-right of this canvas
        
        rotate(degrees = 135f) { // Point away from Top-Right (Rocket Head) -> Point Down-Left
             scale(scale, scale) {
                 drawCircle(
                     brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEA00), Color(0xFFFF3D00), Color.Transparent), // Yellow -> Red -> Transparent
                        center = center,
                        radius = size.minDimension / 2
                     ),
                     alpha = alpha
                 )
             }
        }
    }
}
