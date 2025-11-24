package site.aiok.onepic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import site.aiok.onepic.data.LevelRepository
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.ui.components.GlassBox
import site.aiok.onepic.ui.components.GlassTopBar
import site.aiok.onepic.ui.components.MeshGradientBackground

@Composable
fun LevelSelectScreen(onLevelSelected: (LevelConfig) -> Unit) {
    MeshGradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Select Level",
                modifier = Modifier.padding(16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(LevelRepository.levels) { level ->
                    LevelCard(level = level, onClick = { onLevelSelected(level) })
                }
            }
        }
    }
}

@Composable
fun LevelCard(level: LevelConfig, onClick: () -> Unit) {
    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = level.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${level.rows} x ${level.cols}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = level.difficulty,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
