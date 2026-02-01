package site.aiok.onepic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import site.aiok.onepic.utils.NotificationHelper
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import site.aiok.onepic.data.LevelProgressManager
import site.aiok.onepic.ui.components.MeshGradientBackground
import site.aiok.onepic.audio.SoundManager
import site.aiok.onepic.utils.LocaleHelper
import site.aiok.onepic.R
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.Edit
import android.net.Uri

@Composable
fun MoreScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // 获取用户资料
    var nickname by remember { mutableStateOf(LevelProgressManager.getNickname(context)) }
    var avatarPath by remember { mutableStateOf(LevelProgressManager.getAvatarPath(context)) }
    
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf(nickname) }
    var nicknameError by remember { mutableStateOf<String?>(null) }

    // 获取统计数据
    val totalCoins = LevelProgressManager.getTotalMergeScore(context)
    val totalStars = LevelProgressManager.getTotalStars(context)
    val classicCompleted = LevelProgressManager.getCompletedClassicLevels(context).size
    val galleryCompleted = LevelProgressManager.getCompletedGalleryLevels(context).size
    val totalCompleted = classicCompleted + galleryCompleted
    
    // 获取解锁关卡数
    val classicUnlocked = LevelProgressManager.getUnlockedClassicLevels(context).size
    val galleryUnlocked = LevelProgressManager.getUnlockedGalleryLevels(context).size
    val totalUnlocked = classicUnlocked + galleryUnlocked

    // 获取打卡数据
    val prefs = context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
    val consecutiveDays = prefs.getInt("consecutive_days", 0)

    // 通知权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            LevelProgressManager.setDailyReminderEnabled(context, true)
            NotificationHelper.scheduleDailyReminder(context)
        }
    }

    // 设置对话框状态
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // 头像选择器
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = site.aiok.onepic.utils.ProfileImageUtils.saveAvatarFromUri(context, it)
            if (savedPath != null) {
                LevelProgressManager.saveAvatarPath(context, savedPath)
                avatarPath = savedPath
            }
        }
    }

    site.aiok.onepic.ui.components.GalaxyBackground(particleTheme = "profile") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = stringResource(R.string.personal_center),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 用户头像区域 - Glassmorphism
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 头像 - 点击可编辑
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF2979FF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clip(CircleShape)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPath != null) {
                            coil.compose.AsyncImage(
                                model = avatarPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.cd_avatar),
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }
                    }
                    
                    // 用户信息 - 点击昵称编辑
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                nicknameInput = nickname
                                nicknameError = null
                                showNicknameDialog = true 
                            },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = nickname,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            // 分割线与内容区（已缩减间距）
            Spacer(modifier = Modifier.height(16.dp))
            
            // 昵称编辑对话框
            if (showNicknameDialog) {
                AlertDialog(
                    onDismissRequest = { showNicknameDialog = false },
                    containerColor = Color(0xFF1A1A1A),
                    title = {
                        Text(
                            text = stringResource(R.string.edit_nickname),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nicknameInput,
                                onValueChange = { 
                                    nicknameInput = it
                                    nicknameError = null 
                                },
                                label = { Text(stringResource(R.string.enter_nickname), color = Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF2979FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF2979FF)
                                )
                            )
                            if (nicknameError != null) {
                                Text(
                                    text = nicknameError!!,
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = nicknameInput.trim()
                                when {
                                    trimmed.length < 2 -> nicknameError = context.getString(site.aiok.onepic.R.string.nickname_too_short)
                                    trimmed.length > 12 -> nicknameError = context.getString(site.aiok.onepic.R.string.nickname_too_long)
                                    else -> {
                                        LevelProgressManager.saveNickname(context, trimmed)
                                        nickname = trimmed
                                        showNicknameDialog = false
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(site.aiok.onepic.R.string.save), color = Color(0xFF2979FF))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNicknameDialog = false }) {
                            Text(stringResource(site.aiok.onepic.R.string.cancel), color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
            
            // 功能菜单
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // 内联语言选择器
                    LanguageSelectorRow(context)

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp), 
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    MenuItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings),
                        onClick = { showSettingsDialog = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp), 
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    MenuItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about),
                        onClick = { showAboutDialog = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp), 
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    MenuItem(
                        icon = Icons.Default.Share,
                        title = stringResource(R.string.share_app),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app))
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_app)))
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp), 
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    MenuItem(
                        icon = Icons.Default.Apps,
                        title = stringResource(R.string.more_games),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=netrill.com"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // No browser or store
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 热门推荐：2048 (容器化设计 - 整合更多游戏按钮)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 0.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_2048),
                        contentDescription = "2048 Game",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=site.aiok.game2048")))
                                } catch (e: Exception) {}
                            }
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2048 Premium",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(R.string.recommend_game_title),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    
                    // 模拟下载按钮
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2979FF).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(0.5.dp, Color(0xFF2979FF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=site.aiok.game2048")))
                                } catch (e: Exception) {}
                            }
                    ) {
                        Text(
                            text = "GET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2979FF)
                        )
                    }
                }
            }
            
            // 版本信息
            Text(
                text = "OnePic v1.0.0 • System Online",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(60.dp))
        }
        
        // 设置对话框
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
        
        // 关于对话框
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}

@Composable
fun MenuSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF2979FF)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2979FF),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun LanguageSelectorRow(context: android.content.Context) {
    val languages = remember { site.aiok.onepic.utils.LocaleHelper.getSupportedLanguages(context) }
    val currentLang = remember { site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF00E676)
            )
            Text(
                text = stringResource(R.string.language),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            languages.forEach { (code, label) ->
                item {
                    val isSelected = currentLang == code
                    Surface(
                        onClick = {
                            if (!isSelected) {
                                site.aiok.onepic.utils.LocaleHelper.saveLanguage(context, code)
                                // Restart Activity
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .height(36.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF2979FF) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF2979FF) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF2979FF)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White.copy(alpha = 0.3f)
        )
    }
}

// 设置对话框 - Dark Theme
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onClearData: () -> Unit = {}
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E2C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // 音效开关
                val soundManager = remember { SoundManager.getInstance(context) }
                var soundEnabled by remember { mutableStateOf(soundManager.isEnabled()) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF2979FF)
                        )
                        Text(
                            text = stringResource(R.string.sound_effects),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { enabled ->
                            soundEnabled = enabled
                            soundManager.setEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2979FF)
                        )
                    )
                }

                // 每日提醒开关
                var reminderEnabled by remember { mutableStateOf(LevelProgressManager.isDailyReminderEnabled(context)) }
                
                // 通知权限请求 (在 Dialog 内部也需要 launcher，但这里直接用主界面的 launcher 或重新定义一个)
                // 注意：Dialog 内部使用 rememberLauncherForActivityResult 是允许的
                val dialogPermissionLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        reminderEnabled = true
                        LevelProgressManager.setDailyReminderEnabled(context, true)
                        NotificationHelper.scheduleDailyReminder(context)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF2979FF)
                        )
                        Text(
                            text = stringResource(R.string.daily_reminder),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                        reminderEnabled = true
                                        LevelProgressManager.setDailyReminderEnabled(context, true)
                                        NotificationHelper.scheduleDailyReminder(context)
                                    } else {
                                        dialogPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    reminderEnabled = true
                                    LevelProgressManager.setDailyReminderEnabled(context, true)
                                    NotificationHelper.scheduleDailyReminder(context)
                                }
                            } else {
                                reminderEnabled = false
                                LevelProgressManager.setDailyReminderEnabled(context, false)
                                NotificationHelper.cancelDailyReminder(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2979FF)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // 清除数据
                var showClearDataConfirm by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDataConfirm = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFFFF5252)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.clear_data),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF5252)
                            )
                            Text(
                                text = stringResource(R.string.clear_data_desc),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }
                
                // 清除数据确认对话框
                if (showClearDataConfirm) {
                    AlertDialog(
                        onDismissRequest = { showClearDataConfirm = false },
                        containerColor = Color(0xFF2C2C3E),
                        title = { Text(stringResource(R.string.clear_data_confirm_title), color = Color(0xFFFF5252)) },
                        text = { 
                            Text(
                                stringResource(R.string.clear_data_confirm_message),
                                textAlign = TextAlign.Start,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // 清除所有数据
                                    LevelProgressManager.resetProgress(context)
                                    // 清除打卡数据
                                    context.getSharedPreferences("check_in", android.content.Context.MODE_PRIVATE)
                                        .edit().clear().apply()
                                    // 清除合并得分（如果单独存储）
                                    context.getSharedPreferences("level_progress", android.content.Context.MODE_PRIVATE)
                                        .edit().clear().apply()
                                    
                                    // 触发主界面刷新
                                    onClearData()
                                    
                                    showClearDataConfirm = false
                                    onDismiss() // 关闭设置对话框
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                )
                            ) {
                                Text(stringResource(R.string.clear_data_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDataConfirm = false }) {
                                Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    )
                }
            }
        }
    }
}

// 关于对话框 - Dark Theme
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E2C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                
                Text(
                    text = stringResource(R.string.about),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "OnePic Puzzle\nv1.0.0\n\nDesigned by AI & Human.\nA journey through the fragmented universe.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                ) {
                    Text(stringResource(R.string.close), color = Color.White)
                }
            }
        }
    }
}


