package site.aiok.onepic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import site.aiok.onepic.ui.components.MeshGradientBackground
import kotlin.random.Random

@Composable
fun IntroScreen(onStartJourney: () -> Unit) {
    var showButton by remember { mutableStateOf(false) }
    
    // 存储每一行的显示状态，实现逐行动画
    val fullLines = remember { mutableStateOf<List<String>>(emptyList()) }
    val visibleLinesCount = remember { mutableIntStateOf(0) }
    
    // 信号干扰（Glitch）状态
    var glitchX by remember { mutableStateOf(0f) }
    var glitchAlpha by remember { mutableStateOf(1f) }

    // Prologue text from localized strings
    val fullText: String = androidx.compose.ui.res.stringResource(site.aiok.onepic.R.string.intro_prologue)
    val buttonText: String = androidx.compose.ui.res.stringResource(site.aiok.onepic.R.string.intro_button)

    LaunchedEffect(Unit) {
        fullLines.value = fullText.split("\n")
        fullLines.value.forEach { _ ->
            visibleLinesCount.intValue++
            delay(1200L) // 每一行出现的间隔
        }
        delay(800)
        showButton = true
    }
    
    // 随机信号干扰循环
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2000, 5000)) // 间歇性干扰
            repeat(Random.nextInt(3, 7)) {
                glitchX = Random.nextFloat() * 10f - 5f
                glitchAlpha = Random.nextFloat() * 0.3f + 0.7f
                delay(50)
                glitchX = 0f
                glitchAlpha = 1f
                delay(30)
            }
        }
    }

    MeshGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            StardustEffect()
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrollable Content Area with animated lines
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 64.dp)
                            .offset(x = glitchX.dp)
                            .alpha(glitchAlpha)
                    ) {
                        fullLines.value.forEachIndexed { index, line ->
                            AnimatedVisibility(
                                visible = index < visibleLinesCount.intValue,
                                enter = fadeIn(tween(1500)) + 
                                        slideInVertically(tween(1500)) { it / 2 }
                            ) {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        lineHeight = 28.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Fixed Bottom Button Area
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(tween(1000)) + 
                            slideInVertically(tween(1000)) { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 64.dp, top = 16.dp)
                    ) {
                        // Rocket Launch State
                        var isLaunching by remember { mutableStateOf(false) }
                        var isCharging by remember { mutableStateOf(false) } // New state for pre-launch vibration
                        val rocketOffsetY = remember { Animatable(0f) }
                        val rocketScale = remember { Animatable(1f) }
                        val vibrationOffset = remember { mutableStateOf(Offset.Zero) }
                        
                        // Flame Animation State
                        val flameScale = remember { Animatable(1f) }
                        val flameColor = animateColorAsState(
                            targetValue = if (isLaunching) Color(0xFFFF5722) else Color(0xFF8E24AA), 
                            animationSpec = tween(300)
                        )

                        // Launch Logic
                        val scope = rememberCoroutineScope()
                        val launchRocket = {
                            if (!isLaunching) {
                                isLaunching = true
                                isCharging = true 
                                scope.launch {
                                    // 1. Ignite Engine & Charge (Vibration)
                                    launch { flameScale.animateTo(4.0f, tween(800, easing = LinearEasing)) }
                                    launch { rocketScale.animateTo(0.95f, tween(200)) } 
                                    
                                    // Heavy Vibration Loop for Charging duration
                                    val startTime = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - startTime < 800) {
                                        vibrationOffset.value = Offset(
                                            x = Random.nextFloat() * 6f - 3f,
                                            y = Random.nextFloat() * 6f - 3f
                                        )
                                        delay(16) // ~60fps shake
                                    }
                                    vibrationOffset.value = Offset.Zero
                                    isCharging = false

                                    // 2. Blast Off (Fly up)
                                    launch { rocketOffsetY.animateTo(-2500f, tween(800, easing = LinearEasing)) }
                                    launch { rocketScale.animateTo(1.2f, tween(500)) } 
                                    
                                    delay(600)
                                    onStartJourney()
                                }
                            }
                        }

                        // Ark Rocket Container (Stationary Dock + Animated Rocket)
                        val infiniteTransition = rememberInfiniteTransition(label = "rocket_visual")
                        // Floating animation (disable when launching to control manually)
                        val floatOffset by if (!isLaunching) {
                            infiniteTransition.animateFloat(
                                initialValue = -5f,
                                targetValue = 5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "float"
                            )
                        } else {
                            remember { mutableFloatStateOf(0f) }
                        }

                        Box(
                            modifier = Modifier
                                .size(120.dp) // Container size
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = launchRocket
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Stationary Base (The Dock/Portal)
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .shadow(12.dp, CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF8E24AA), // Purple
                                                Color(0xFF311B92), // Deep Indigo
                                                Color(0xFF000000)  // Black
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            )

                            // 2. Animated Assembly (Rocket + Engine Flame)
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        translationY = rocketOffsetY.value + floatOffset + vibrationOffset.value.y
                                        translationX = vibrationOffset.value.x
                                        scaleX = rocketScale.value
                                        scaleY = rocketScale.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Real Tail Flame (Directional Jet)
                                Box(
                                    modifier = Modifier
                                        .size(width = 18.dp, height = 60.dp) 
                                        .align(Alignment.Center)
                                        .offset(y = 32.dp) // Strictly centered below
                                        .graphicsLayer {
                                            transformOrigin = TransformOrigin(0.5f, 0f) // Grow downwards
                                            scaleY = flameScale.value
                                            scaleX = if (isLaunching) 0.8f else 1f
                                            alpha = if (isLaunching) 0.9f else 0.0f 
                                            rotationZ = 0f // Strictly vertical
                                        }
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFFFFFFF), // Core: White hot
                                                    Color(0xFFFFEB3B), // Middle: Yellow
                                                    Color(0xFFFF5722), // End: Red
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                                        )
                                )

                                // Idle Glow
                                if (!isLaunching) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .offset(y = 10.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF8E24AA).copy(alpha = 0.6f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                // Rocket Icon
                                Icon(
                                    imageVector = Icons.Filled.RocketLaunch,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .graphicsLayer {
                                            // Fix rotation: Material Icon is tilted 45 degrees by default. 
                                            // We rotate -45 to make it point straight UP.
                                            val baseRotation = -45f 
                                            val idleWiggle = if (!isLaunching) (kotlin.math.sin(floatOffset.toDouble() * 0.5).toFloat() * 5f) else 0f
                                            
                                            // Only apply base rotation and idle wiggle here. Launch shake is now handled by parent Box translation
                                            rotationZ = baseRotation + idleWiggle 
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        IntroButton(
                            text = buttonText,
                            onClick = launchRocket
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StardustEffect() {
    val particles = remember { List(40) { MovingParticle() } }
    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTime = it }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val shipSize = 6.dp.toPx()
        val shipWidth = 4.dp.toPx()
        val satelliteSize = 2.dp.toPx()
        val panelLength = 10.dp.toPx()
        val panelHeight = 3.dp.toPx()
        val panelWidth = 5.dp.toPx()
        val panelOffset = 5.dp.toPx()

        particles.forEach { p ->
            // Update physical position (velocity * time)
            p.update(size.width, size.height)
            
            val alpha = p.alpha * (0.8f + 0.2f * kotlin.math.sin(frameTime / 500_000_000f + p.seed))
            
            when (p.type) {
                ParticleType.SHIP -> {
                    // Space Ship: Point and rotate
                    val rotation = (frameTime / 2_000_000_000f * 360f * p.rotationSpeed + p.seed * 100) % 360
                    rotate(rotation, Offset(p.x, p.y)) {
                        // Drawing a simple tech-triangle/ship shape
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(p.x, p.y - shipSize)
                            lineTo(p.x - shipWidth, p.y + shipSize)
                            lineTo(p.x + shipWidth, p.y + shipSize)
                            close()
                        }
                        drawPath(path, Color.White.copy(alpha = alpha * 0.7f))
                        // Engine glow
                        drawCircle(Color(0xFF2575FC).copy(alpha = alpha), 2.dp.toPx(), Offset(p.x, p.y + shipSize))
                    }
                }
                ParticleType.SATELLITE -> {
                    // Satellite: Body + Slowly adjusting solar panels
                    val panelAngle = 30f * kotlin.math.sin(frameTime / 1_500_000_000f * p.rotationSpeed + p.seed)
                    drawRect(Color.White.copy(alpha = alpha), Offset(p.x - satelliteSize, p.y - satelliteSize), androidx.compose.ui.geometry.Size(satelliteSize * 2, satelliteSize * 2))
                    
                    rotate(panelAngle, Offset(p.x, p.y)) {
                        drawLine(
                            Color.White.copy(alpha = alpha * 0.6f),
                            Offset(p.x - panelLength, p.y),
                            Offset(p.x + panelLength, p.y),
                            1.dp.toPx()
                        )
                        drawRect(Color(0xFF6A11CB).copy(alpha = alpha * 0.4f), Offset(p.x - panelLength, p.y - panelHeight), androidx.compose.ui.geometry.Size(panelWidth, panelHeight * 2))
                        drawRect(Color(0xFF6A11CB).copy(alpha = alpha * 0.4f), Offset(p.x + panelOffset, p.y - panelHeight), androidx.compose.ui.geometry.Size(panelWidth, panelHeight * 2))
                    }
                }
                else -> {
                    // Basic Stardust
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = p.radius,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }
    }
}

private enum class ParticleType { STARDUST, SHIP, SATELLITE }

private class MovingParticle(
    var x: Float = Random.nextFloat() * 2000f, // Initial random spread
    var y: Float = Random.nextFloat() * 2000f,
    val type: ParticleType = when (Random.nextInt(10)) {
        0 -> ParticleType.SHIP
        1 -> ParticleType.SATELLITE
        else -> ParticleType.STARDUST
    },
    val radius: Float = Random.nextFloat() * 2.0f + 0.5f,
    val alpha: Float = Random.nextFloat() * 0.5f + 0.1f,
    val vx: Float = (Random.nextFloat() - 0.5f) * 0.5f, // Slow drift
    val vy: Float = (Random.nextFloat() - 0.5f) * 0.5f,
    val seed: Float = Random.nextFloat() * 1000f,
    val rotationSpeed: Float = Random.nextFloat() * 0.5f + 0.5f
) {
    fun update(width: Float, height: Float) {
        // Ensure initial x/y is within screen bounds on first frame if not already
        if (x > width + 100) x = Random.nextFloat() * width
        if (y > height + 100) y = Random.nextFloat() * height
        
        x += vx
        y += vy
        
        // Wraparound
        if (x < -50f) x = width + 50f
        if (x > width + 50f) x = -50f
        if (y < -50f) y = height + 50f
        if (y > height + 50f) y = -50f
    }
}

@Composable
private fun IntroButton(text: String, onClick: () -> Unit) {
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(50.dp.toPx())
                )
            }
            .scale(scale)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color.White
        )
    }
}
