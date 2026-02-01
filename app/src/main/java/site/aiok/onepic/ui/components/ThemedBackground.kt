
package site.aiok.onepic.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

// Themes
data class ParticleThemeConfig(
    val colors: List<Color>,
    val speedMultiplier: Float = 1f,
    val sizeBase: Float = 100f,
    val count: Int = 10,
    val style: ParticleStyle = ParticleStyle.FLOAT,
    val shape: ParticleShape = ParticleShape.CIRCLE
)

enum class ParticleStyle {
    FLOAT, // Gentle drift in all directions (Default)
    RISE,  // Drift upwards (Fire, Magic)
    FALL,  // Drift downwards (Nature, Ice)
    ORBIT  // Slight rotational bias
}

enum class ParticleShape {
    CIRCLE,
    RING,
    SQUARE,
    TRIANGLE,
    DIAMOND,
    HEXAGON,
    STAR
}

enum class VisitorType {
    METEOR,
    SATELLITE,
    UFO
}

class CelestialVisitor(
    val type: VisitorType,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val speed: Float,
    val size: Float,
    val color: Color, // New randomized color
    val startScale: Float = 1f,
    val targetScale: Float = 1f
) {
    var progress = 0f // 0 to 1
    val id = Random.nextInt()

    fun update(delta: Float) {
        progress += delta * speed
    }

    fun isFinished() = progress >= 1.0f

    fun draw(scope: DrawScope) {
        val currentX = startX + (targetX - startX) * progress
        val currentY = startY + (targetY - startY) * progress
        val currentScale = startScale + (targetScale - startScale) * progress
        
        val screenX = currentX * scope.size.width
        val screenY = currentY * scope.size.height

        when (type) {
            VisitorType.METEOR -> drawMeteor(scope, screenX, screenY, currentScale)
            VisitorType.SATELLITE -> drawSatellite(scope, screenX, screenY, currentScale)
            VisitorType.UFO -> drawUFO(scope, screenX, screenY, currentScale)
        }
    }

    private fun drawMeteor(scope: DrawScope, x: Float, y: Float, scale: Float) {
        val size = this.size * scale // Apply depth scale
        val dx = targetX - startX
        val dy = targetY - startY
        val pathLen = kotlin.math.sqrt(dx * dx + dy * dy)
        val dirX = dx / pathLen
        val dirY = dy / pathLen
        
        val time = System.currentTimeMillis()
        
        // 动态变色：在原始颜色基础上增加平滑偏移
        // 注意：Compose Color 的分量可能是通过函数访问的
        val dynamicColor = Color(
            red = (color.red + sin(time / 800.0 + id).toFloat() * 0.15f).coerceIn(0f, 1f),
            green = (color.green + cos(time / 900.0 + id).toFloat() * 0.15f).coerceIn(0f, 1f),
            blue = (color.blue + sin(time / 1000.0 + id).toFloat() * 0.15f).coerceIn(0f, 1f),
            alpha = color.alpha
        )

        // 随机尾部长度控制：忽长忽短
        val trailLengthFactor = 1f + (sin(time / 450.0 + id * 0.3).toFloat() * 0.6f) // 0.4 ~ 1.6 倍比例
        
        // Multi-layered Meteor Trail (Aligned perfectly opposite to movement)
        for (i in 0 until 12) {
            val alpha = (1f - i / 12f) * 0.5f
            val offset = i * 18f * trailLengthFactor 
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dynamicColor.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x - dirX * offset, y - dirY * offset),
                    radius = (size * 2f * (1f - i / 15f)) * scope.density
                ),
                radius = (size * 2f * (1f - i / 15f)) * scope.density,
                center = Offset(x - dirX * offset, y - dirY * offset)
            )
        }
        
        // Heat Shield Glow
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), dynamicColor.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(x, y),
                radius = size * 1.5f * scope.density
            ),
            radius = size * 1.5f * scope.density,
            center = Offset(x, y)
        )

        // Meteor Core
        scope.drawCircle(
            color = Color.White,
            radius = size * 0.8f * scope.density,
            center = Offset(x, y)
        )
    }

    private fun drawSatellite(scope: DrawScope, x: Float, y: Float, scale: Float) {
        val size = this.size * scale // Apply depth scale
        val satSize = size * 1.8f * scope.density
        val time = System.currentTimeMillis()
        
        // 1. Solar Panels with "Scanning Sheen"
        val sheenOffset = (time % 2000) / 2000f * (satSize * 2)
        val panelBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF1976D2), Color.White.copy(alpha = 0.4f), Color(0xFF1976D2)),
            start = Offset(x - satSize + sheenOffset - 20f, y),
            end = Offset(x - satSize + sheenOffset + 20f, y)
        )

        // Left Panel
        scope.drawRect(
            brush = panelBrush,
            topLeft = Offset(x - satSize, y - satSize / 6),
            size = Size(satSize / 1.8f, satSize / 3)
        )
        // Right Panel
        scope.drawRect(
            brush = panelBrush,
            topLeft = Offset(x + satSize / 2.2f, y - satSize / 6),
            size = Size(satSize / 1.8f, satSize / 3)
        )

        // 2. Metallic Body (Silver Gradient)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF78909C))
            ),
            topLeft = Offset(x - satSize / 3, y - satSize / 4),
            size = Size(satSize / 1.5f, satSize / 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
        )

        // 3. Oscillating Antenna
        val antennaOsc = kotlin.math.sin(time / 150.0).toFloat() * 5f
        scope.drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(x, y - satSize / 4),
            end = Offset(x + antennaOsc, y - satSize / 1.5f),
            strokeWidth = 1.5f * scope.density
        )

        // 4. Randomized Blinking LED
        val blinkAlpha = (kotlin.math.sin(time / 200.0).toFloat() + 1f) / 2f
        scope.drawCircle(
            color = color.copy(alpha = blinkAlpha), // Use random color for LED
            radius = 2.5f * scope.density,
            center = Offset(x, y)
        )
    }

    private fun drawUFO(scope: DrawScope, x: Float, y: Float, scale: Float) {
        val size = this.size * scale
        val ufoWidth = size * 4.5f * scope.density
        val discHeight = ufoWidth * 0.22f
        val domeWidth = ufoWidth * 0.5f
        val domeHeight = domeWidth * 0.4f
        val time = System.currentTimeMillis()
        
        val rotationAngle = (time / 1000.0 * 3.5).toFloat() 

        // 优化点 1：不再使用 List<Object> 过滤，改用单次循环两阶段绘制或两轮循环节省内存
        // 第一次循环：仅绘制背面灯光
        val lightCount = 8
        for (i in 0 until lightCount) {
            val baseAngle = (i * 2.0 * kotlin.math.PI / lightCount).toFloat()
            val totalAngle = baseAngle + rotationAngle
            val z = sin(totalAngle)
            
            if (z <= 0) { // 背面
                val lx = x + cos(totalAngle) * (ufoWidth * 0.48f) 
                val ly = y + sin(totalAngle) * (discHeight * 0.5f)
                val depthScale = 0.7f + (z + 1f) * 0.35f
                val depthAlpha = 0.3f + (z + 1f) * 0.4f
                val lightPulse = (sin(time / 180.0 + i * 1.2).toFloat() + 1f) / 2f
                
                val indColor = Color(
                    red = (color.red + sin(i.toDouble()).toFloat() * 0.05f).coerceIn(0f, 1f),
                    green = (color.green + cos(i.toDouble()).toFloat() * 0.05f).coerceIn(0f, 1f),
                    blue = (color.blue + sin(i * 1.5).toFloat() * 0.05f).coerceIn(0f, 1f),
                    alpha = color.alpha
                )
                scope.drawCircle(
                    color = indColor.copy(alpha = (0.3f + 0.4f * lightPulse) * depthAlpha),
                    radius = (2.5f * depthScale) * scope.density,
                    center = Offset(lx, ly)
                )
            }
        }

        // 优化点 2：绘制飞船动力喷焰 (保持 Path 创建但减少 apply 套餐开销)
        val beamAlpha = 0.12f + (sin(time / 200.0).toFloat() + 1f) * 0.04f
        val beamPath = Path()
        beamPath.moveTo(x - ufoWidth * 0.15f, y + discHeight * 0.05f)
        beamPath.lineTo(x + ufoWidth * 0.15f, y + discHeight * 0.05f)
        beamPath.lineTo(x + ufoWidth * 0.4f, y + discHeight * 2.2f)
        beamPath.lineTo(x - ufoWidth * 0.4f, y + discHeight * 2.2f)
        beamPath.close()
        
        scope.drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = beamAlpha), Color.Transparent),
                startY = y + discHeight * 0.05f,
                endY = y + discHeight * 2.2f
            )
        )

        // 优化点 3：机身渐变反光
        val sheenOffset = (sin(rotationAngle.toDouble()).toFloat() * 0.3f)
        val bodyBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF263238), Color(0xFF90A4AE).copy(alpha = 0.9f + sheenOffset * 0.1f), Color(0xFF263238)),
            start = Offset(x - ufoWidth * (0.5f + sheenOffset), y),
            end = Offset(x + ufoWidth * (0.5f - sheenOffset), y)
        )
        scope.drawOval(
            brush = bodyBrush,
            topLeft = Offset(x - ufoWidth / 2f, y - discHeight / 2f),
            size = Size(ufoWidth, discHeight)
        )

        val domePath = Path()
        val domeRect = Rect(x - domeWidth / 2f, y - domeHeight, x + domeWidth / 2f, y + domeHeight)
        domePath.moveTo(x - domeWidth / 2f, y)
        domePath.arcTo(domeRect, 180f, 180f, false)
        domePath.close()
        
        scope.drawPath(
            path = domePath,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), Color(0xFF00B0FF).copy(alpha = 0.6f), Color(0xFF01579B).copy(alpha = 0.8f)),
                center = Offset(x, y - domeHeight * 0.3f),
                radius = domeWidth / 1.5f
            )
        )

        // 优化点 4：第二次循环仅绘制前景灯光
        for (i in 0 until lightCount) {
            val baseAngle = (i * 2.0 * kotlin.math.PI / lightCount).toFloat()
            val totalAngle = baseAngle + rotationAngle
            val z = sin(totalAngle)
            
            if (z > 0) { // 前面
                val lx = x + cos(totalAngle) * (ufoWidth * 0.48f) 
                val ly = y + sin(totalAngle) * (discHeight * 0.5f)
                val depthScale = 0.7f + (z + 1f) * 0.35f
                val depthAlpha = 0.3f + (z + 1f) * 0.4f
                val lightPulse = (sin(time / 180.0 + i * 1.2).toFloat() + 1f) / 2f
                
                val indColor = Color(
                    red = (color.red + sin(i.toDouble()).toFloat() * 0.05f).coerceIn(0f, 1f),
                    green = (color.green + cos(i.toDouble()).toFloat() * 0.05f).coerceIn(0f, 1f),
                    blue = (color.blue + sin(i * 1.5).toFloat() * 0.05f).coerceIn(0f, 1f),
                    alpha = color.alpha
                )
                
                val lightRadius = (3.2f * depthScale) * scope.density
                scope.drawCircle(
                    color = indColor.copy(alpha = (0.5f + 0.5f * lightPulse) * depthAlpha),
                    radius = lightRadius,
                    center = Offset(lx, ly)
                )
                
                val glowRad = (9f * depthScale) * scope.density
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(indColor.copy(alpha = 0.45f * depthAlpha * lightPulse), Color.Transparent),
                        center = Offset(lx, ly),
                        radius = glowRad
                    ),
                    radius = glowRad,
                    center = Offset(lx, ly)
                )
            }
        }
    }
}

@Composable
fun CelestialVisitorOverlay() {
    val activeVisitors = remember { mutableStateListOf<CelestialVisitor>() }
    
    // Curated Palette for Celestial Visitors
    val visitorColors = remember {
        listOf(
            Color(0xFF00E5FF), // Cyan
            Color(0xFFD500F9), // Purple
            Color(0xFFFF4081), // Pink
            Color(0xFFFFD600), // Gold
            Color(0xFF00E676)  // Green
        )
    }
    
    // Spawn Logic
    LaunchedEffect(Unit) {
        while (true) {
            // Increased frequency for verification: 4-6 seconds
            kotlinx.coroutines.delay(Random.nextLong(4000, 6000))
            if (activeVisitors.size < 3) { // Slightly more concurrent visitors
                val type = VisitorType.values().random()
                val side = Random.nextInt(4) // 0: Top, 1: Bottom, 2: Left, 3: Right
                
                var startX = 0f
                var startY = 0f
                var targetX = 0f
                var targetY = 0f
                var speed = 0.05f 
                var size = 8f // Increased base size
                
                when (side) {
                    0 -> { startX = Random.nextFloat(); startY = -0.1f; targetX = Random.nextFloat(); targetY = 1.1f }
                    1 -> { startX = Random.nextFloat(); startY = 1.1f; targetX = Random.nextFloat(); targetY = -0.1f }
                    2 -> { startX = -0.1f; startY = Random.nextFloat(); targetX = 1.1f; targetY = Random.nextFloat() }
                    3 -> { startX = 1.1f; startY = Random.nextFloat(); targetX = -0.1f; targetY = Random.nextFloat() }
                }

                when (type) {
                    VisitorType.METEOR -> { speed = 0.35f; size = 10f }
                    VisitorType.SATELLITE -> { speed = 0.04f; size = 15f }
                    VisitorType.UFO -> { speed = 0.12f; size = 14f }
                }
                
                // Randomize Depth Effect (Near/Far)
                val depthMode = Random.nextInt(3) // 0: Approach, 1: Recede, 2: Pass By
                val startScale: Float
                val targetScale: Float
                
                when (depthMode) {
                    0 -> { // Approach: Far (Small) -> Near (Large)
                        startScale = Random.nextDouble(0.4, 0.6).toFloat()
                        targetScale = Random.nextDouble(1.1, 1.4).toFloat()
                    }
                    1 -> { // Recede: Near (Large) -> Far (Small)
                        startScale = Random.nextDouble(1.1, 1.4).toFloat()
                        targetScale = Random.nextDouble(0.4, 0.6).toFloat()
                    }
                    else -> { // Pass By: Relatively constant but varied depth
                        val baseScale = Random.nextDouble(0.7, 1.1).toFloat()
                        startScale = baseScale
                        targetScale = baseScale * Random.nextDouble(0.9, 1.1).toFloat()
                    }
                }
                
                activeVisitors.add(CelestialVisitor(type, startX, startY, targetX, targetY, speed, size, visitorColors.random(), startScale, targetScale))
            }
        }
    }

    // Animation Loop
    var lastNanoTime by remember { mutableStateOf(System.nanoTime()) }
    Canvas(
        modifier = Modifier.fillMaxSize() // Implicitly transparent to touches as no click/pointer modifiers are added
    ) {
        val currentNanoTime = System.nanoTime()
        val delta = (currentNanoTime - lastNanoTime) / 1_000_000_000f
        lastNanoTime = currentNanoTime
        
        val iterator = activeVisitors.iterator()
        while (iterator.hasNext()) {
            val visitor = iterator.next()
            visitor.update(delta)
            if (visitor.isFinished()) {
                iterator.remove()
            } else {
                visitor.draw(this)
            }
        }
    }
}


@Composable
fun ThemedParticleOverlay(
    theme: String,
    modifier: Modifier = Modifier
) {
    val config = remember(theme) { getParticleTheme(theme) }
    
    // Remember particles for the key 'theme' so they reset when theme changes
    val particles = remember(theme) {
        List(config.count) { ThemedParticle(config) }
    }

    // Static Path Cache - Shared across all particles of the same shape
    val pathCache = remember { mutableMapOf<ParticleShape, Path>() }

    // Sensor State for Gravity/Tilt
    val context = LocalContext.current
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Smoothing factor (Low-pass filter)
                    val alpha = 0.1f
                    // Accelerometer gives m/s^2. X is horizontal tilt, Y is vertical tilt.
                    // We flip the axes to match screen space (X is X, Y is -Y because Y increases downwards)
                    tiltX = tiltX * (1 - alpha) + it.values[0] * alpha
                    tiltY = tiltY * (1 - alpha) + it.values[1] * alpha
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    var time by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTime ->
                time = frameTime
            }
        }
    }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
             val w = size.width
             val h = size.height
             
             particles.forEach { p ->
                 p.update(w, h, config, tiltX, tiltY)
                 p.draw(this, time, config, pathCache)
             }
        }
    }
}

private class ThemedParticle(config: ParticleThemeConfig) {
    var x = Random.nextFloat()
    var y = Random.nextFloat()
    
    var radius = config.sizeBase * (0.5f + Random.nextFloat() * 1.0f)
    
    // Speed vectors (-1..1) * multiplier
    var speedX = (Random.nextFloat() - 0.5f) * 0.02f * config.speedMultiplier
    var speedY = (Random.nextFloat() - 0.5f) * 0.02f * config.speedMultiplier
    
    // Rotation
    var rotation = Random.nextFloat() * 360f
    var rotationSpeed = (Random.nextFloat() - 0.5f) * 0.2f // Reduced from 0.5f for smoother feel
    
    // Adjust initial speed based on style
    init {
        when (config.style) {
            ParticleStyle.RISE -> speedY = -0.05f * Random.nextFloat() * config.speedMultiplier // Up (negative Y)
            ParticleStyle.FALL -> speedY = 0.05f * Random.nextFloat() * config.speedMultiplier // Down (positive Y)
            else -> {} 
        }
    }
    
    var phase = Random.nextFloat() * 6.28f
    var color = config.colors.random()
    
    // Constant speed magnitude and current movement angle
    private val moveSpeed = 0.0024f * config.speedMultiplier
    private var moveAngle = Random.nextFloat() * 2f * PI.toFloat()
    
    fun update(width: Float, height: Float, config: ParticleThemeConfig, tiltX: Float, tiltY: Float) {
        // Calculate tilt direction (if tilt is significant enough)
        val tiltMag = kotlin.math.sqrt(tiltX * tiltX + tiltY * tiltY)
        if (tiltMag > 0.5f) {
            val targetAngle = kotlin.math.atan2(tiltY, -tiltX)
            
            // Smoothly rotate moveAngle towards targetAngle
            // Normalize angle difference to [-PI, PI]
            var angleDiff = targetAngle - moveAngle
            while (angleDiff > PI) angleDiff -= 2f * PI.toFloat()
            while (angleDiff < -PI) angleDiff += 2f * PI.toFloat()
            
            // Turn speed: increased from 0.02f to 0.08f for snappier feedback
            val turnSpeed = 0.08f
            moveAngle += angleDiff * turnSpeed
        }

        // Apply constant speed in moveAngle direction
        x += kotlin.math.cos(moveAngle) * moveSpeed
        y += kotlin.math.sin(moveAngle) * moveSpeed
        
        // Wrap around screen bounds with some padding
        if (x < -0.1f) x = 1.1f
        if (x > 1.1f) x = -0.1f
        if (y < -0.1f) y = 1.1f
        if (y > 1.1f) y = -0.1f
        
        rotation += rotationSpeed
    }
    
    fun draw(scope: DrawScope, time: Long, config: ParticleThemeConfig, pathCache: MutableMap<ParticleShape, Path>) {
        val cx = x * scope.size.width
        val cy = y * scope.size.height
        
        // Breathe alpha slowly
        val alphaBase = 0.1f + (sin((time / 1000f + phase).toDouble()).toFloat() + 1f) * 0.05f
        val currentColor = color.copy(alpha = alphaBase.coerceIn(0f, 0.4f))
        
        val r = radius * scope.density
        
        scope.translate(left = cx, top = cy) {
            scope.rotate(rotation) {
                when (config.shape) {
                    ParticleShape.CIRCLE -> {
                        drawCircle(color = currentColor, radius = r)
                    }
                    ParticleShape.RING -> {
                        drawCircle(color = currentColor, radius = r, style = Stroke(width = r * 0.2f))
                    }
                    ParticleShape.SQUARE -> {
                        drawRect(
                            color = currentColor, 
                            topLeft = Offset(-r, -r), 
                            size = Size(r * 2, r * 2)
                        )
                    }
                    ParticleShape.TRIANGLE -> {
                        val path = pathCache.getOrPut(ParticleShape.TRIANGLE) {
                             Path().apply {
                                moveTo(0f, -1f)
                                lineTo(0.866f, 0.5f)
                                lineTo(-0.866f, 0.5f)
                                close()
                            }
                        }
                        scope.rotate(0f) { // dummy to keep scope
                             val scaleMatrix = androidx.compose.ui.graphics.Matrix().apply {
                                scale(r, r, 1f)
                             }
                             val scaledPath = Path().apply {
                                 addPath(path)
                                 transform(scaleMatrix)
                             }
                             drawPath(scaledPath, color = currentColor)
                        }
                    }
                    ParticleShape.DIAMOND -> {
                        val path = pathCache.getOrPut(ParticleShape.DIAMOND) {
                            Path().apply {
                                moveTo(0f, -1f)
                                lineTo(0.7f, 0f)
                                lineTo(0f, 1f)
                                lineTo(-0.7f, 0f)
                                close()
                            }
                        }
                         val scaleMatrix = androidx.compose.ui.graphics.Matrix().apply {
                            scale(r, r, 1f)
                         }
                         val scaledPath = Path().apply {
                             addPath(path)
                             transform(scaleMatrix)
                         }
                         drawPath(scaledPath, color = currentColor)
                    }
                    ParticleShape.HEXAGON -> {
                        val path = pathCache.getOrPut(ParticleShape.HEXAGON) {
                            Path().apply {
                                for (i in 0 until 6) {
                                    val angle = 2.0 * PI / 6 * i
                                    val px = sin(angle).toFloat()
                                    val py = cos(angle).toFloat()
                                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                        }
                         val scaleMatrix = androidx.compose.ui.graphics.Matrix().apply {
                            scale(r, r, 1f)
                         }
                         val scaledPath = Path().apply {
                             addPath(path)
                             transform(scaleMatrix)
                         }
                         drawPath(scaledPath, color = currentColor)
                    }
                    ParticleShape.STAR -> {
                         val path = pathCache.getOrPut(ParticleShape.STAR) {
                             Path().apply {
                                for (i in 0 until 5) {
                                    val angleOuter = (2.0 * PI / 5 * i) - (PI / 2)
                                    val ox = cos(angleOuter).toFloat()
                                    val oy = sin(angleOuter).toFloat()
                                    if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                                    
                                    val angleInner = (2.0 * PI / 5 * i) + (PI / 5) - (PI / 2)
                                    val ix = (0.4f * cos(angleInner)).toFloat()
                                    val iy = (0.4f * sin(angleInner)).toFloat()
                                    lineTo(ix, iy)
                                }
                                close()
                            }
                        }
                         val scaleMatrix = androidx.compose.ui.graphics.Matrix().apply {
                            scale(r, r, 1f)
                         }
                         val scaledPath = Path().apply {
                             addPath(path)
                             transform(scaleMatrix)
                         }
                         drawPath(scaledPath, color = currentColor)
                    }
                }
            }
        }
    }
}

private fun getParticleTheme(theme: String): ParticleThemeConfig {
    return when (theme) {
        "light" -> ParticleThemeConfig( // 光芒
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFFFFF)), 
            speedMultiplier = 0.12f, // Reduced from 0.3f
            count = 12,
            shape = ParticleShape.CIRCLE
        )
        "nature" -> ParticleThemeConfig( // 林间
            colors = listOf(Color(0xFFC8E6C9), Color(0xFFF1F8E9)), 
            speedMultiplier = 0.4f, 
            style = ParticleStyle.FALL,
            shape = ParticleShape.CIRCLE
        )
        "abyss" -> ParticleThemeConfig( // 深渊
            colors = listOf(Color(0xFF0288D1), Color(0xFF00ACC1)), 
            speedMultiplier = 0.3f, 
            sizeBase = 80f,
            shape = ParticleShape.RING // Bubbles
        )
        "fire" -> ParticleThemeConfig( // 烈焰
            colors = listOf(Color(0xFFFF7043), Color(0xFFFFAB91)), 
            speedMultiplier = 0.6f, 
            style = ParticleStyle.RISE, 
            sizeBase = 60f, 
            count = 10,
            shape = ParticleShape.TRIANGLE // Embers
        )
        "magic" -> ParticleThemeConfig( // 魔法
            colors = listOf(Color(0xFFBA68C8), Color(0xFFE1BEE7)), 
            speedMultiplier = 0.35f,
            count = 8,
            shape = ParticleShape.STAR // Sparkles
        )
        "ruins" -> ParticleThemeConfig( // 废墟
            colors = listOf(Color(0xFFBCAAA4), Color(0xFFD7CCC8)), 
            speedMultiplier = 0.2f, 
            style = ParticleStyle.FLOAT,
            count = 6,
            shape = ParticleShape.SQUARE // Debris
        )
        "ice" -> ParticleThemeConfig( // 冻土
            colors = listOf(Color(0xFFB3E5FC), Color(0xFFFFFFFF)), 
            speedMultiplier = 0.3f, 
            style = ParticleStyle.FALL,
            count = 8,
            shape = ParticleShape.DIAMOND // Crystals
        )
        "mechanical" -> ParticleThemeConfig( // 机械
            colors = listOf(Color(0xFF4DD0E1), Color(0xFFB2EBF2)), 
            speedMultiplier = 0.4f, 
            sizeBase = 90f,
            count = 8,
            shape = ParticleShape.HEXAGON // Tech
        )
        "time" -> ParticleThemeConfig( // 时间
            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFF9C4)), 
            speedMultiplier = 0.25f,
            count = 8,
            shape = ParticleShape.DIAMOND // Hourglass sand
        )
        "cosmos" -> ParticleThemeConfig( // 星辰
            colors = listOf(Color(0xFF7E57C2), Color(0xFF512DA8)), 
            speedMultiplier = 0.3f,
            count = 10,
            shape = ParticleShape.STAR
        )
        "void" -> ParticleThemeConfig( // 虚空
            colors = listOf(Color(0xFF90A4AE), Color(0xFF455A64)), 
            speedMultiplier = 0.2f, 
            count = 5,
            shape = ParticleShape.SQUARE // Glitch pixels
        )
        "unity" -> ParticleThemeConfig( // 归一 (飞升关卡)
            colors = listOf(Color(0xFFFF8A80), Color(0xFF80D8FF), Color(0xFFA5D6A7), Color(0xFFCE93D8)), 
            speedMultiplier = 0.15f, // Reduced from 0.4f
            count = 10,
            shape = ParticleShape.HEXAGON
        )
        "signin" -> ParticleThemeConfig( // 签到页 (独立主题)
            colors = listOf(Color(0xFFFFD54F), Color(0xFF81D4FA)), 
            speedMultiplier = 0.1f, // Very slow
            count = 8,
            shape = ParticleShape.RING
        )
        "profile" -> ParticleThemeConfig( // 个人中心 (独立主题)
            colors = listOf(Color(0xFFCE93D8), Color(0xFFB3E5FC)), 
            speedMultiplier = 0.1f, // Very slow
            count = 8,
            shape = ParticleShape.SQUARE
        )
        else -> ParticleThemeConfig( // Default Magic
            colors = listOf(Color.White), 
            speedMultiplier = 0.3f,
            shape = ParticleShape.STAR
        )
    }
}
