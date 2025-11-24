package site.aiok.onepic.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Int,
    var alpha: Int = 255,
    var life: Float = 1.0f,
    var decay: Float = 0.02f,
    var size: Float = 10f
)

data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,
    var alpha: Int = 255,
    var life: Int = 60
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val paint = Paint().apply { style = Paint.Style.FILL }
    private val random = Random.Default
    
    var isFireworksMode = false
        set(value) {
            field = value
            if (value) {
                fireworksBurstsLeft = 5 // Limit to 5 bursts
                fireworksTimer = 20 // Start immediately
            }
        }
    private var fireworksTimer = 0
    private var fireworksBurstsLeft = 0
    private var width = 0
    private var height = 0

    fun setDimensions(w: Int, h: Int) {
        width = w
        height = h
    }
    
    fun clear() {
        particles.clear()
        floatingTexts.clear()
        isFireworksMode = false
    }

    fun emit(x: Float, y: Float, count: Int, colors: List<Int> = listOf(Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN)) {
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * Math.PI
            // Reduced speed range for smaller explosion radius (was 15f + 5f)
            val speed = random.nextFloat() * 8f + 2f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    color = colors.random(),
                    // Increased decay for shorter life (was 0.03f + 0.01f)
                    decay = random.nextFloat() * 0.05f + 0.02f,
                    // Reduced particle size (was 10f + 5f)
                    size = random.nextFloat() * 5f + 2f
                )
            )
        }
    }

    fun addFloatingText(x: Float, y: Float, text: String, color: Int) {
        floatingTexts.add(FloatingText(x, y, text, color))
    }

    fun update() {
        if (isFireworksMode) {
            fireworksTimer++
            if (fireworksTimer > 20) { // Emit every 20 frames roughly
                fireworksTimer = 0
                if (fireworksBurstsLeft > 0) {
                    val x = random.nextFloat() * width
                    val y = random.nextFloat() * height * 0.5f // Top half
                    // Reduced particle count for fireworks (was 50)
                    emit(x, y, 25)
                    fireworksBurstsLeft--
                } else {
                    isFireworksMode = false // Auto stop after bursts run out
                }
            }
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.5f // Gravity
            p.life -= p.decay
            p.alpha = (p.life * 255).toInt().coerceIn(0, 255)

            if (p.life <= 0) {
                iterator.remove()
            }
        }

        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val text = textIterator.next()
            text.y -= 3f // Float up
            text.life--
            text.alpha = (text.life * 255 / 60).coerceIn(0, 255)
            if (text.life <= 0) textIterator.remove()
        }
    }

    fun draw(canvas: Canvas) {
        particles.forEach { p ->
            paint.color = p.color
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }

        val textPaint = Paint().apply {
            textSize = 80f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }

        floatingTexts.forEach { text ->
            textPaint.color = text.color
            textPaint.alpha = text.alpha
            canvas.drawText(text.text, text.x, text.y, textPaint)
        }
    }
    
    fun isActive(): Boolean = particles.isNotEmpty() || isFireworksMode || floatingTexts.isNotEmpty()
}
