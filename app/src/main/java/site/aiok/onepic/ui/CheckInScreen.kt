package site.aiok.onepic.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

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
    
    MeshGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 顶部金币显示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日打卡",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    Text(
                        text = "$totalCoins",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            // 当前日期显示 - 使用系统时区，每次重新获取
            val currentDateDisplay = run {
                val calendar = Calendar.getInstance() // 使用系统默认时区
                SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE).apply {
                    timeZone = calendar.timeZone // 明确使用系统时区
                }.format(calendar.time)
            }
            Text(
                text = currentDateDisplay,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 打卡卡片 - 美化版
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = cardColor.copy(alpha = 0.3f))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (hasCheckedInToday) {
                                listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.9f),
                                    Color(0xFF66BB6A).copy(alpha = 0.8f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF2196F3).copy(alpha = 0.9f),
                                    Color(0xFF42A5F5).copy(alpha = 0.8f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(enabled = !hasCheckedInToday) {
                        performCheckIn(context, hasCheckedInToday) { coins, days ->
                            hasCheckedInToday = true
                            coinsEarned = coins
                            consecutiveDays = days
                            totalCoins = LevelProgressManager.getTotalMergeScore(context)
                            showCoinAnimation = true
                            // 2秒后隐藏动画
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(2000)
                                showCoinAnimation = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (hasCheckedInToday) {
                        // 已打卡状态
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f),
                                            Color.White.copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已打卡",
                                modifier = Modifier.size(60.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        
                        Text(
                            text = "今日已打卡",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        if (coinsEarned > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "获得",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "+$coinsEarned",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "🪙",
                                    fontSize = 20.sp
                                )
                            }
                        }
                    } else {
                        // 未打卡状态
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.3f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "打卡",
                                modifier = Modifier.size(50.dp),
                                tint = Color.White
                            )
                        }
                        
                        Text(
                            text = "点击打卡",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = "每日可获得金币奖励",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 打卡按钮 - 美化版
            Button(
                onClick = {
                    performCheckIn(context, hasCheckedInToday) { coins, days ->
                        hasCheckedInToday = true
                        coinsEarned = coins
                        consecutiveDays = days
                        totalCoins = LevelProgressManager.getTotalMergeScore(context)
                        showCoinAnimation = true
                        // 2秒后隐藏动画
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            showCoinAnimation = false
                        }
                    }
                },
                enabled = !hasCheckedInToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasCheckedInToday) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (hasCheckedInToday) 0.dp else 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                if (hasCheckedInToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "今日已打卡",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "立即打卡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 统计信息卡片组 - 统一大小
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 连续打卡天数
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        content = {
                            Text(
                                text = "连续打卡",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$consecutiveDays",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "天",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                        }
                    )
                }
                
                // 今日奖励 - 统一大小和样式
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        content = {
                            Text(
                                text = "今日奖励",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (hasCheckedInToday) "$coinsEarned" else "?",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "🪙",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 近7天打卡情况 - 当打卡状态改变时更新
            WeekCheckInHistory(
                modifier = Modifier.fillMaxWidth(),
                context = context,
                refreshTrigger = hasCheckedInToday // 当打卡状态改变时刷新
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 奖励说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 打卡奖励",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "每日打卡可获得 10-50 金币\n连续打卡天数越多，奖励越丰厚！",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// 近7天打卡历史组件
@Composable
fun WeekCheckInHistory(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    refreshTrigger: Boolean = false
) {
    val prefs = remember {
        context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    }
    
    // 获取最近7天的打卡记录 - 当 refreshTrigger 改变时重新计算
    val weekHistory = remember(refreshTrigger) {
        val calendar = Calendar.getInstance() // 使用系统时区
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = calendar.timeZone // 明确使用系统时区
        }
        val checkedInDates = prefs.getStringSet("checked_in_dates", setOf()) ?: setOf()
        
        (0..6).map { dayOffset ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val date = calendar.time
            val dateStr = dateFormat.format(date)
            val isCheckedIn = checkedInDates.contains(dateStr)
            
            DayInfo(
                date = date,
                dateStr = dateStr,
                isCheckedIn = isCheckedIn,
                isToday = dayOffset == 0,
                dayOfWeek = getDayOfWeekAbbr(calendar.get(Calendar.DAY_OF_WEEK))
            )
        }.reversed() // 反转顺序，让最早的在左边
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "近7天打卡记录",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 7天日历视图
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekHistory.forEach { dayInfo ->
                    DayCheckInItem(dayInfo = dayInfo)
                }
            }
        }
    }
}

@Composable
fun DayCheckInItem(dayInfo: DayInfo) {
    // 使用系统时区格式化日期
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault()).apply {
        timeZone = calendar.timeZone
    }
    val dayNumber = dateFormat.format(dayInfo.date)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 星期几
        Text(
            text = dayInfo.dayOfWeek,
            fontSize = 12.sp,
            color = if (dayInfo.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            fontWeight = if (dayInfo.isToday) FontWeight.Bold else FontWeight.Normal
        )
        
        // 日期圆圈
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = if (dayInfo.isCheckedIn) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF66BB6A)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clip(CircleShape)
                .then(
                    if (dayInfo.isToday && !dayInfo.isCheckedIn) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (dayInfo.isCheckedIn) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已打卡",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            } else {
                Text(
                    text = dayNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dayInfo.isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
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

private fun getDayOfWeekAbbr(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.SUNDAY -> "日"
        Calendar.MONDAY -> "一"
        Calendar.TUESDAY -> "二"
        Calendar.WEDNESDAY -> "三"
        Calendar.THURSDAY -> "四"
        Calendar.FRIDAY -> "五"
        Calendar.SATURDAY -> "六"
        else -> ""
    }
}

// 执行打卡逻辑
private fun performCheckIn(
    context: android.content.Context,
    alreadyCheckedIn: Boolean,
    onSuccess: (coins: Int, days: Int) -> Unit
) {
    if (alreadyCheckedIn) return
    
    // 获取上次打卡日期 - 使用系统时区
    val prefs = context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    val lastCheckInDate = prefs.getString("last_check_in_date", "")
    val calendar = Calendar.getInstance() // 使用系统时区
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = calendar.timeZone // 明确使用系统时区
    }
    val currentDate = dateFormat.format(calendar.time)
    val lastConsecutiveDays = prefs.getInt("consecutive_days", 0)
    
    // 计算连续打卡天数
    val consecutiveDays = if (lastCheckInDate == currentDate) {
        // 今天已经打卡过了
        lastConsecutiveDays
    } else {
        val yesterday = Calendar.getInstance().apply {
            timeZone = calendar.timeZone // 确保使用相同的时区
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayStr = dateFormat.format(yesterday.time)
        
        if (lastCheckInDate == yesterdayStr) {
            // 连续打卡
            lastConsecutiveDays + 1
        } else {
            // 中断了，重新开始
            1
        }
    }
    
    // 计算奖励金币（基础10金币 + 连续天数奖励，最多50金币）
    val baseCoins = 10
    val bonusCoins = (consecutiveDays * 2).coerceAtMost(40)
    val totalCoins = (baseCoins + bonusCoins).coerceAtMost(50)
    
    // 保存打卡记录 - 同时保存到日期集合中
    val checkedInDates = prefs.getStringSet("checked_in_dates", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    checkedInDates.add(currentDate)
    
    // 清理7天前的记录（每周更新）- 使用系统时区
    val cleanCalendar = Calendar.getInstance()
    cleanCalendar.timeZone = calendar.timeZone // 使用相同的系统时区
    val cleanDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = calendar.timeZone
    }
    val sevenDaysAgo = cleanCalendar.apply {
        add(Calendar.DAY_OF_YEAR, -7)
    }.time
    checkedInDates.removeAll { dateStr ->
        try {
            val date = cleanDateFormat.parse(dateStr)
            date != null && date.before(sevenDaysAgo)
        } catch (e: Exception) {
            false
        }
    }
    
    // 如果日期改变，重置今日奖励
    val lastCoinsDate = prefs.getString("last_coins_date", "")
    val editor = prefs.edit()
        .putString("last_check_in_date", currentDate)
        .putInt("consecutive_days", consecutiveDays)
        .putStringSet("checked_in_dates", checkedInDates)
    
    // 只有今天第一次打卡时才保存奖励，如果日期变了说明是新的一天
    if (lastCoinsDate != currentDate) {
        editor.putInt("today_coins", totalCoins)
            .putString("last_coins_date", currentDate)
    } else {
        // 如果今天已经打卡过，保持之前的奖励
        editor.putInt("today_coins", prefs.getInt("today_coins", totalCoins))
    }
    
    editor.apply()
    
    // 增加金币到总分数
    LevelProgressManager.saveTotalMergeScore(context, totalCoins)
    
    // 回调
    onSuccess(totalCoins, consecutiveDays)
}
