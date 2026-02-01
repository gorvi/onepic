package site.aiok.onepic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType {
    LEAF, BUBBLE, SPARKLE, EMBER, SNOW, FISH, GEAR, STAR, FRAGMENT
}

data class Particle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var alpha: Float,
    var type: ParticleType,
    var color: Color,
    val initialPhase: Float = Random.nextFloat() * 2 * PI.toFloat(),
    var directionX: Int = 1 // 1 for right, -1 for left (For Fish)
)

@Composable
fun ParticleOverlay(
    type: ParticleType,
    density: Int = 20, // Number of particles
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val densityPx = LocalDensity.current.density
        val widthPx = maxWidth.value * densityPx
        val heightPx = maxHeight.value * densityPx
        
        // We'll maintain particles in a state holder
        val particles = remember(type, widthPx, heightPx) {
            val list = ArrayList<Particle>()
            // Ensure we have valid dimensions before spawning
            if (widthPx > 0 && heightPx > 0) {
                repeat(density) {
                    list.add(generateRandomParticle(type, widthPx, heightPx, true)) 
                }
            }
            list
        }
        
        // Game Loop
        var time by remember { mutableLongStateOf(0L) }
        
        LaunchedEffect(type) {
            val startTime = System.nanoTime()
            while (isActive) {
                withFrameNanos { now ->
                    time = now - startTime
                }
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // If dimensions mismatch significantly (e.g. rotation), we might want to handle it, 
            // but usually BoxWithConstraints recomposes.
            
            particles.forEach { p ->
                // Update
                when (p.type) {
                    ParticleType.LEAF -> {
                        p.y += p.speedY
                        p.x += sin(time / 1_000_000_000f + p.initialPhase) * 1.5f 
                        p.rotation += p.rotationSpeed
                    }
                    ParticleType.BUBBLE -> {
                        p.y -= p.speedY 
                        p.x += cos(time / 800_000_000f + p.initialPhase) * 0.5f 
                    }
                    ParticleType.SNOW -> {
                        p.y += p.speedY
                        p.x += p.speedX + sin(time / 1_500_000_000f + p.initialPhase) * 0.5f
                    }
                    ParticleType.GEAR -> {
                        p.y += p.speedY
                        p.rotation += p.rotationSpeed
                    }
                    ParticleType.FRAGMENT -> {
                        p.y += p.speedY
                        p.x += p.speedX
                        p.rotation += p.rotationSpeed
                    }
                    ParticleType.STAR -> {
                        p.y += p.speedY * 0.2f // Very slow descent
                        p.x += p.speedX * 0.2f
                        // Twinkle effect using alpha
                        p.alpha = 0.3f + abs(sin(time / 500_000_000f + p.initialPhase)) * 0.7f
                    }
                    ParticleType.FISH -> {
                        // Fish swim horizontally
                        p.x += p.speedX * p.directionX
                        p.y += sin(time / 500_000_000f + p.initialPhase) * 0.5f // Bobbing up and down
                        
                        // Turn around at edges with padding
                        // Use canvasWidth for bounds check
                        if (p.x > canvasWidth + 100 && p.directionX == 1) {
                             p.directionX = -1
                             p.y = Random.nextFloat() * canvasHeight
                        } else if (p.x < -100 && p.directionX == -1) {
                            p.directionX = 1
                            p.y = Random.nextFloat() * canvasHeight
                        }
                    }
                    else -> {
                        p.y += p.speedY
                        p.x += p.speedX
                    }
                }
                
                // Boundary checks & Recycle (Except Fish which manage themselves)
                if (p.type != ParticleType.FISH) {
                    if (p.type == ParticleType.BUBBLE) {
                        if (p.y < -50) resetParticle(p, type, canvasWidth, canvasHeight)
                    } else {
                        if (p.y > canvasHeight + 50) resetParticle(p, type, canvasWidth, canvasHeight)
                    }
                }
                
                // Draw
                withTransform({
                    translate(left = p.x, top = p.y)
                    // For fish, flip horizontally if moving left
                    if (p.type == ParticleType.FISH && p.directionX == -1) {
                        scale(scaleX = -1f, scaleY = 1f)
                    }
                    if (p.type != ParticleType.FISH) {
                        rotate(degrees = p.rotation)
                    }
                }) {
                    when (p.type) {
                    ParticleType.LEAF -> {
                        drawOval(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(-p.size / 2, -p.size),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size * 2)
                        )
                    }
                    ParticleType.BUBBLE -> {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = p.size / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                    ParticleType.GEAR -> {
                        // Simple gear shape (Cross)
                        val gearPath = Path().apply {
                            moveTo(-p.size/2, -p.size/2)
                            lineTo(p.size/2, p.size/2)
                            moveTo(p.size/2, -p.size/2)
                            lineTo(-p.size/2, p.size/2)
                        }
                        drawPath(path = gearPath, color = p.color.copy(alpha = p.alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size / 4)
                    }
                    ParticleType.STAR -> {
                        // Diamond shape for star
                        val starPath = Path().apply {
                            moveTo(0f, -p.size)
                            lineTo(p.size/2, 0f)
                            lineTo(0f, p.size)
                            lineTo(-p.size/2, 0f)
                            close()
                        }
                        drawPath(path = starPath, color = p.color.copy(alpha = p.alpha))
                    }
                    ParticleType.FRAGMENT -> {
                        // Random polygon/triangle for void fragments
                        val fragPath = Path().apply {
                            moveTo(0f, -p.size/2)
                            lineTo(p.size/2, p.size/2)
                            lineTo(-p.size/2, p.size/2)
                            close()
                        }
                        drawPath(path = fragPath, color = p.color.copy(alpha = p.alpha))
                    }
                    ParticleType.FISH -> {
                        // Draw a simple fish shape
                        val fishPath = Path().apply {
                            // Body
                            moveTo(-p.size, 0f)
                            quadraticBezierTo(0f, -p.size/1.5f, p.size, 0f)
                            quadraticBezierTo(0f, p.size/1.5f, -p.size, 0f)
                            // Tail
                            moveTo(-p.size, 0f)
                            lineTo(-p.size * 1.5f, -p.size/2)
                            lineTo(-p.size * 1.5f, p.size/2)
                            close()
                        }
                         drawPath(path = fishPath, color = p.color.copy(alpha = p.alpha))
                         
                         // Eye
                         drawCircle(Color.White, radius = p.size/8, center = Offset(p.size/2, -p.size/4))
                         drawCircle(Color.Black, radius = p.size/16, center = Offset(p.size/2, -p.size/4))
                    }
                    else -> {
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size / 2)
                    }
                }
            }
        }
    }
}
}

fun generateRandomParticle(type: ParticleType, w: Float, h: Float, randomY: Boolean): Particle {
    val x = Random.nextFloat() * w
    val y = if (randomY) Random.nextFloat() * h else (if (type == ParticleType.BUBBLE) h + 20f else -20f)
    
    return when (type) {
        ParticleType.LEAF -> Particle(
            x = x, y = y,
            size = 15f + Random.nextFloat() * 15f,
            speedY = 1.5f + Random.nextFloat() * 2f,
            speedX = 0f,
            rotation = Random.nextFloat() * 360,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 4f,
            alpha = 0.6f + Random.nextFloat() * 0.4f,
            type = type,
            color = if (Random.nextBoolean()) Color(0xFF81C784) else Color(0xFFAED581)
        )
        ParticleType.BUBBLE -> Particle(
            x = x, y = y,
            size = 10f + Random.nextFloat() * 15f,
            speedY = 1.5f + Random.nextFloat() * 2.5f,
            speedX = 0f,
            rotation = 0f,
            rotationSpeed = 0f,
            alpha = 0.4f + Random.nextFloat() * 0.4f,
            type = type,
            color = Color(0xFFE1F5FE)
        )
        ParticleType.GEAR -> Particle(
            x = x, y = y,
            size = 20f + Random.nextFloat() * 15f,
            speedY = 1.0f + Random.nextFloat() * 1.5f,
            speedX = 0f,
            rotation = Random.nextFloat() * 360,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
            alpha = 0.3f + Random.nextFloat() * 0.3f,
            type = type,
            color = Color(0xFFAFAFAF)
        )
        ParticleType.STAR -> Particle(
            x = x, y = y,
            size = 4f + Random.nextFloat() * 8f,
            speedY = 0.5f,
            speedX = (Random.nextFloat() - 0.5f) * 1f,
            rotation = Random.nextFloat() * 360,
            rotationSpeed = 0.5f,
            alpha = 0.8f,
            type = type,
            color = Color.White
        )
        ParticleType.FRAGMENT -> Particle(
            x = x, y = y,
            size = 10f + Random.nextFloat() * 10f,
            speedY = 1.5f + Random.nextFloat() * 2f,
            speedX = (Random.nextFloat() - 0.5f) * 2f,
            rotation = Random.nextFloat() * 360,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 6f,
            alpha = 0.5f + Random.nextFloat() * 0.4f,
            type = type,
            color = Color(0xFFCE93D8)
        )
        ParticleType.FISH -> Particle(
            x = x, y = y,
            size = 20f + Random.nextFloat() * 20f,
            speedY = 0f,
            speedX = 1.0f + Random.nextFloat() * 1.5f,
            rotation = 0f,
            rotationSpeed = 0f,
            alpha = 0.8f + Random.nextFloat() * 0.2f,
            type = type,
            color = listOf(Color(0xFFFF7043), Color(0xFFFFCA28), Color(0xFF42A5F5), Color(0xFFEF5350)).random(),
            directionX = if(Random.nextBoolean()) 1 else -1 
        )
        else -> Particle(
            x = x, y = y,
            size = 10f, speedY = 2f, speedX = 0f, rotation = 0f, rotationSpeed = 0f, alpha = 1f, type = type, color = Color.White
        )
    }
}

fun resetParticle(p: Particle, type: ParticleType, w: Float, h: Float) {
    if(type == ParticleType.FISH) return 
    
    val newP = generateRandomParticle(type, w, h, false) 
    p.x = newP.x
    p.y = newP.y
    p.size = newP.size
    p.speedY = newP.speedY
    p.alpha = newP.alpha
    p.rotation = newP.rotation
    p.color = newP.color 
    p.type = type 
}
