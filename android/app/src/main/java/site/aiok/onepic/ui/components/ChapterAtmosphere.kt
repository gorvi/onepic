package site.aiok.onepic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ChapterAtmosphere(
    theme: String,
    modifier: Modifier = Modifier
) {
    // Delegate to the new ThemedParticle System
    ThemedParticleOverlay(
        theme = theme,
        modifier = modifier
    )
}
