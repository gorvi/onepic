package site.aiok.onepic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import site.aiok.onepic.R

@Composable
fun LevelCompleteDialog(
    stars: Int, // 1-3星
    timeInSeconds: Int,
    scoreGained: Int = 0, // 获得的合并得分（硬币分数）
    onDismiss: () -> Unit
) {
    // 弹框滑入动画
    val slideInOffset = remember { Animatable(1000f) }
    val alpha = remember { Animatable(0f) }
    
    // 星星出现动画
    var showStars by remember { mutableStateOf(false) }
    // 分数加成动画
    var showScore by remember { mutableStateOf(false) }
    val scoreAnimation = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // 弹框从底部滑入
        slideInOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        alpha.animateTo(1f, animationSpec = tween(300))
        
        // 延迟显示星星
        kotlinx.coroutines.delay(300)
        showStars = true
        
        // 延迟显示分数
        kotlinx.coroutines.delay(700)
        showScore = true
        // 分数从0递增到实际值
        scoreAnimation.animateTo(
            targetValue = scoreGained.toFloat(),
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f) // 占屏幕高度的60%
                .offset(y = slideInOffset.value.dp)
                .alpha(alpha.value)
                .padding(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 关闭按钮（右上角）
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color(0xFF424242),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp) // 为关闭按钮留出空间
            ) {
                // 标题
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp
                    )
                )
                
                Text(
                    text = when(stars) {
                        3 -> stringResource(R.string.perfect)
                        2 -> stringResource(R.string.great)
                        else -> stringResource(R.string.keep_going)
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = Color(0xFFD84315)
                )
                
                // 时间显示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = formatTime(timeInSeconds),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = Color(0xFFE65100)
                    )
                }
                
                // 星星显示
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    for (i in 1..3) {
                        StarIcon(
                            filled = i <= stars,
                            show = showStars,
                            delay = (i - 1) * 100L
                        )
                    }
                }
                
                // 分数加成效果
                if (showScore && scoreGained > 0) {
                    val scale by animateFloatAsState(
                        targetValue = if (showScore) 1f else 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ), label = "score_scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.3f),
                                        Color(0xFFFFA000).copy(alpha = 0.2f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 硬币emoji - 添加描边效果
                            Text(
                                text = "🪙",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                            Text(
                                text = "+${scoreAnimation.value.toInt()}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                text = stringResource(R.string.points),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                        blurRadius = 3f
                                    )
                                ),
                                color = Color(0xFFFFA000)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarIcon(filled: Boolean, show: Boolean, delay: Long) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    LaunchedEffect(show) {
        if (show) {
            kotlinx.coroutines.delay(delay)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(600)
            )
        }
    }
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆
        if (filled) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f))
            )
        }
        
        // 星星图标 - 添加描边提高对比度
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            // 描边层（黑色，稍大）
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(50.dp)
            )
            // 前景层（金色或灰色）
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (filled) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                modifier = Modifier.size(48.dp)
            )
        }
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

// 根据时间和历史最好成绩计算星星数
fun calculateStars(timeInSeconds: Int, rows: Int, cols: Int, previousBestTime: Int = Int.MAX_VALUE): Int {
    val pieces = rows * cols
    
    // 如果是第一次完成，根据绝对时间评级
    if (previousBestTime == Int.MAX_VALUE || previousBestTime == 0) {
        val threeStarTime = pieces * 5  // 每块5秒 = 3星
        val twoStarTime = pieces * 10   // 每块10秒 = 2星
        
        return when {
            timeInSeconds <= threeStarTime -> 3
            timeInSeconds <= twoStarTime -> 2
            else -> 1
        }
    }
    
    // 如果不是第一次，根据与最佳成绩的比较
    val improvement = (previousBestTime - timeInSeconds).toFloat() / previousBestTime
    
    return when {
        timeInSeconds < previousBestTime -> {
            // 有进步！根据进步幅度给星
            when {
                improvement >= 0.20f -> 3  // 提升20%以上 = 3星
                improvement >= 0.10f -> 2  // 提升10%-20% = 2星
                else -> 1                   // 提升小于10% = 1星
            }
        }
        else -> 1  // 没有进步 = 1星
    }
}

