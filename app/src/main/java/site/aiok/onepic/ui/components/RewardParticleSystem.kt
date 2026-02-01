package site.aiok.onepic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RewardType { COIN, STAR, COIN_X2 }

class RewardParticle(
    val id: Int,
    val type: RewardType,
    initialOffset: Offset,
    val targetOffset: Offset
) {
    var currentOffset by mutableStateOf(initialOffset)
    var alpha by mutableStateOf(1f)
    var scale by mutableStateOf(1f)
    var isArrived by mutableStateOf(false)
}

@Composable
fun rememberRewardController(): RewardController {
    val scope = rememberCoroutineScope()
    return remember { RewardController(scope) }
}

class RewardController(private val scope: kotlinx.coroutines.CoroutineScope) {
    val particles = mutableStateListOf<RewardParticle>()
    private var nextId = 0

    fun emit(
        type: RewardType,
        count: Int,
        source: Offset,
        target: Offset,
        onHit: () -> Unit = {}
    ) {
        scope.launch {
            repeat(count) { i ->
                val id = nextId++
                // Add some randomness to initial burst
                val burstOffset = Offset(
                    (Random.nextFloat() - 0.5f) * 200f,
                    (Random.nextFloat() - 0.5f) * 200f
                )
                val particle = RewardParticle(id, type, source + burstOffset, target)
                particles.add(particle)

                launch {
                    animateParticle(particle, onHit)
                }
                delay(50) // Staggered emission
            }
        }
    }

    private suspend fun animateParticle(particle: RewardParticle, onHit: () -> Unit) {
        val duration = 800 + Random.nextInt(400)
        val startTime = System.currentTimeMillis()
        val startPos = particle.currentOffset
        
        // Control points for a curved path
        val controlPoint = Offset(
            (startPos.x + particle.targetOffset.x) / 2 + (Random.nextFloat() - 0.5f) * 400f,
            (startPos.y + particle.targetOffset.y) / 2
        )

        val anim = TargetBasedAnimation(
            animationSpec = tween(duration, easing = FastOutSlowInEasing),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f
        )

        var playTime = 0L
        while (playTime < anim.durationNanos) {
            val progress = anim.getValueFromNanos(playTime)
            
            // Quadratic Bezier Curve
            val x = (1 - progress) * (1 - progress) * startPos.x + 
                    2 * (1 - progress) * progress * controlPoint.x + 
                    progress * progress * particle.targetOffset.x
            val y = (1 - progress) * (1 - progress) * startPos.y + 
                    2 * (1 - progress) * progress * controlPoint.y + 
                    progress * progress * particle.targetOffset.y
            
            particle.currentOffset = Offset(x, y)
            
            // Adjust scale and alpha
            if (progress < 0.2f) {
                particle.scale = 0.5f + (progress / 0.2f) * 0.7f
                particle.alpha = (progress / 0.2f)
            } else if (progress > 0.8f) {
                particle.scale = 1.2f - ((progress - 0.8f) / 0.2f) * 0.4f
            }
            
            delay(16)
            playTime = (System.currentTimeMillis() - startTime) * 1_000_000L
        }
        
        particle.isArrived = true
        onHit()
        delay(100)
        particles.remove(particle)
    }
}

@Composable
fun RewardAnimationOverlay(controller: RewardController) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        controller.particles.forEach { particle ->
            drawReward(particle)
        }
    }
}

private fun DrawScope.drawReward(particle: RewardParticle) {
    val radius = 12f * particle.scale
    val color = when (particle.type) {
        RewardType.COIN, RewardType.COIN_X2 -> Color(0xFFFFD700)
        RewardType.STAR -> Color(0xFFFFEB3B)
    }
    
    // Simple representation using circles/shapes on canvas
    // For a more premium look, we could draw bitamp or complex paths
    if (particle.type == RewardType.COIN) {
        drawCircle(
            color = color.copy(alpha = particle.alpha),
            radius = radius,
            center = particle.currentOffset
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f * particle.alpha),
            radius = radius * 0.6f,
            center = particle.currentOffset
        )
    } else if (particle.type == RewardType.COIN_X2) {
        // Draw coin with X2 text
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = particle.alpha),
            radius = radius * 1.2f,
            center = particle.currentOffset
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.6f * particle.alpha),
            radius = radius * 0.7f,
            center = particle.currentOffset
        )
        // Bonus: "X2" Text on canvas (optional if complex, but let's try a simple indicator or just different glow)
        drawCircle(
            color = Color.White.copy(alpha = 0.4f * particle.alpha),
            radius = radius * 1.5f,
            center = particle.currentOffset,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    } else {
        // Draw a simple star shape or glowing dot
        drawCircle(
            color = color.copy(alpha = 0.8f * particle.alpha),
            radius = radius * 1.5f,
            center = particle.currentOffset
        )
        drawCircle(
            color = Color.White.copy(alpha = particle.alpha),
            radius = radius * 0.8f,
            center = particle.currentOffset
        )
    }
}
