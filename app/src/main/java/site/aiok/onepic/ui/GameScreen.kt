package site.aiok.onepic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import site.aiok.onepic.logic.ImageSlicer
import site.aiok.onepic.view.GameBoardView
import site.aiok.onepic.ui.components.GlassTopBar
import site.aiok.onepic.ui.components.MeshGradientBackground
import site.aiok.onepic.model.LevelConfig
import site.aiok.onepic.model.ImageSource

@Composable
fun GameScreen(
    levelConfig: LevelConfig, 
    levelIndex: Int,
    levelMode: String,
    onBack: () -> Unit, 
    onLevelComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var elapsedTime by remember { mutableStateOf(0) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completionStars by remember { mutableStateOf(0) }
    var scoreGained by remember { mutableStateOf(0) }
    var currentScore by remember { mutableStateOf(0) }  // 实时得分（合并/拆解产生的分数）
    
    // 获取历史最佳时间
    val bestTime = remember(levelIndex, levelMode) {
        when (levelMode) {
            "classic" -> site.aiok.onepic.data.LevelProgressManager.getClassicLevelBestTime(context, levelIndex)
            "gallery" -> site.aiok.onepic.data.LevelProgressManager.getGalleryLevelBestTime(context, levelIndex)
            else -> Int.MAX_VALUE
        }
    }
    
    // Load bitmap based on ImageSource
    val bitmap = remember(levelConfig) {
        try {
            val originalBitmap = when (val source = levelConfig.imageSource) {
                is ImageSource.Generated -> {
                    null // Will be generated below
                }
                is ImageSource.Asset -> {
                    val inputStream = context.assets.open(source.path)
                    BitmapFactory.decodeStream(inputStream)
                }
                is ImageSource.UriSource -> {
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, source.uri)
                    } else {
                        val sourceDec = ImageDecoder.createSource(context.contentResolver, source.uri)
                        ImageDecoder.decodeBitmap(sourceDec) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    }
                }
                is ImageSource.Resource -> {
                    BitmapFactory.decodeResource(context.resources, source.resId)
                }
            }

            // Standardize Bitmap Size
            if (originalBitmap != null) {
                // Scale down if too large, or fit to a standard game size
                // For now, we scale to a max dimension to keep performance good
                val maxDim = 1024
                val scale = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
                    minOf(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height)
                } else {
                    1f
                }
                
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * scale).toInt(),
                        (originalBitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    originalBitmap
                }
            } else {
                 // Generated Fallback (Classic Mode)
        val width = 800
        val height = 600
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        
                 // Use hash of ID for consistent generation
                 val seed = levelConfig.levelId.hashCode().toLong()
                 
                 // Base Hue based on level
                 val baseHue = (seed * 77) % 360f
                 
                 // 1. Background Gradient (Diagonal)
                 val colors = intArrayOf(
                     android.graphics.Color.HSVToColor(floatArrayOf(baseHue, 0.3f, 0.9f)),
                     android.graphics.Color.HSVToColor(floatArrayOf((baseHue + 40) % 360f, 0.4f, 0.8f))
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
                 paint.color = android.graphics.Color.argb(50, 0, 0, 0)
                 for (i in 0..width step 100) {
                     canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
                 }
                 for (i in 0..height step 100) {
                     canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
                 }
                 
                 // 3. Random Geometric Shapes (to create unique edges)
                 val rnd = java.util.Random(seed)
                 for (i in 0..15) {
                     paint.color = android.graphics.Color.HSVToColor(
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
                 paint.color = android.graphics.Color.argb(100, 255, 255, 255)
                 paint.textSize = 400f
                 paint.textAlign = android.graphics.Paint.Align.CENTER
                 paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                 // Center text vertically
                 val textBounds = android.graphics.Rect()
                 val idText = levelConfig.levelId.replace("c", "") // Simple display for classic
                 paint.getTextBounds(idText, 0, idText.length, textBounds)
                 val yOffset = textBounds.height() / 2f
                 canvas.drawText(idText, width / 2f, height / 2f + yOffset, paint)

                 // 5. Border
                 paint.style = android.graphics.Paint.Style.STROKE
                 paint.strokeWidth = 10f
        paint.color = android.graphics.Color.BLACK
                 canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        bmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to empty bitmap
            Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        }
    }

    val pieces = remember(levelConfig) {
        ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols)
    }

    // 计时器更新
    var gameBoardView: GameBoardView? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            gameBoardView?.let {
                elapsedTime = it.getElapsedSeconds()
            }
        }
    }

    MeshGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            GlassTopBar(
                title = levelConfig.title,
                onBack = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
                
                // 计时器和星星显示
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 计时器
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "⏱",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = site.aiok.onepic.ui.components.formatTime(elapsedTime),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                    
                    // 实时得分（使用硬币emoji）
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 硬币emoji - 添加描边效果
                            Text(
                                text = "🪙",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 24.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                            Text(
                                text = currentScore.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                    
                    // 目标星星
                    site.aiok.onepic.ui.components.GlassBox(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(3) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            
            // Game Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        GameBoardView(ctx).apply {
                            setPieces(pieces)
                                gameBoardView = this
                                
                                // 处理实时得分变化（合并/拆解）
                                onScoreChange = { scoreDelta ->
                                    currentScore += scoreDelta
                                    // 实时保存合并得分（scoreDelta可以是正数或负数）
                                    site.aiok.onepic.data.LevelProgressManager.saveTotalMergeScore(context, scoreDelta)
                                }
                                
                                onPuzzleComplete = { timeInSeconds ->
                                    elapsedTime = timeInSeconds
                                    
                                    // 计算星星数（考虑历史最佳）
                                    completionStars = site.aiok.onepic.ui.components.calculateStars(
                                        timeInSeconds, 
                                        levelConfig.rows, 
                                        levelConfig.cols,
                                        bestTime
                                    )
                                    
                                    // 保存最佳时间和星星数
                                    when (levelMode) {
                                        "classic" -> {
                                            site.aiok.onepic.data.LevelProgressManager.saveClassicLevelBestTime(context, levelIndex, timeInSeconds)
                                            val currentBestStars = site.aiok.onepic.data.LevelProgressManager.getClassicLevelStars(context, levelIndex)
                                            if (completionStars > currentBestStars) {
                                                site.aiok.onepic.data.LevelProgressManager.saveClassicLevelStars(context, levelIndex, completionStars)
                                            }
                                        }
                                        "gallery" -> {
                                            site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelBestTime(context, levelIndex, timeInSeconds)
                                            val currentBestStars = site.aiok.onepic.data.LevelProgressManager.getGalleryLevelStars(context, levelIndex)
                                            if (completionStars > currentBestStars) {
                                                site.aiok.onepic.data.LevelProgressManager.saveGalleryLevelStars(context, levelIndex, completionStars)
                                            }
                                        }
                                    }
                                    
                                    // scoreGained 应该是这局获得的合并得分（硬币分数），而不是星星数
                                    scoreGained = currentScore
                                    
                                    showCompleteDialog = true
                                    onLevelComplete()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // 完成弹窗
            if (showCompleteDialog) {
                site.aiok.onepic.ui.components.LevelCompleteDialog(
                    stars = completionStars,
                    timeInSeconds = elapsedTime,
                    scoreGained = scoreGained,
                    onRestart = {
                        showCompleteDialog = false
                        scoreGained = 0
                        currentScore = 0  // 重置实时得分
                        gameBoardView?.setPieces(
                            ImageSlicer.sliceImage(bitmap, levelConfig.rows, levelConfig.cols)
                        )
                    },
                    onNextLevel = {
                        showCompleteDialog = false
                        scoreGained = 0
                        onBack() // 返回关卡选择界面，解锁机制会自动生效
                    },
                    onDismiss = {
                        showCompleteDialog = false
                        scoreGained = 0
                    }
                )
            }
        }
    }
}
