package site.aiok.onepic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.ui.components.MeshGradientBackground
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoreScreen() {
    val context = LocalContext.current
    
    // 获取用户数据 - 直接读取，确保每次组合时获取最新数据
    val totalCoins = LevelProgressManager.getTotalMergeScore(context)
    val totalStars = LevelProgressManager.getTotalStars(context)
    val classicCompleted = LevelProgressManager.getCompletedClassicLevels(context).size
    val galleryCompleted = LevelProgressManager.getCompletedGalleryLevels(context).size
    val totalCompleted = classicCompleted + galleryCompleted
    
    // 获取解锁关卡数
    val classicUnlocked = remember {
        LevelProgressManager.getUnlockedClassicLevels(context).size
    }
    val galleryUnlocked = remember {
        LevelProgressManager.getUnlockedGalleryLevels(context).size
    }
    val totalUnlocked = classicUnlocked + galleryUnlocked
    
    // 获取打卡数据 - 直接读取，确保实时更新
    val prefs = context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    val consecutiveDays = prefs.getInt("consecutive_days", 0)
    
    MeshGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = "个人中心",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 用户头像区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3),
                                        Color(0xFF42A5F5)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "头像",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                    
                    // 用户信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "拼图大师",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${System.currentTimeMillis().toString().takeLast(6)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 统计数据 - 4个卡片
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 第一行：完成关卡和总星星
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        title = "完成",
                        value = "$totalCompleted",
                        unit = null,
                        gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                    )
                    
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        title = "星星",
                        value = "$totalStars",
                        unit = null,
                        gradientColors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                    )
                }
                
                // 第二行：总金币和连续打卡
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = null,
                        title = "金币",
                        value = "$totalCoins",
                        unit = "🪙",
                        gradientColors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                    )
                    
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        title = "打卡",
                        value = "$consecutiveDays",
                        unit = "天",
                        gradientColors = listOf(Color(0xFF2196F3), Color(0xFF42A5F5))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 额外统计信息卡片 - 使用图标和简短文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemWithIcon(
                        icon = Icons.Default.CheckCircle,
                        title = "解锁", // 用CheckCircle表示已解锁，更直观
                        value = "$totalUnlocked",
                        iconColor = Color(0xFF2196F3)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(50.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    StatItemWithIcon(
                        icon = Icons.Default.CheckCircle,
                        title = "完成率", // 用CheckCircle表示完成，更直观
                        value = if (totalUnlocked > 0) {
                            "${(totalCompleted * 100 / totalUnlocked).coerceAtMost(100)}"
                        } else {
                            "0"
                        },
                        iconColor = Color(0xFF4CAF50)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(50.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    StatItemWithIcon(
                        icon = Icons.Default.Star,
                        title = "经典", // 使用完整中文，更直观
                        value = "$classicCompleted",
                        iconColor = Color(0xFFFFD700)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(50.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    StatItemWithIcon(
                        icon = Icons.Default.CheckCircle,
                        title = "画廊", // 使用完整中文，更直观
                        value = "$galleryCompleted",
                        iconColor = Color(0xFF66BB6A)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 功能菜单
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    MenuItem(
                        icon = Icons.Default.Settings,
                        title = "设置",
                        onClick = { /* TODO: 打开设置 */ }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    MenuItem(
                        icon = Icons.Default.Info,
                        title = "关于",
                        onClick = { /* TODO: 打开关于 */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 版本信息
            Text(
                text = "OnePic v1.0.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: String?,
    value: String,
    unit: String?,
    gradientColors: List<Color>
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.2f) }
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：文字标签
                if (title != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 底部：图标/图案在前，数字在后
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 图标或单位（图案）
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = gradientColors[0]
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (unit != null) {
                        Text(
                            text = unit,
                            fontSize = 28.sp,
                            color = gradientColors[0],
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    
                    // 数字
                    Text(
                        text = value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = gradientColors[0],
                        maxLines = 1
                    )
                    
                    // 单位（如果有图标且有单位）
                    if (icon != null && unit != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.widthIn(min = 60.dp, max = 80.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatItemWithIcon(
    icon: ImageVector,
    title: String,
    value: String,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(min = 60.dp, max = 90.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconColor
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
