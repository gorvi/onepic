package site.aiok.onepic.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.ui.components.MeshGradientBackground
import site.aiok.onepic.audio.SoundManager
import site.aiok.onepic.audio.SoundType
import site.aiok.onepic.R
import site.aiok.onepic.utils.LocaleHelper
import androidx.compose.ui.res.stringResource
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

// 可复用的数字动画组件
@Composable
fun AnimatedNumberText(
    targetValue: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = ""
) {
    val animatedValue = remember { Animatable(targetValue.toFloat()) }
    var shouldAnimate by remember { mutableStateOf(false) }
    var previousValue by remember { mutableStateOf(targetValue) }
    
    // 当目标值改变时，触发动画
    LaunchedEffect(targetValue) {
        if (targetValue != previousValue) {
            shouldAnimate = true
            previousValue = targetValue
            
            // 数字从当前值平滑过渡到目标值
            animatedValue.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
            
            // 动画完成后重置
            kotlinx.coroutines.delay(400)
            shouldAnimate = false
        }
    }
    
    // 缩放动画 - 数字变化时放大再恢复
    val scale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "number_scale"
    )
    
    // 颜色闪烁效果（数字变化时短暂变亮）
    val textColor by animateColorAsState(
        targetValue = if (shouldAnimate) {
            // 根据颜色类型调整亮度
            when {
                // 金色变亮
                color.red > 0.9f && color.green > 0.8f && color.blue < 0.3f -> 
                    Color(0xFFFFEB3B)
                // 其他颜色：增加亮度（RGB各通道乘以1.3）
                else -> Color(
                    red = (color.red * 1.3f).coerceAtMost(1f),
                    green = (color.green * 1.3f).coerceAtMost(1f),
                    blue = (color.blue * 1.3f).coerceAtMost(1f),
                    alpha = color.alpha
                )
            }
        } else {
            color
        },
        animationSpec = tween(300),
        label = "number_color"
    )
    
    Text(
        text = "$prefix${animatedValue.value.toInt()}$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = textColor,
        modifier = modifier.scale(scale)
    )
}

@Composable
fun CheckInScreen() {
    val context = LocalContext.current
    
    // 从 SharedPreferences 读取打卡状态
    val prefs = remember { 
        context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    }
    // 使用系统时区获取当前日期 - 每次进入页面时都重新获取最新日期
    val currentDate = run {
        val calendar = Calendar.getInstance() // 自动使用系统默认时区
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = calendar.timeZone // 明确使用系统时区
        }.format(calendar.time)
    }
    
    var hasCheckedInToday by remember { 
        mutableStateOf(prefs.getString("last_check_in_date", "") == currentDate)
    }
    var consecutiveDays by remember { 
        mutableStateOf(prefs.getInt("consecutive_days", 0))
    }
    // 从SharedPreferences读取今日奖励 - 只有今天打卡的才显示奖励
    var coinsEarned by remember { 
        mutableStateOf(
            if (prefs.getString("last_check_in_date", "") == currentDate) {
                // 如果今天已经打卡，读取保存的奖励
                prefs.getInt("today_coins", 0)
            } else {
                // 今天还没打卡，显示0
                0
            }
        )
    }
    var showCoinAnimation by remember { mutableStateOf(false) }
    var totalCoins by remember { mutableStateOf(LevelProgressManager.getTotalMergeScore(context)) }
    
    // 打卡成功动画状态
    var showCheckInSuccess by remember { mutableStateOf(false) }
    var cardPulseScale by remember { mutableStateOf(1f) }
    
    // 红点状态管理
    var shouldShowRedDot by remember { mutableStateOf(LevelProgressManager.shouldShowCheckInRedDot(context)) }
    
    // 进入页面时如果有红点，由于已展示给用户看，点击行为将消除它（或者只要进入就消除？）
    // 用户的需求是：点击后两个小红点消失。
    // 点击指点击“签到卡片”本身
    
    // 音效管理器
    val soundManager = remember { SoundManager.getInstance(context) }
    
    // 打卡卡片脉冲动画（打卡成功时）
    val cardScale by animateFloatAsState(
        targetValue = cardPulseScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardPulseScale"
    )
    
    // 打卡成功时的闪光效果
    val flashAlpha by animateFloatAsState(
        targetValue = if (showCheckInSuccess) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "flashAlpha"
    )
    
    // 打卡按钮动画
    val buttonScale by animateFloatAsState(
        targetValue = if (hasCheckedInToday) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )
    
    // 打卡卡片颜色动画
    val cardColor by animateColorAsState(
        targetValue = if (hasCheckedInToday) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFF2196F3)
        },
        animationSpec = tween(500),
        label = "cardColor"
    )
    
    // 金币动画
    val coinScale by animateFloatAsState(
        targetValue = if (showCoinAnimation) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "coinScale"
    )
    
    site.aiok.onepic.ui.components.GalaxyBackground(particleTheme = "signin") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // 顶部金币显示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_check_in),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                // 金币显示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.3f),
                                    Color(0xFFFFA500).copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 20.sp,
                        modifier = Modifier.scale(coinScale)
                    )
                    AnimatedNumberText(
                        targetValue = totalCoins,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            // 当前日期显示
            val currentDateDisplay = run {
                val calendar = Calendar.getInstance()
                val locale = LocaleHelper.getSavedLanguage(context).let {
                    when (it) {
                        "en" -> Locale.ENGLISH
                        "zh" -> Locale.CHINESE
                        else -> Locale.getDefault()
                    }
                }
                val pattern = if (locale == Locale.CHINESE) {
                    "yyyy年MM月dd日 EEEE"
                } else {
                    "EEEE, MMMM dd, yyyy"
                }
                SimpleDateFormat(pattern, locale).apply {
                    timeZone = calendar.timeZone
                }.format(calendar.time)
            }
            Text(
                text = currentDateDisplay,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 主打卡卡片 - 全息毛玻璃版
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.linearGradient(
                            colors = if (hasCheckedInToday) {
                                listOf(Color(0xFF00E676).copy(alpha = 0.5f), Color.Transparent, Color(0xFF00E676).copy(alpha = 0.2f))
                            } else {
                                listOf(Color(0xFF00B0FF).copy(alpha = 0.5f), Color.Transparent, Color(0xFF00B0FF).copy(alpha = 0.2f))
                            }
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = !hasCheckedInToday) {
                        performCheckIn(context, hasCheckedInToday) { coins, days ->
                            soundManager.playSound(SoundType.SNAP, 1.0f)
                            try {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 30, 50), -1))
                                } else {
                                    @Suppress("DEPRECATION") vibrator?.vibrate(150)
                                }
                            } catch (e: Exception) {}
                            
                            showCheckInSuccess = true
                            cardPulseScale = 1.15f
                            hasCheckedInToday = true
                            coinsEarned = coins
                            consecutiveDays = days
                            totalCoins = LevelProgressManager.getTotalMergeScore(context)
                            showCoinAnimation = true
                            
                            // 消除红点
                            LevelProgressManager.markCheckInRedDotSeen(context)
                            shouldShowRedDot = false
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(150)
                                cardPulseScale = 1f
                                delay(200)
                                showCheckInSuccess = false
                                delay(1200)
                                showCoinAnimation = false
                            }
                        }
                    }
                    .scale(cardScale),
                contentAlignment = Alignment.Center
            ) {
                // 背景全息网格装饰
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val strokeWidth = 0.5.dp.toPx()
                    val step = 20.dp.toPx()
                    var x = 0f
                    while (x < canvasWidth) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.03f),
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, canvasHeight),
                            strokeWidth = strokeWidth
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < canvasHeight) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.03f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                            strokeWidth = strokeWidth
                        )
                        y += step
                    }
                }

                // 角标装饰
                val cornerColor = if (hasCheckedInToday) Color(0xFF00E676) else Color(0xFF00B0FF)
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top-Left corner
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(12.dp, 2.dp).background(cornerColor.copy(alpha = 0.6f)))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(2.dp, 12.dp).background(cornerColor.copy(alpha = 0.6f)))
                    // Bottom-Right corner
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(12.dp, 2.dp).background(cornerColor.copy(alpha = 0.6f)))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(2.dp, 12.dp).background(cornerColor.copy(alpha = 0.6f)))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasCheckedInToday) {
                        // 已打卡状态 - 极光粒子感
                        Box(contentAlignment = Alignment.Center) {
                            // 外围发光
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF00E676).copy(alpha = 0.2f), Color.Transparent)
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF00E676)
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.check_in_energy_active),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        
                    } else {
                        // 未打卡状态 - 待激活感
                        Box(contentAlignment = Alignment.Center) {
                            // 扫描动画圆环
                            val infiniteTransition = rememberInfiniteTransition(label = "scan")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                                label = "rotation"
                            )
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer { rotationZ = rotation }
                                    .border(1.dp, Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF00B0FF), Color.Transparent)), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF00B0FF).copy(alpha = 0.8f)
                            )
                            
                            if (shouldShowRedDot) {
                                // 卡片内部红点
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .border(1.5.dp, Color(0xFF1E1E2C), CircleShape)
                                )
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.check_in_energy_activate), // 注意：我需要补一个这个资源或复用
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Text(
                            text = stringResource(R.string.check_in_sync_required),
                            fontSize = 12.sp,
                            color = Color(0xFF00B0FF).copy(alpha = 0.5f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 统计卡片 - 增强科技排版
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Pair(stringResource(R.string.consecutive_check_in), stringResource(R.string.check_in_days_suffix, consecutiveDays)),
                    Pair(stringResource(R.string.today_earned), if (hasCheckedInToday) "+$coinsEarned 🪙" else stringResource(R.string.check_in_pending))
                ).forEachIndexed { index, pair ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = pair.first.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = pair.second,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 0) Color(0xFF00B0FF) else Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 30天打卡情况
            MonthlyCheckInHistory(
                modifier = Modifier.fillMaxWidth(),
                context = context,
                refreshTrigger = hasCheckedInToday
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// 双倍金币 Buff 组件 - 支持手动激活
@Composable
fun DoubleCoinsBuffTimer(context: Context) {
    var remainingSeconds by remember { mutableStateOf(LevelProgressManager.getDoubleCoinsRemainingSeconds(context)) }
    var isReady by remember { mutableStateOf(LevelProgressManager.isDoubleCoinsReady(context)) }
    val isActive = remainingSeconds > 0
    
    // 定时器更新
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            remainingSeconds = LevelProgressManager.getDoubleCoinsRemainingSeconds(context)
            isReady = LevelProgressManager.isDoubleCoinsReady(context)
        }
    }
    
    val soundManager = remember { SoundManager.getInstance(context) }
    
    when {
        isActive -> {
            // 运行态：显示倒计时
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.15f),
                                Color(0xFFFFA500).copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.buff_double_coins_active),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.buff_overclocking),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.End
                    )
                }
                
                // 底部能量条动画
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth((remainingSeconds / 180f).coerceAtMost(1f))
                        .height(2.dp)
                        .background(Color(0xFFFFD700))
                )
            }
        }
        isReady -> {
            // 就绪态：显示待激活按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable {
                        soundManager.playSound(SoundType.COMPLETE, 1.0f)
                        val seconds = LevelProgressManager.getBuffEstimatedSeconds(context)
                        LevelProgressManager.activateDoubleCoinsBuff(context, seconds)
                        LevelProgressManager.setDoubleCoinsReady(context, false)
                        remainingSeconds = seconds
                        isReady = false
                    }
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
                                .background(Color(0xFFFFD700).copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.buff_ready),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.buff_waiting),
                                fontSize = 10.sp,
                                color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = stringResource(R.string.buff_activate),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        modifier = Modifier
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.buff_duration_rule),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// 30天打卡历史组件 - 增强网格质感
@Composable
fun MonthlyCheckInHistory(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    refreshTrigger: Boolean = false
) {
    val prefs = remember { context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE) }
    
    val monthHistory = remember(refreshTrigger) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = calendar.timeZone }
        val checkedInDates = prefs.getStringSet("checked_in_dates", setOf()) ?: setOf()
        
        (0..29).map { dayOffset ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val date = calendar.time
            val dateStr = dateFormat.format(date)
            val isCheckedIn = checkedInDates.contains(dateStr)
            DayInfo(date, dateStr, isCheckedIn, dayOffset == 0, "")
        }.reversed()
    }
    
    Box(
        modifier = modifier
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp, 16.dp).background(Color(0xFF00E676)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.check_in_station_log),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val columns = 7
            val rows = monthHistory.chunked(columns)
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { dayInfo ->
                            MonthDayIcon(dayInfo = dayInfo)
                        }
                        if (rowItems.size < columns) {
                            repeat(columns - rowItems.size) { Spacer(modifier = Modifier.size(48.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthDayIcon(dayInfo: DayInfo) {
    val dayNumber = run {
        val cal = Calendar.getInstance()
        cal.time = dayInfo.date
        cal.get(Calendar.DAY_OF_MONTH).toString()
    }
    
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                color = when {
                    dayInfo.isCheckedIn -> Color(0xFF00E676).copy(alpha = 0.15f)
                    dayInfo.isToday -> Color(0xFF00B0FF).copy(alpha = 0.1f)
                    else -> Color.White.copy(alpha = 0.02f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (dayInfo.isToday) 1.5.dp else 0.5.dp,
                color = when {
                    dayInfo.isCheckedIn -> Color(0xFF00E676).copy(alpha = 0.8f)
                    dayInfo.isToday -> Color(0xFF00B0FF)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dayInfo.isCheckedIn) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF00E676)
            )
        } else {
            Text(
                text = dayNumber,
                fontSize = 13.sp,
                color = if (dayInfo.isToday) Color(0xFF00B0FF) else Color.White.copy(alpha = 0.2f),
                fontWeight = if (dayInfo.isToday) FontWeight.Black else FontWeight.Normal
            )
        }
    }
}

data class DayInfo(
    val date: Date,
    val dateStr: String,
    val isCheckedIn: Boolean,
    val isToday: Boolean,
    val dayOfWeek: String
)

private fun performCheckIn(
    context: android.content.Context, 
    alreadyCheckedIn: Boolean, 
    onSuccess: (coins: Int, days: Int) -> Unit
) {
    if (alreadyCheckedIn) return
    
    val prefs = context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = calendar.timeZone }
    val currentDate = dateFormat.format(calendar.time)
    
    val lastCheckInDate = prefs.getString("last_check_in_date", "")
    val lastConsecutiveDays = prefs.getInt("consecutive_days", 0)
    
    val yesterday = Calendar.getInstance().apply {
        timeZone = calendar.timeZone
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayStr = dateFormat.format(yesterday.time)
    
    val consecutiveDays = if (lastCheckInDate == yesterdayStr) {
        lastConsecutiveDays + 1
    } else {
        1
    }
    
    val totalCoins = (10 + (consecutiveDays * 2).coerceAtMost(40)).coerceAtMost(50)
    
    val checkedInDates = prefs.getStringSet("checked_in_dates", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    checkedInDates.add(currentDate)
    
    val lastCoinsDate = prefs.getString("last_coins_date", "")
    val editor = prefs.edit()
        .putString("last_check_in_date", currentDate)
        .putInt("consecutive_days", consecutiveDays)
        .putStringSet("checked_in_dates", checkedInDates)
    
    if (lastCoinsDate != currentDate) {
        editor.putInt("today_coins", totalCoins)
            .putString("last_coins_date", currentDate)
    }
    
    editor.apply()
    
    // 启动 10秒预热 + 双倍金币 Buff (3分 + 1分 * 连签天数)
    val buffSeconds = LevelProgressManager.calculateBuffDuration(consecutiveDays)
    LevelProgressManager.activateDoubleCoinsBuff(context, buffSeconds)
    
    LevelProgressManager.saveTotalMergeScore(context, totalCoins)
    onSuccess(totalCoins, consecutiveDays)
}
