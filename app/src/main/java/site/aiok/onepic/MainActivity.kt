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

                    when (currentScreen) {
                        "level_select" -> {
                            site.aiok.onepic.ui.LevelSelectScreen(
                                onLevelSelected = { level ->
                                    selectedLevel = level
                                    currentScreen = "game"
                                }
                            )
                        }
                        "game" -> {
                            selectedLevel?.let { level ->
                                site.aiok.onepic.ui.GameScreen(
                                    levelConfig = level,
                                    onBack = {
                                        currentScreen = "level_select"
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
