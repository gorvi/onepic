package site.aiok.onepic.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GalaxyBackground(
    particleTheme: String = "magic",
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0221), // Deepest Void Purple
                        Color(0xFF190638), // Dark Nebula
                        Color(0xFF0F2027), // Deep Space Blue
                        Color(0xFF000000)  // Black
                    )
                )
            )
    ) {
        // Render Themed Particles instead of old smoke particles
        ThemedParticleOverlay(theme = particleTheme)
        
        // Content Overlay
        content()
    }
}

@Composable
fun SmokeParticleSystem() {
    val particleCount = 15 // Not too many to avoid clutter
    
    val particles = remember {
        List(particleCount) { SmokeParticle() }
    }
    
    // Animation loop
    var time by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTime ->
                time = frameTime
            }
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        particles.forEach { p ->
            p.update(width, height, time)
            p.draw(this)
        }
    }
}

private class SmokeParticle {
    var x = Random.nextFloat()
    var y = Random.nextFloat()
    
    // Large, soft particles
    var radiusBase = 100f + Random.nextFloat() * 200f
    
    var speedX = (Random.nextFloat() - 0.5f) * 0.05f // Very flow drift
    var speedY = (Random.nextFloat() - 0.5f) * 0.05f
    
    var rotation = Random.nextFloat() * 360f
    var rotationSpeed = (Random.nextFloat() - 0.5f) * 0.2f
    
    var alphaBase = 0.05f + Random.nextFloat() * 0.1f // Very faint
    var phase = Random.nextFloat() * 6.28f
    
    var color = if (Random.nextBoolean()) Color(0xFF6A1B9A) else Color(0xFF283593) // Purple or Indigo
    
    fun update(width: Float, height: Float, frameTime: Long) {
        // Slow drift
        x += speedX
        y += speedY
        
        rotation += rotationSpeed
        
        // Wrap around
        if (x < -0.2f) x = 1.2f
        if (x > 1.2f) x = -0.2f
        if (y < -0.2f) y = 1.2f
        if (y > 1.2f) y = -0.2f
    }
    
    fun draw(scope: DrawScope) {
        val cx = x * scope.size.width
        val cy = y * scope.size.height
        
        // Breathe alpha
        val currentAlpha = alphaBase + (sin((scope.size.width.toLong() + phase).toDouble()).toFloat() * 0.02f)
        
        scope.translate(left = cx, top = cy) {
             rotate(rotation) {
                 scale(scaleX = 2f, scaleY = 1f) { // Stretch to look like wisps
                     drawCircle(
                         color = color.copy(alpha = currentAlpha.coerceIn(0f, 1f)),
                         radius = radiusBase * scope.density,
                     )
                 }
             }
        }
    }
}
