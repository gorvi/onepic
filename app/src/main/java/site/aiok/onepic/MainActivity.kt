package site.aiok.onepic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.theme.OnepicTheme
import site.aiok.onepic.ui.components.BottomNavigationBar
import site.aiok.onepic.ui.components.BottomNavItem
import site.aiok.onepic.utils.LocaleHelper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 语言设置已在 attachBaseContext 中处理
        enableEdgeToEdge()
        setContent {
            OnepicTheme {
                // 底部导航栏的当前路由
                var currentBottomNavRoute by remember { mutableStateOf(BottomNavItem.Home.route) }
                // 游戏相关的导航状态
                var currentScreen by remember { mutableStateOf("level_select") }
                var selectedLevel by remember { mutableStateOf<site.aiok.onepic.model.LevelConfig?>(null) }
                var levelCompleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var levelIndex by remember { mutableStateOf(0) }
                var levelMode by remember { mutableStateOf("classic") } // classic or gallery
                // 用于触发关卡选择页面重新检查滚动
                var levelSelectKey by remember { mutableStateOf(0) }
                // 保存进入游戏前的 completedLevels，用于检测是否完成了新关卡
                var completedLevelsBeforeGame by remember { mutableStateOf<Set<Int>>(emptySet()) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // 只在非游戏页面显示底部导航栏
                        if (currentScreen != "game") {
                            BottomNavigationBar(
                                currentRoute = currentBottomNavRoute,
                                onNavigate = { route ->
                                    currentBottomNavRoute = route
                                    // 如果切换到首页，确保显示关卡选择页面
                                    if (route == BottomNavItem.Home.route) {
                                        currentScreen = "level_select"
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    // 根据底部导航栏路由和游戏状态显示不同页面
                    when {
                        // 游戏页面（优先级最高）
                        currentScreen == "game" -> {
                            selectedLevel?.let { level ->
                                site.aiok.onepic.ui.GameScreen(
                                    levelConfig = level,
                                    levelIndex = levelIndex,
                                    levelMode = levelMode,
                                    onBack = {
                                        // 返回到之前的底部导航页面（通常是首页）
                                        currentScreen = "level_select"
                                        currentBottomNavRoute = BottomNavItem.Home.route
                                        
                                        // 检查是否完成了关卡（通过检查 completedLevels 是否增加）
                                        val context = this@MainActivity
                                        val completedLevels = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(context)
                                        
                                        // 检查是否有新完成的关卡
                                        val newCompletedLevels = completedLevels - completedLevelsBeforeGame
                                        
                                        // 如果有新完成的关卡，增加 key 以触发动画滚动到下一关
                                        if (newCompletedLevels.isNotEmpty()) {
                                            levelSelectKey++
                                        }
                                    },
                                    onLevelComplete = {
                                        // 只执行完成回调，不在这里触发动画
                                        levelCompleteCallback?.invoke()
                                    }
                                )
                            }
                        }
                        // 底部导航栏页面
                        currentBottomNavRoute == BottomNavItem.Home.route -> {
                            site.aiok.onepic.ui.LevelSelectScreen(
                                scrollTriggerKey = levelSelectKey, // 用于触发滚动的 key
                                completedLevelsBeforeGame = completedLevelsBeforeGame, // 传入进入游戏前的 completedLevels
                                onLevelSelected = { level, index, mode, onComplete ->
                                    // 保存最后玩的关卡索引
                                    site.aiok.onepic.data.LevelProgressManager.saveLastPlayedLevel(
                                        this@MainActivity, index
                                    )
                                    // 保存进入游戏前的 completedLevels
                                    completedLevelsBeforeGame = site.aiok.onepic.data.LevelProgressManager.getCompletedClassicLevels(this@MainActivity)
                                    selectedLevel = level
                                    levelIndex = index
                                    levelMode = mode
                                    levelCompleteCallback = onComplete
                                    currentScreen = "game"
                                }
                            )
                        }
                        currentBottomNavRoute == BottomNavItem.CheckIn.route -> {
                            site.aiok.onepic.ui.CheckInScreen()
                        }
                        currentBottomNavRoute == BottomNavItem.More.route -> {
                            site.aiok.onepic.ui.MoreScreen()
                        }
                    }
                }
            }
        }
    }
    
    // 注意：updateLocale 方法已不再需要，语言切换通过 attachBaseContext 处理
    
    override fun attachBaseContext(newBase: android.content.Context) {
        val savedLanguage = LocaleHelper.getSavedLanguage(newBase)
        val locale = when (savedLanguage) {
            "en" -> Locale.ENGLISH
            "zh" -> Locale.CHINESE
            else -> {
                // 根据系统语言自动选择
                val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    newBase.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    newBase.resources.configuration.locale
                }
                when (systemLocale.language) {
                    "en" -> Locale.ENGLISH
                    "zh" -> Locale.CHINESE
                    else -> Locale.CHINESE // 默认中文
                }
            }
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
