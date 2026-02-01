package site.aiok.onepic.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun LevelRow(
    idx: Int,
    level: site.aiok.onepic.model.LevelConfig,
    unlockedLevels: Set<Int>,
    completedLevels: Set<Int>,
    completedAscendedIds: Set<Int>,
    newlyCompletedLevels: Set<Int>,
    previousNextToPlayIndex: Int,
    sharedAnim: site.aiok.onepic.ui.SharedAnimationState,
    onLevelSelected: (site.aiok.onepic.model.LevelConfig, Int, String, () -> Unit) -> Unit,
    onCompleteLevel: (Int) -> Unit,
    onUnlockLevel: (Int) -> Unit,
    onCompleteAscendedLevel: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ctx = context
    
    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        val isUnlocked = idx in unlockedLevels || idx == 0
        val isCompleted = idx in completedLevels
        val stars = androidx.compose.runtime.remember(idx) { 
            site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, idx) 
        }
        
        // Ascended Logic
        val levelId = idx // ID matches index
        val ascendedLevel = androidx.compose.runtime.remember(levelId) { site.aiok.onepic.data.LevelRepository.getAscendedLevel(context, levelId) }
        val isAscendedUnlocked = isCompleted // Concept: Finish Normal -> Unlock Ascended
        val isAscendedCompleted = completedAscendedIds.contains(levelId)

        val isNextToPlay = isUnlocked && !isCompleted && 
            (idx == 0 || completedLevels.contains(idx - 1))
        
        val offsetX = androidx.compose.runtime.remember(idx) {
            when (idx % 4) {
                0 -> 0.dp
                1 -> 60.dp
                2 -> 0.dp
                3 -> (-60).dp
                else -> 0.dp
            }
        }
        
        // Stage Index Logic
        val stageIndex = if (idx == 0) 0 else (idx - 1) / 5 + 1
        val isFirstOfStage = idx == 0 || (idx > 0 && (idx - 1) % 5 == 0)
        
        // Universal Path Connection (Connect to Previous Level)
        if (idx > 0) {
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))
            site.aiok.onepic.ui.components.PathConnector(
                fromOffset = when ((idx - 1) % 4) {
                    0 -> 0.dp
                    1 -> 60.dp
                    2 -> 0.dp
                    3 -> (-60).dp
                    else -> 0.dp
                },
                toOffset = offsetX,
                isCompleted = completedLevels.contains(idx - 1),
                stageIndex = stageIndex
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))
        }
        
        val displayLevelNumber = if (idx == 0) "S" else idx.toString()

        // ...Stars and Node Logic (Remains unchanged)
        val ascendedStars = androidx.compose.runtime.remember(levelId) {
             site.aiok.onepic.data.LevelProgressManager.getAscendedLevelStars(context, levelId)
        }

        site.aiok.onepic.ui.components.LevelNode(
            level = level,
            levelNumber = idx,
            displayLevelNumber = displayLevelNumber,
            isLocked = !isUnlocked,
            isCompleted = isCompleted,
            isNextToPlay = isNextToPlay,
            stars = stars,
            ascendedStars = ascendedStars, 
            offsetX = offsetX,
            stageIndex = stageIndex,
            sharedAnim = sharedAnim,
            previousNextToPlayIndex = previousNextToPlayIndex,
            isNewlyCompleted = idx in newlyCompletedLevels,
            ascendedLevel = ascendedLevel,
            isAscendedUnlocked = isAscendedUnlocked,
            isAscendedCompleted = isAscendedCompleted,
            onAscendedClick = {
                if (ascendedLevel != null && isAscendedUnlocked) {
                    onLevelSelected(ascendedLevel, levelId, "ascended") {
                        site.aiok.onepic.data.LevelProgressManager.markAscendedLevelCompleted(ctx, levelId)
                        onCompleteAscendedLevel(levelId)
                    }
                }
            },
            onClick = {
                if (isUnlocked) {
                    onLevelSelected(level, idx, "classic") {
                        site.aiok.onepic.data.LevelProgressManager.markClassicLevelCompleted(ctx, idx)
                        site.aiok.onepic.data.LevelProgressManager.unlockClassicLevel(ctx, idx + 1)
                        onCompleteLevel(idx)
                        onUnlockLevel(idx + 1)
                    }
                }
            }
        )
        
        // Stage Divider Logic (Always at the end of the first level row, visually AFTER the node)
        if (idx == 0) {
            // Newcomer Guide (Stage 0) below the tutorial node
            site.aiok.onepic.ui.components.StageDivider(stageIndex = 0, sharedAnim = sharedAnim)
        } else if (idx == 1) {
            // Chapter 1 (Stage 1) below level 1
            site.aiok.onepic.ui.components.StageDivider(stageIndex = 1, sharedAnim = sharedAnim)
        } else if (idx > 1 && (idx - 1) % 5 == 0) {
            // Subsequent chapters (Stage 2+) every 5 levels
            // Level 6 is the start of Stage 2 (Index 2)
            // Level 11 is Stage 3 (Index 3)
            val shiftedStageIndex = (idx - 1) / 5 + 1
            site.aiok.onepic.ui.components.StageDivider(stageIndex = shiftedStageIndex, sharedAnim = sharedAnim)
        }
    }
}
