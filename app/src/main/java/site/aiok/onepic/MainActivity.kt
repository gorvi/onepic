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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnepicTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Simple Navigation State
                    var currentScreen by remember { mutableStateOf("level_select") }
                    var selectedLevel by remember { mutableStateOf<site.aiok.onepic.model.LevelConfig?>(null) }
                    var levelCompleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                    var levelIndex by remember { mutableStateOf(0) }
                    var levelMode by remember { mutableStateOf("classic") } // classic or gallery

                    when (currentScreen) {
                        "level_select" -> {
                            site.aiok.onepic.ui.LevelSelectScreen(
                                onLevelSelected = { level, index, mode, onComplete ->
                                    selectedLevel = level
                                    levelIndex = index
                                    levelMode = mode
                                    levelCompleteCallback = onComplete
                                    currentScreen = "game"
                                }
                            )
                        }
                        "game" -> {
                            selectedLevel?.let { level ->
                                site.aiok.onepic.ui.GameScreen(
                                    levelConfig = level,
                                    levelIndex = levelIndex,
                                    levelMode = levelMode,
                                    onBack = {
                                        currentScreen = "level_select"
                                    },
                                    onLevelComplete = {
                                        levelCompleteCallback?.invoke()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
