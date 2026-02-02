package site.aiok.onepic.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import site.aiok.onepic.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun LevelCompleteDialog(
    stars: Int, // 1-3星
    timeInSeconds: Int,
    scoreGained: Int = 0,
    isDemoLevel: Boolean = false, // 新增：是否为演示/引导关卡
    guideHint: String? = null, // 新增：自定义引导话术
    levelTitle: String? = null,
    storyText: String? = null,
    onDoubleReward: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    
    var showStars by remember { mutableStateOf(false) }
    var doubleRewardClaimed by remember { mutableStateOf(false) }
    val scoreAnimation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        scope.launch {
            alpha.animateTo(1f, animationSpec = tween(400))
        }
        
        delay(400)
        showStars = true
        
        delay(600)
        scoreAnimation.animateTo(
            targetValue = scoreGained.toFloat(),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Card
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 600.dp) // Limit height for small screens or long text
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            this.alpha = alpha.value
                        }
                        .shadow(24.dp, RoundedCornerShape(32.dp))
                        .border(
                            1.dp, 
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.85f),
                                    Color(0xFFF0F0F0).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp) // 减小整体间距
                    ) {
                        // Celebration Emoji
                        Text(
                            text = "\uD83C\uDF8A",
                            fontSize = 44.sp, // 略微缩小 emoji
                            modifier = Modifier.graphicsLayer { 
                                translationY = -8f * (1f - alpha.value)
                            }
                        )

                        // Success Message (Reduced padding)
                        Text(
                            text = when(stars) {
                                3 -> stringResource(R.string.perfect)
                                2 -> stringResource(R.string.great)
                                else -> stringResource(R.string.keep_going)
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp, // 略微缩小标题
                                textAlign = TextAlign.Center,
                                brush = Brush.verticalGradient(listOf(Color(0xFFD84315), Color(0xFFFF8A65)))
                            )
                        )

                        if (!levelTitle.isNullOrBlank()) {
                            Text(
                                text = levelTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        }

                        // Stars Row (Only if not a demo level)
                        if (!isDemoLevel && stars > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                for (i in 1..3) {
                                    DialogStarIcon(
                                        filled = i <= stars,
                                        show = showStars,
                                        delay = (i - 1) * 150L
                                    )
                                }
                            }
                        } else if (isDemoLevel) {
                            // Demo level: Show guide hint with Ascended Icon
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF90CAF9).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AscendedIconHelper()
                                Text(
                                    text = guideHint ?: stringResource(R.string.guide_gallery_hint),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF1976D2),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Stats Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 降低内部间距
                        ) {
                            // Time
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⏱️", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = formatTime(timeInSeconds),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF424242)
                                )
                            }

                            // Score / Coins / Double Button Group
                            if (scoreGained > 0) {
                                // 提取动画变量以便复用
                                val multiplier = if (doubleRewardClaimed) 2 else 1
                                val displayValue by animateIntAsState(
                                    targetValue = (scoreAnimation.value * multiplier).toInt(),
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "scoreDisplay"
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("\uD83E\uDE99", fontSize = 22.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text(
                                        text = "+$displayValue",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 30.sp
                                        ),
                                        color = Color(0xFFFFA000),
                                        modifier = Modifier.scale(if (doubleRewardClaimed) 1.1f else 1.0f)
                                    )
                                    Text(
                                        text = " pts",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFFA000).copy(alpha = 0.7f)
                                    )
                                }

                                // Double Reward Ad Button (Moved inside stats section for tighter layout)
                                if (!isDemoLevel && !doubleRewardClaimed) {
                                    // Pulse animation (Increased for visibility)
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val buttonScale by infiniteTransition.animateFloat(
                                        initialValue = 0.95f,
                                        targetValue = 1.05f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "buttonScale"
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                if (!doubleRewardClaimed) {
                                                    onDoubleReward?.invoke()
                                                    doubleRewardClaimed = true
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("\uD83C\uDFAC", fontSize = 20.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "+$displayValue \u2192 +${displayValue * 2}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 18.sp,
                                                    color = Color(0xFF3E2723)
                                                )
                                                Text(
                                                    text = stringResource(R.string.ad_to_double_short),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF5D4037)
                                                )
                                            }
                                        }
                                    }
                                } else if (doubleRewardClaimed) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "✓ " + stringResource(R.string.ad_double_claimed),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (!storyText.isNullOrBlank()) {
                            Text(
                                text = "\u201C $storyText \u201D",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = TextAlign.Center
                                ),
                                color = Color(0xFF757575),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Close", 
                        tint = Color.White, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AscendedIconHelper() {
    val infiniteTransition = rememberInfiniteTransition(label = "ascended_hint_anim")
    
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(8.dp, CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8E24AA), // Purple
                        Color(0xFF311B92), // Deep Indigo
                        Color(0xFF000000)  // Black
                    )
                ),
                shape = CircleShape
            )
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Rocket Container
        Box(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    translationX = floatOffset * 1.5f
                    translationY = -floatOffset * 1.5f
                    rotationZ = kotlin.math.sin(floatOffset * 0.5f) * 10f
                }
        ) {
            // Re-using the logic for rocket icon
            Icon(
                imageVector = Icons.Filled.RocketLaunch,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DialogStarIcon(filled: Boolean, show: Boolean, delay: Long) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(show) {
        if (show) {
            delay(delay)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (filled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 根据时间和碎片数量计算星星数 (统一绝对时间标准)
fun calculateStars(timeInSeconds: Int, rows: Int, cols: Int, previousBestTime: Int = Int.MAX_VALUE): Int {
    val pieces = rows * cols
    
    // 3星阈值：平均每块4秒 (如 4块=16秒, 12块=48秒)
    val threeStarThreshold = pieces * 4L
    // 2星阈值：平均每块8秒
    val twoStarThreshold = pieces * 8L
    
    return when {
        timeInSeconds <= threeStarThreshold -> 3
        timeInSeconds <= twoStarThreshold -> 2
        else -> 1
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        String.format("%d:%02d", mins, secs)
    } else {
        String.format("%d\"", secs)
    }
}
