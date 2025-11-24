package site.aiok.onepic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import site.aiok.onepic.logic.ImageSlicer
import site.aiok.onepic.view.GameBoardView
import site.aiok.onepic.ui.components.GlassTopBar
import site.aiok.onepic.ui.components.MeshGradientBackground
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

import site.aiok.onepic.model.LevelConfig

@Composable
fun GameScreen(levelConfig: LevelConfig, onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Generate bitmap based on level ID with complex patterns for better visibility
    val bitmap = remember(levelConfig) {
        val width = 800
        val height = 600
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        
        // Base Hue based on level
        val baseHue = (levelConfig.levelId * 77) % 360f
        
        // 1. Background Gradient (Diagonal)
        val colors = intArrayOf(
            Color.HSVToColor(floatArrayOf(baseHue, 0.3f, 0.9f)),
            Color.HSVToColor(floatArrayOf((baseHue + 40) % 360f, 0.4f, 0.8f))
        )
        val gradient = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null // Reset shader
        
        // 2. Grid Lines (Subtle help for alignment)
        paint.strokeWidth = 2f
        paint.color = Color.argb(50, 0, 0, 0)
        val cellSize = 100f
        for (i in 0..width step 100) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        }
        for (i in 0..height step 100) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
        }
        
        // 3. Random Geometric Shapes (to create unique edges)
        val rnd = java.util.Random(levelConfig.levelId.toLong())
        for (i in 0..15) {
            paint.color = Color.HSVToColor(
                200, // Alpha
                floatArrayOf((baseHue + rnd.nextInt(180)) % 360f, 0.7f, 0.8f)
            )
            
            val cx = rnd.nextFloat() * width
            val cy = rnd.nextFloat() * height
            val size = 50f + rnd.nextFloat() * 150f
            
            when (rnd.nextInt(3)) {
                0 -> canvas.drawCircle(cx, cy, size / 2, paint)
                1 -> canvas.drawRect(cx - size/2, cy - size/2, cx + size/2, cy + size/2, paint)
                2 -> {
                    // Triangle
                    val path = android.graphics.Path()
                    path.moveTo(cx, cy - size/2)
                    path.lineTo(cx - size/2, cy + size/2)
                    path.lineTo(cx + size/2, cy + size/2)
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
        
        // 4. Large Text Number (Center)
        paint.color = Color.argb(100, 255, 255, 255)
        paint.textSize = 400f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        // Center text vertically
        val textBounds = android.graphics.Rect()
        paint.getTextBounds("${levelConfig.levelId}", 0, "${levelConfig.levelId}".length, textBounds)
        val yOffset = textBounds.height() / 2f
        canvas.drawText("${levelConfig.levelId}", width / 2f, height / 2f + yOffset, paint)

        // 5. Border
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        bmp
    }

    val pieces = remember(levelConfig) {
        ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols)
    }

    MeshGradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            GlassTopBar(
                title = levelConfig.title,
                onBack = onBack,
                modifier = Modifier.padding(16.dp)
            )
            
            // Game Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        GameBoardView(ctx).apply {
                            setPieces(pieces)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
